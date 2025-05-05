package com.bryja.wpisquareboardback.repository;

import com.bryja.wpisquareboardback.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class GameRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GameRepository gameRepository;

    @Test
    void findByStatus_whenActiveGameExists_returnsGame() {
        Game activeGame = new Game(10,10);
        activeGame.setStatus(GameStatus.ACTIVE);
        entityManager.persistAndFlush(activeGame);

        Game finishedGame = new Game(23,22);
        finishedGame.setStatus(GameStatus.FINISHED);
        entityManager.persistAndFlush(finishedGame);

        Optional<Game> foundGame = gameRepository.findByStatus(GameStatus.ACTIVE);

        assertThat(foundGame).isPresent();
        assertThat(foundGame.get().getId()).isEqualTo(activeGame.getId());
        assertThat(foundGame.get().getStatus()).isEqualTo(GameStatus.ACTIVE);
        assertThat(foundGame.get().getBoardWidth()).isEqualTo(10);
        assertThat(foundGame.get().getBoardHeight()).isEqualTo(10);
        assertThat(foundGame.get().getUnits()).isEmpty();
        assertThat(finishedGame.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(finishedGame.getUnits()).isEmpty();
        assertThat(finishedGame.getBoardWidth()).isEqualTo(23);
        assertThat(finishedGame.getBoardHeight()).isEqualTo(22);
    }

    @Test
    void findByStatus_whenNoActiveGameExists_returnsEmpty() {
        Game finishedGame = new Game(5,5);
        finishedGame.setStatus(GameStatus.FINISHED);
        entityManager.persistAndFlush(finishedGame);
        Optional<Game> foundGame = gameRepository.findByStatus(GameStatus.ACTIVE);
        assertThat(foundGame).isNotPresent();
    }
}
