package com.bryja.wpisquareboardback.service;

import com.bryja.wpisquareboardback.repository.*;
import com.bryja.wpisquareboardback.model.*;
import com.bryja.wpisquareboardback.config.*;
import com.bryja.wpisquareboardback.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final GameRepository gameRepository;
    private final UnitRepository unitRepository;
    private final GameConfigProperties config;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public Game createNewGame() {
        log.info("Attempting to create a new game.");


        gameRepository.findByStatus(GameStatus.ACTIVE).ifPresent(activeGame -> {
            log.warn("Found existing active game ID: {}. Marking as FINISHED.", activeGame.getId());
            activeGame.setStatus(GameStatus.FINISHED);
            activeGame.setFinishedAt(Instant.now());
            gameRepository.save(activeGame);

        });


        Game newGame = new Game(config.getBoard().getWidth(), config.getBoard().getHeight());
        placeInitialUnits(newGame);

        Game savedGame = gameRepository.save(newGame);
        log.info("Successfully created and saved new game with ID: {}", savedGame.getId());
        return savedGame;
    }

    public Game findActiveGameOrFail() {
        return gameRepository.findByStatus(GameStatus.ACTIVE)
                .orElseThrow(() -> new GameNotFoundException("No active game found. Please start a new game."));
    }
    public Game findGameByIdOrFail(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found with ID: " + gameId));
    }


    private void placeInitialUnits(Game game) {
        int width = game.getBoardWidth();
        int height = game.getBoardHeight();
        int maxUnits = width * height;
        int totalInitialUnits = config.getTotalInitialUnits() * 2; // For both players

        if (totalInitialUnits > maxUnits) {
            throw new InvalidCommandException(String.format(
                    "Cannot place %d units on a %dx%d board (max %d). Check configuration.",
                    totalInitialUnits, width, height, maxUnits));
        }

        Set<Position> occupiedPositions = new HashSet<>();

        placeUnitsForPlayer(game, PlayerColor.WHITE, config.getUnits().getInitial().getArchers(), UnitType.ARCHER, occupiedPositions);
        placeUnitsForPlayer(game, PlayerColor.WHITE, config.getUnits().getInitial().getVehicles(), UnitType.VEHICLE, occupiedPositions);
        placeUnitsForPlayer(game, PlayerColor.WHITE, config.getUnits().getInitial().getCannons(), UnitType.CANNON, occupiedPositions);

        placeUnitsForPlayer(game, PlayerColor.BLACK, config.getUnits().getInitial().getArchers(), UnitType.ARCHER, occupiedPositions);
        placeUnitsForPlayer(game, PlayerColor.BLACK, config.getUnits().getInitial().getVehicles(), UnitType.VEHICLE, occupiedPositions);
        placeUnitsForPlayer(game, PlayerColor.BLACK, config.getUnits().getInitial().getCannons(), UnitType.CANNON, occupiedPositions);

        log.info("Placed {} initial units for game.", game.getUnits().size());
    }

    private void placeUnitsForPlayer(Game game, PlayerColor color, int count, UnitType type, Set<Position> occupied) {
        for (int i = 0; i < count; i++) {
            Position randomPos;
            int attempts = 0;
            do {
                randomPos = new Position(
                        random.nextInt(game.getBoardWidth()),
                        random.nextInt(game.getBoardHeight())
                );
                attempts++;
                if (attempts > game.getBoardWidth() * game.getBoardHeight() * 2) { // Safety break
                    log.error("Could not find unique position after {} attempts for {} {} unit {}. Check board size/unit count.", attempts, color, type, i+1);
                    throw new RuntimeException("Failed to place units randomly, too many attempts. Board might be too full.");
                }
            } while (occupied.contains(randomPos));

            occupied.add(randomPos);
            Unit unit;
            switch (type) {
                case ARCHER: unit = new Archer(game, color, randomPos); break;
                case VEHICLE: unit = new Vehicle(game, color, randomPos); break;
                case CANNON: unit = new Cannon(game, color, randomPos); break;
                default: throw new IllegalArgumentException("Unknown unit type to place: " + type);
            }
            unit.setUnitType(type);
            game.addUnit(unit);
            // log.debug("Placed {} {} at {},{}", color, type, randomPos.getX(), randomPos.getY());
        }
    }


    public Optional<Unit> findUnitAtPosition(Long gameId, Position position) {
        if (position == null) return Optional.empty();
        return unitRepository.findByGameIdAndPositionAndStatus(gameId, position, UnitStatus.ACTIVE);
    }
    public Optional<Unit> findUnitAtPositionForUpdate(Long gameId, Position position) {
        if (position == null) return Optional.empty();
        return unitRepository.findByGameIdAndPositionAndStatusForUpdate(gameId, position, UnitStatus.ACTIVE);
    }
}
