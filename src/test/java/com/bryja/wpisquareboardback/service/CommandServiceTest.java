package com.bryja.wpisquareboardback.service;

import com.bryja.wpisquareboardback.config.GameConfigProperties;
import com.bryja.wpisquareboardback.dto.CommandRequestDTO;
import com.bryja.wpisquareboardback.exception.*;
import com.bryja.wpisquareboardback.model.*;
import com.bryja.wpisquareboardback.repository.CommandHistoryRepository;
import com.bryja.wpisquareboardback.repository.GameRepository;
import com.bryja.wpisquareboardback.repository.UnitRepository;
import com.bryja.wpisquareboardback.util.BoardUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommandServiceTest {

    @Mock
    private UnitRepository unitRepository;
    @Mock
    private CommandHistoryRepository historyRepository;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private GameService gameService;

    @Spy
    private GameConfigProperties gameConfigProperties;

    @Spy
    private BoardUtils boardUtils;

    @InjectMocks
    private CommandService commandService;

    private Game testGame;
    private Archer whiteArcher;
    private Vehicle blackVehicle;
    private final Long GAME_ID = 1L;
    private final Long ARCHER_ID = 10L;
    private final Long VEHICLE_ID = 20L;

    @BeforeEach
    void setUp() {
        testGame = mock(Game.class);
        lenient().when(testGame.getId()).thenReturn(GAME_ID);
        lenient().when(testGame.getBoardWidth()).thenReturn(10);
        lenient().when(testGame.getBoardHeight()).thenReturn(10);
        whiteArcher = spy(Archer.class);
        lenient().when(whiteArcher.getId()).thenReturn(ARCHER_ID);
        lenient().when(whiteArcher.getGame()).thenReturn(testGame);
        lenient().when(whiteArcher.getPlayerColor()).thenReturn(PlayerColor.WHITE);
        lenient().when(whiteArcher.getUnitType()).thenReturn(UnitType.ARCHER);
        lenient().when(whiteArcher.getStatus()).thenReturn(UnitStatus.ACTIVE);
        lenient().when(whiteArcher.getPosition()).thenReturn(new Position(1, 1));
        lenient().when(whiteArcher.getLastActionTimestamp()).thenReturn(Instant.EPOCH);

        blackVehicle = spy(Vehicle.class);
        lenient().when(blackVehicle.getId()).thenReturn(VEHICLE_ID);
        lenient().when(blackVehicle.getGame()).thenReturn(testGame);
        lenient().when(blackVehicle.getPlayerColor()).thenReturn(PlayerColor.BLACK);
        lenient().when(blackVehicle.getUnitType()).thenReturn(UnitType.VEHICLE);
        lenient().when(blackVehicle.getStatus()).thenReturn(UnitStatus.ACTIVE);
        lenient().when(blackVehicle.getPosition()).thenReturn(new Position(1, 5));
        lenient().when(blackVehicle.getLastActionTimestamp()).thenReturn(Instant.EPOCH);

        //lenient().when(boardUtils.isWithinBounds(any(Position.class), eq(10), eq(10))).thenReturn(true);
    }

    @Test
    void executeCommand_moveArcherSuccess_updatesPositionAndTimestamp() {
        CommandRequestDTO moveCommand = new CommandRequestDTO();
        moveCommand.setPlayerColor(PlayerColor.WHITE);
        moveCommand.setCommandType(CommandType.MOVE);
        moveCommand.setTargetX(0);
        moveCommand.setTargetY(1);

        Position targetPos = new Position(0, 1);

        when(unitRepository.findByIdAndGameIdForUpdate(ARCHER_ID, GAME_ID)).thenReturn(Optional.of(whiteArcher));
        lenient().when(gameService.findUnitAtPositionForUpdate(GAME_ID, targetPos)).thenReturn(Optional.empty());
        when(unitRepository.save(whiteArcher)).thenReturn(whiteArcher);
        when(historyRepository.save(any(CommandHistory.class))).thenReturn(mock(CommandHistory.class));

        Unit resultUnit = commandService.executeCommand(GAME_ID, ARCHER_ID, moveCommand);

        assertThat(resultUnit).isNotNull();
        assertThat(resultUnit.getId()).isEqualTo(ARCHER_ID);
        verify(whiteArcher).setPosition(eq(targetPos));
        verify(whiteArcher).setLastActionTimestamp(any(Instant.class));
        verify(whiteArcher).setMoveCount(1);
        verify(unitRepository).save(whiteArcher);
        verify(historyRepository).save(any(CommandHistory.class));
    }

    @Test
    void executeCommand_whenCooldownNotMet_throwsCooldownException() {
        CommandRequestDTO moveCommand = new CommandRequestDTO();
        moveCommand.setPlayerColor(PlayerColor.WHITE);
        moveCommand.setCommandType(CommandType.MOVE);
        moveCommand.setTargetX(1);
        moveCommand.setTargetY(2);

        when(whiteArcher.getLastActionTimestamp()).thenReturn(Instant.now().minusSeconds(2));

        when(unitRepository.findByIdAndGameIdForUpdate(ARCHER_ID, GAME_ID)).thenReturn(Optional.of(whiteArcher));
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(testGame));
        assertThatThrownBy(() -> commandService.executeCommand(GAME_ID, ARCHER_ID, moveCommand))
                .isInstanceOf(CooldownException.class)
                .hasMessageContaining("Cooldown remaining");

        verify(whiteArcher, never()).setPosition(any());
        verify(unitRepository, never()).save(any());
        verify(historyRepository).save(any(CommandHistory.class));
    }

    @Test
    void executeCommand_vehicleRunsOverEnemy_destroysEnemyAndMoves() {
        CommandRequestDTO moveCommand = new CommandRequestDTO();
        moveCommand.setPlayerColor(PlayerColor.BLACK);
        moveCommand.setCommandType(CommandType.MOVE);
        moveCommand.setTargetX(1);
        moveCommand.setTargetY(4);
        lenient().when(whiteArcher.getPosition()).thenReturn(new Position(1, 4));
        Position targetPos = new Position(1, 4);


        when(unitRepository.findByIdAndGameIdForUpdate(VEHICLE_ID, GAME_ID)).thenReturn(Optional.of(blackVehicle));
        when(unitRepository.save(any(Unit.class))).thenAnswer(i -> i.getArgument(0));
        when(historyRepository.save(any(CommandHistory.class))).thenReturn(mock(CommandHistory.class));

        when(gameService.findUnitAtPosition(GAME_ID, new Position(1, 4))).thenReturn(Optional.of(whiteArcher));

        commandService.executeCommand(GAME_ID, VEHICLE_ID, moveCommand);

        verify(blackVehicle).setPosition(eq(targetPos));
        verify(blackVehicle).setLastActionTimestamp(any(Instant.class));
        verify(whiteArcher).setStatus(eq(UnitStatus.DESTROYED));
        verify(unitRepository).save(blackVehicle);
        verify(unitRepository).save(whiteArcher);


        verify(historyRepository).save(argThat(h -> h.getResultDescription().contains("destroyed enemy unit")));
    }


    @Test
    void executeCommand_whenOptimisticLockExceptionOccurs_throwsConcurrencyConflictException() {
        CommandRequestDTO moveCommand = new CommandRequestDTO();
        moveCommand.setPlayerColor(PlayerColor.WHITE);
        moveCommand.setCommandType(CommandType.MOVE);
        moveCommand.setTargetX(1);
        moveCommand.setTargetY(2);
        Position targetPos = new Position(1, 2);

        when(unitRepository.findByIdAndGameIdForUpdate(ARCHER_ID, GAME_ID)).thenReturn(Optional.of(whiteArcher));
        lenient().when(gameService.findUnitAtPositionForUpdate(GAME_ID, targetPos)).thenReturn(Optional.empty());
        doThrow(new OptimisticLockingFailureException("Simulated lock conflict"))
                .when(unitRepository).save(whiteArcher);

        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(testGame));
        when(unitRepository.findById(ARCHER_ID)).thenReturn(Optional.of(whiteArcher));

        assertThatThrownBy(() -> commandService.executeCommand(GAME_ID, ARCHER_ID, moveCommand))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Command execution failed unexpectedly");

        verify(historyRepository).save(any(CommandHistory.class));
    }

    // todo tests:
    // - Moving blocked by Ally
    // - Shooting and hitting
    // - Shooting and missing
    // - Commanding opponent unit (ActionNotAllowedException)
    // - Moving out of bounds (OutOfBoundsException)
    // - Invalid move distance/direction (InvalidCommandException)
    // - Invalid shoot range/direction (InvalidCommandException)
    // - Commanding destroyed unit (InvalidCommandException)
    // - Random command generation/execution paths
    // - Tests for Cannon logic
}
