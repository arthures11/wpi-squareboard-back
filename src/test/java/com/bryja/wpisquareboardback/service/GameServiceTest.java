package com.bryja.wpisquareboardback.service;

import com.bryja.wpisquareboardback.config.GameConfigProperties;
import com.bryja.wpisquareboardback.exception.InvalidCommandException;
import com.bryja.wpisquareboardback.model.*;
import com.bryja.wpisquareboardback.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Initialize mocks
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;
    @Mock
    private GameConfigProperties defaultConfig;

    @InjectMocks
    private GameService gameService;

    @BeforeEach
    void setUp() {

        lenient().when(defaultConfig.getBoard()).thenReturn(mock(GameConfigProperties.Board.class));
        lenient().when(defaultConfig.getBoard().getWidth()).thenReturn(10);
        lenient().when(defaultConfig.getBoard().getHeight()).thenReturn(10);

        lenient().when(defaultConfig.getUnits()).thenReturn(mock(GameConfigProperties.Units.class));
        lenient().when(defaultConfig.getUnits().getInitial()).thenReturn(mock(GameConfigProperties.Units.Initial.class));
        lenient().when(defaultConfig.getUnits().getInitial().getArchers()).thenReturn(2);
        lenient().when(defaultConfig.getUnits().getInitial().getVehicles()).thenReturn(1);
        lenient().when(defaultConfig.getUnits().getInitial().getCannons()).thenReturn(1);

    }

    @Test
    void createNewGame_withNullConfig_usesDefaultsAndArchivesOldGame() {
        Game oldActiveGame = mock(Game.class);
        when(gameRepository.findByStatus(GameStatus.ACTIVE)).thenReturn(Optional.of(oldActiveGame));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(defaultConfig.getBoard().getWidth()).thenReturn(100);
        when(defaultConfig.getBoard().getHeight()).thenReturn(100);
        when(defaultConfig.getUnits().getInitial().getArchers()).thenReturn(10);
        when(defaultConfig.getUnits().getInitial().getVehicles()).thenReturn(10);
        when(defaultConfig.getUnits().getInitial().getCannons()).thenReturn(10);

        Game newGame = gameService.createNewGame();

        assertThat(newGame).isNotNull();
        assertThat(newGame.getStatus()).isEqualTo(GameStatus.ACTIVE);
        assertThat(newGame.getBoardWidth()).isEqualTo(100);
        assertThat(newGame.getBoardHeight()).isEqualTo(100);
        assertThat(newGame.getUnits()).hasSize((10+ 10 + 10) * 2);

        verify(gameRepository).findByStatus(GameStatus.ACTIVE);
        verify(oldActiveGame).setStatus(GameStatus.FINISHED);
        verify(oldActiveGame).setFinishedAt(any(java.time.Instant.class));
        verify(gameRepository).save(oldActiveGame);

        verify(gameRepository).save(newGame);
    }

    @Test
    void createNewGame_whenTooManyUnitsForBoard_throwsInvalidCommandException() {


        when(defaultConfig.getBoard().getWidth()).thenReturn(10);
        when(defaultConfig.getBoard().getHeight()).thenReturn(10);

        when(defaultConfig.getUnits().getInitial().getArchers()).thenReturn(20);
        when(defaultConfig.getUnits().getInitial().getVehicles()).thenReturn(20);
        when(defaultConfig.getUnits().getInitial().getCannons()).thenReturn(20);

        when(gameRepository.findByStatus(GameStatus.ACTIVE)).thenReturn(Optional.empty());


        assertThatThrownBy(() -> gameService.createNewGame())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to place units randomly, too many attempts. Board might be too full.");

        verify(gameRepository, never()).save(any(Game.class));
    }




    @Test
    void placeInitialUnits_placesCorrectNumberOfUnitsWithoutOverlap() {


        when(defaultConfig.getUnits().getInitial().getArchers()).thenReturn(1);
        when(defaultConfig.getUnits().getInitial().getVehicles()).thenReturn(1);
        when(defaultConfig.getUnits().getInitial().getCannons()).thenReturn(0);

        Game testGame = new Game(7,7);

        when(gameRepository.save(any(Game.class))).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> {
            Game createdGame = gameService.createNewGame();

            ArgumentCaptor<Game> gameCaptor = ArgumentCaptor.forClass(Game.class);
            verify(gameRepository).save(gameCaptor.capture());
            Game savedGame = gameCaptor.getValue();

            assertThat(savedGame.getUnits()).hasSize((
                    defaultConfig.getUnits().getInitial().getArchers() +
                            defaultConfig.getUnits().getInitial().getVehicles() +
                            defaultConfig.getUnits().getInitial().getCannons()
            ) * 2);


            long distinctPositions = savedGame.getUnits().stream()
                    .map(Unit::getPosition)
                    .distinct()
                    .count();
            assertThat(distinctPositions).isEqualTo(savedGame.getUnits().size());

        });


    }
}
