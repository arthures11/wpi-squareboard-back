package com.bryja.wpisquareboardback.service;

import com.bryja.wpisquareboardback.repository.*;
import com.bryja.wpisquareboardback.model.*;
import com.bryja.wpisquareboardback.config.*;
import com.bryja.wpisquareboardback.exception.*;
import com.bryja.wpisquareboardback.util.*;
import com.bryja.wpisquareboardback.dto.*;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommandService {

    private final UnitRepository unitRepository;
    private final CommandHistoryRepository historyRepository;
    private final GameService gameService; // Use GameService to find units at positions, get game state
    private final GameRepository gameRepository; // For saving game status if needed
    private final GameConfigProperties config;
    private final BoardUtils boardUtils;
    private final SecureRandom random = new SecureRandom();

    @Transactional(propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = Exception.class)
    public Unit executeCommand(Long gameId, Long unitId, CommandRequestDTO request) {
        String resultDescription = "FAILED: Unknown reason";
        Position targetPosition = null;
        if (request.getTargetX() != null && request.getTargetY() != null) {
            targetPosition = new Position(request.getTargetX(), request.getTargetY());
        }

        Unit actingUnit = null;
        Optional<Unit> targetUnitOpt = Optional.empty();

        try {
            // 1. game fetching (implicitly validates gameId via unit fetch) and Acting Unit (with lock)
            actingUnit = unitRepository.findByIdAndGameIdForUpdate(unitId, gameId)
                    .orElseThrow(() -> new UnitNotFoundException("Unit " + unitId + " not found in game " + gameId));

            Game game = actingUnit.getGame();

            // 2. some validations
            validateUnitIsActive(actingUnit);
            validatePlayerControl(actingUnit, request.getPlayerColor());
            validateActionAllowed(actingUnit.getUnitType(), request.getCommandType());

            // 3. cd check (based on last successful action)
            checkCooldown(actingUnit, request.getCommandType());

            switch (request.getCommandType()) {
                case MOVE:
                    targetPosition = validateAndGetTargetPosition(request, "MOVE");
                    resultDescription = handleMoveCommand(game, actingUnit, targetPosition);
                    break;
                case SHOOT:
                    targetPosition = validateAndGetTargetPosition(request, "SHOOT");
                    targetUnitOpt = gameService.findUnitAtPositionForUpdate(game.getId(), targetPosition);
                    resultDescription = handleShootCommand(game, actingUnit, targetPosition, targetUnitOpt);
                    break;
                default:
                    throw new InvalidCommandException("Unsupported command type: " + request.getCommandType());
            }

            // 4. unit state update (timestamp, move count) if successful
            if (resultDescription.startsWith("SUCCESS")) {
                actingUnit.setLastActionTimestamp(Instant.now());
                if (request.getCommandType() == CommandType.MOVE) {
                    actingUnit.setMoveCount(actingUnit.getMoveCount() + 1);
                }
            } else if (resultDescription.contains("FAILED_BLOCKED")) {
                actingUnit.setLastActionTimestamp(Instant.now());
            }

            // 5. save updated acting unit (and potentially target unit if destroyed)
            Unit savedActingUnit = unitRepository.save(actingUnit); // Will throw OptimisticLockException if version mismatch


            // 6. save the record to history
            CommandHistory history = new CommandHistory(game, savedActingUnit, request.getPlayerColor(), request.getCommandType(), targetPosition, resultDescription);
            game.addCommandHistory(history);
            savedActingUnit.addCommandHistory(history);
            historyRepository.save(history);

            log.info("Command {} executed for unit {} by player {}. Result: {}", request.getCommandType(), unitId, request.getPlayerColor(), resultDescription);
            return savedActingUnit;


        } catch (OptimisticLockException ole) {
            log.warn("Optimistic Lock Exception during command execution for unit {}: {}", unitId, ole.getMessage());
            resultDescription = "FAILED: Concurrency conflict, please retry.";
            recordFailedCommandAttempt(gameId, unitId, request.getPlayerColor(), request.getCommandType(), targetPosition, resultDescription);
            throw new ConcurrencyConflictException(resultDescription);
        } catch (CooldownException | InvalidCommandException | ActionNotAllowedException |
                 OutOfBoundsException | PositionOccupiedException e) {
            log.warn("Command failed for unit {}: {}", (actingUnit != null ? actingUnit.getId() : unitId), e.getMessage());
            resultDescription = "FAILED: " + e.getMessage();
            recordFailedCommandAttempt(gameId, unitId, request.getPlayerColor(), request.getCommandType(), targetPosition, resultDescription);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during command execution for unit {}: {}", (actingUnit != null ? actingUnit.getId() : unitId), e.getMessage(), e);
            resultDescription = "FAILED: Internal server error.";
            recordFailedCommandAttempt(gameId, unitId, request.getPlayerColor(), request.getCommandType(), targetPosition, resultDescription);
            throw new RuntimeException("Command execution failed unexpectedly.", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedCommandAttempt(Long gameId, Long unitId, PlayerColor playerColor, CommandType commandType, Position targetPosition, String resultDescription) {
        try {
            Game game = gameRepository.findById(gameId).orElse(null);
            Unit unit = unitRepository.findById(unitId).orElse(null);
            if (game != null) {
                CommandHistory history = new CommandHistory(game, unit, playerColor, commandType, targetPosition, resultDescription);
                historyRepository.save(history);
                log.debug("Recorded failed command attempt for game {}, unit {}", gameId, unitId);
            } else {
                log.warn("Cannot record failed command attempt, game {} not found.", gameId);
            }

        } catch (Exception e) {
            log.error("Failed to record command failure history for game {}, unit {}: {}", gameId, unitId, e.getMessage(), e);
        }
    }


    private String handleMoveCommand(Game game, Unit unit, Position targetPosition) {
        validateMoveRules(game, unit, targetPosition);

        Optional<Unit> unitAtDestination = gameService.findUnitAtPosition(game.getId(), targetPosition); // Check again within tx

        if (unitAtDestination.isPresent()) {
            Unit occupant = unitAtDestination.get();
            if (occupant.getPlayerColor() == unit.getPlayerColor()) {
                if (unit.getUnitType() == UnitType.VEHICLE) {
                    log.info("Vehicle {} move to {} blocked by ally unit {}", unit.getId(), targetPosition, occupant.getId());
                    return "FAILED_BLOCKED: Ally unit at destination " + targetPosition;
                } else {
                    throw new PositionOccupiedException("Cannot move to " + targetPosition + ", square occupied by ally unit " + occupant.getId());
                }
            } else {
                if (unit.getUnitType() == UnitType.VEHICLE) {
                    log.info("Vehicle {} runs over enemy unit {} at {}", unit.getId(), occupant.getId(), targetPosition);
                    occupant.setStatus(UnitStatus.DESTROYED);
                    unitRepository.save(occupant);
                    unit.setPosition(targetPosition);
                    return "SUCCESS: Moved to " + targetPosition + ", destroyed enemy unit " + occupant.getId();
                } else {
                    throw new PositionOccupiedException("Cannot move to " + targetPosition + ", square occupied by enemy unit " + occupant.getId());
                }
            }
        } else {
            unit.setPosition(targetPosition);
            log.info("Unit {} moved to {}", unit.getId(), targetPosition);
            return "SUCCESS: Moved to " + targetPosition;
        }
    }


    private String handleShootCommand(Game game, Unit unit, Position targetPosition, Optional<Unit> lockedTargetUnitOpt) {
        validateShootRules(game, unit, targetPosition);


        if (lockedTargetUnitOpt.isPresent()) {
            Unit targetUnit = lockedTargetUnitOpt.get();
            log.info("Unit {} shot hit unit {} at {}", unit.getId(), targetUnit.getId(), targetPosition);
            targetUnit.setStatus(UnitStatus.DESTROYED);
            unitRepository.save(targetUnit);

            String destroyedType = (targetUnit.getPlayerColor() == unit.getPlayerColor()) ? "ally" : "enemy";
            return "SUCCESS: Shot target " + targetPosition + ", destroyed " + destroyedType + " unit " + targetUnit.getId();
        } else {
            log.info("Unit {} shot target {} - missed (no unit)", unit.getId(), targetPosition);
            return "SUCCESS: Shot target " + targetPosition + " - missed";
        }
    }


    private Position validateAndGetTargetPosition(CommandRequestDTO request, String action) {
        if (request.getTargetX() == null || request.getTargetY() == null) {
            throw new InvalidCommandException(action + " command requires targetX and targetY.");
        }
        return new Position(request.getTargetX(), request.getTargetY());
    }

    private void validateUnitIsActive(Unit unit) {
        if (unit.getStatus() != UnitStatus.ACTIVE) {
            throw new InvalidCommandException("Unit " + unit.getId() + " is already destroyed.");
        }
    }

    private void validatePlayerControl(Unit unit, PlayerColor requestingPlayer) {
        if (unit.getPlayerColor() != requestingPlayer) {
            throw new ActionNotAllowedException("Player " + requestingPlayer + " cannot command unit " + unit.getId() + " owned by " + unit.getPlayerColor());
        }
    }

    private void validateActionAllowed(UnitType unitType, CommandType commandType) {
        switch (unitType) {
            case ARCHER:
                if (commandType != CommandType.MOVE && commandType != CommandType.SHOOT) {
                    throw new ActionNotAllowedException("Archer cannot perform: " + commandType);
                }
                break;
            case VEHICLE:
                if (commandType != CommandType.MOVE) {
                    throw new ActionNotAllowedException("Vehicle cannot perform: " + commandType);
                }
                break;
            case CANNON:
                if (commandType != CommandType.SHOOT) {
                    throw new ActionNotAllowedException("Cannon cannot perform: " + commandType);
                }
                break;
        }
    }


    private void checkCooldown(Unit unit, CommandType requestedAction) {
        Instant lastActionTime = unit.getLastActionTimestamp();
        if (lastActionTime == null || lastActionTime.equals(Instant.EPOCH)) {
            return;
        }

        int requiredSeconds;
        try {
            requiredSeconds = config.getCooldownSeconds(unit.getUnitType(), requestedAction);
            if (requiredSeconds == Integer.MAX_VALUE) {
                throw new ActionNotAllowedException(unit.getUnitType() + " cannot perform " + requestedAction + " (cooldown check).");
            }
        } catch (IllegalArgumentException e) {
            throw new ActionNotAllowedException("Cannot determine cooldown for " + unit.getUnitType() + " performing " + requestedAction);
        }


        Instant now = Instant.now();
        Duration timeSinceLastAction = Duration.between(lastActionTime, now);
        Duration requiredCooldown = Duration.ofSeconds(requiredSeconds);

        if (timeSinceLastAction.compareTo(requiredCooldown) < 0) {
            long remainingMillis = requiredCooldown.toMillis() - timeSinceLastAction.toMillis();
            throw new CooldownException(String.format("Unit %d cannot perform %s yet. Cooldown remaining: %.1f seconds.",
                    unit.getId(), requestedAction, remainingMillis / 1000.0));
        }
    }

    private void validateMoveRules(Game game, Unit unit, Position target) {
        if (!boardUtils.isWithinBounds(target, game.getBoardWidth(), game.getBoardHeight())) {
            throw new OutOfBoundsException("Target position " + target + " is outside board boundaries.");
        }

        Position current = unit.getPosition();
        int dx = boardUtils.calculateDistanceX(current, target);
        int dy = boardUtils.calculateDistanceY(current, target);
        int manhattanDistance = dx + dy;


        switch (unit.getUnitType()) {
            case ARCHER:
                if (manhattanDistance != 1 || (dx > 0 && dy > 0)) {
                    throw new InvalidCommandException("Archer can only move 1 square orthogonally. Invalid move from " + current + " to " + target);
                }
                break;
            case VEHICLE:
                if (dx > 0 && dy > 0) { // No diagonal moves
                    throw new InvalidCommandException("Vehicle cannot move diagonally. Invalid move from " + current + " to " + target);
                }
                if (manhattanDistance == 0 || manhattanDistance > 3) {
                    throw new InvalidCommandException("Vehicle must move 1, 2, or 3 squares orthogonally. Invalid move from " + current + " to " + target + " (distance " + manhattanDistance + ")");
                }
                break;
            case CANNON:
                throw new ActionNotAllowedException("Cannon cannot move.");
        }
    }


    private void validateShootRules(Game game, Unit unit, Position target) {
        if (!boardUtils.isWithinBounds(target, game.getBoardWidth(), game.getBoardHeight())) {
            throw new OutOfBoundsException("Target position " + target + " is outside board boundaries.");
        }

        Position current = unit.getPosition();
        int dx = boardUtils.calculateDistanceX(current, target);
        int dy = boardUtils.calculateDistanceY(current, target);

        switch (unit.getUnitType()) {
            case ARCHER:
                int range = config.getUnits().getArcher().getShootRange();
                // shoot n fields (configurable) left/right/down/up
                if (target.equals(current)) {
                    throw new InvalidCommandException("Archer cannot shoot its own square.");
                }
                if ((dx > 0 && dy > 0) || (dx == 0 && dy == 0)) {
                    throw new InvalidCommandException("Archer can only shoot orthogonally (up/down/left/right). Invalid target " + target);
                }
                if (dx > range || dy > range) {
                    throw new InvalidCommandException("Archer target " + target + " is out of range (" + range + "). Distance: x=" + dx + ", y=" + dy);
                }
                break;
            case CANNON:
                int rangeX = config.getUnits().getCannon().getShootRangeX();
                int rangeY = config.getUnits().getCannon().getShootRangeY();
                // shoot n squares left/right and m up/down - can shoot diagonally
                if (target.equals(current)) {
                    throw new InvalidCommandException("Cannon cannot shoot its own square.");
                }
                if (dx > rangeX || dy > rangeY) {
                    throw new InvalidCommandException("Cannon target " + target + " is out of range (max X:" + rangeX + ", max Y:" + rangeY + "). Distance: x=" + dx + ", y=" + dy);
                }
                break;
            case VEHICLE:
                throw new ActionNotAllowedException("Vehicle cannot shoot.");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Unit executeRandomCommand(Long gameId, Long unitId, PlayerColor playerColor) {
        // 1. fetching the unit (with lock, as we intend to potentially command it)
        Unit unit = unitRepository.findByIdAndGameIdForUpdate(unitId, gameId)
                .orElseThrow(() -> new UnitNotFoundException("Unit " + unitId + " not found in game " + gameId));

        Game game = unit.getGame();

        // 2. some validations (same as specific command)
        validateUnitIsActive(unit);
        validatePlayerControl(unit, playerColor);

        // 3. possibly commands
        List<CommandRequestDTO> possibleCommands = generatePossibleCommands(game, unit);

        if (possibleCommands.isEmpty()) {
            log.warn("Unit {} has no possible random commands.", unitId);
            recordFailedCommandAttempt(gameId, unitId, playerColor, CommandType.RANDOM_MOVE, null, "FAILED: No valid random moves found.");
            throw new InvalidCommandException("No valid random moves found for unit " + unitId);
        }

        // 4. select random one
        CommandRequestDTO randomCommand = possibleCommands.get(random.nextInt(possibleCommands.size()));
        randomCommand.setPlayerColor(playerColor); // Ensure player color is set

        log.info("Generated random command for unit {}: {} targeting ({}, {})", unitId, randomCommand.getCommandType(), randomCommand.getTargetX(), randomCommand.getTargetY());



        return executeCommand(gameId, unitId, randomCommand);
    }

    private List<CommandRequestDTO> generatePossibleCommands(Game game, Unit unit) {
        List<CommandRequestDTO> commands = new ArrayList<>();
        Position current = unit.getPosition();
        int width = game.getBoardWidth();
        int height = game.getBoardHeight();

        if (unit.getUnitType() == UnitType.ARCHER) {
            int[][] moves = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
            for (int[] move : moves) {
                Position target = new Position(current.getX() + move[0], current.getY() + move[1]);
                if (isValidMoveTarget(game, unit, target)) {
                    commands.add(createCommandDTO(CommandType.MOVE, target));
                }
            }
        } else if (unit.getUnitType() == UnitType.VEHICLE) {
            int[] distances = {1, 2, 3};
            int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
            for (int dist : distances) {
                for (int[] dir : directions) {
                    Position target = new Position(current.getX() + dir[0] * dist, current.getY() + dir[1] * dist);
                    if (isValidMoveTarget(game, unit, target)) {
                        commands.add(createCommandDTO(CommandType.MOVE, target));
                    }
                }
            }
        }

        if (unit.getUnitType() == UnitType.ARCHER) {
            int range = config.getUnits().getArcher().getShootRange();
            int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
            for (int dist = 1; dist <= range; dist++) {
                for (int[] dir : directions) {
                    Position target = new Position(current.getX() + dir[0] * dist, current.getY() + dir[1] * dist);
                    if (isValidShootTarget(game, unit, target)) {
                        commands.add(createCommandDTO(CommandType.SHOOT, target));
                    }
                }
            }
        } else if (unit.getUnitType() == UnitType.CANNON) {
            int rangeX = config.getUnits().getCannon().getShootRangeX();
            int rangeY = config.getUnits().getCannon().getShootRangeY();
            for (int x = -rangeX; x <= rangeX; x++) {
                for (int y = -rangeY; y <= rangeY; y++) {
                    if (x == 0 && y == 0) continue;
                    Position target = new Position(current.getX() + x, current.getY() + y);
                    if (isValidShootTarget(game, unit, target)) {
                        commands.add(createCommandDTO(CommandType.SHOOT, target));
                    }
                }
            }
        }


        return commands;
    }

    private boolean isValidMoveTarget(Game game, Unit unit, Position target) {
        try {
            validateMoveRules(game, unit, target);
            return true;
        } catch (OutOfBoundsException | InvalidCommandException | ActionNotAllowedException e) {
            return false;
        }
    }

    private boolean isValidShootTarget(Game game, Unit unit, Position target) {
        try {
            validateShootRules(game, unit, target);
            return true;
        } catch (OutOfBoundsException | InvalidCommandException | ActionNotAllowedException e) {
            return false;
        }
    }

    private CommandRequestDTO createCommandDTO(CommandType type, Position target) {
        CommandRequestDTO dto = new CommandRequestDTO();
        dto.setCommandType(type);
        dto.setTargetX(target.getX());
        dto.setTargetY(target.getY());
        return dto;
    }
}
