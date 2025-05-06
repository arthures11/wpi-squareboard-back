package com.bryja.wpisquareboardback.repository;

import com.bryja.wpisquareboardback.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UnitRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UnitRepository unitRepository;

    private Game testGame;
    private Unit whiteArcher;
    private Unit blackVehicle;
    private Unit destroyedWhiteCannon;
    private Unit whiteCannon;

    @BeforeEach
    void setUp() {
        testGame = new Game(20,20);
        testGame = entityManager.persistFlushFind(testGame);

        whiteArcher = new Archer(testGame, PlayerColor.WHITE, new Position(1, 1));
        whiteArcher.setStatus(UnitStatus.ACTIVE);
        entityManager.persist(whiteArcher);

        blackVehicle = new Vehicle(testGame, PlayerColor.BLACK, new Position(5, 5));
        blackVehicle.setStatus(UnitStatus.ACTIVE);
        entityManager.persist(blackVehicle);

        destroyedWhiteCannon = new Cannon(testGame, PlayerColor.WHITE, new Position(2, 2));
        destroyedWhiteCannon.setStatus(UnitStatus.DESTROYED);
        entityManager.persist(destroyedWhiteCannon);

        entityManager.flush();
    }

    @Test
    void findByGameIdAndPlayerColorAndStatus_findsActiveWhiteUnits() {
        List<Unit> units = unitRepository.findByGameIdAndPlayerColorAndStatus(testGame.getId(), PlayerColor.WHITE, UnitStatus.ACTIVE);
        assertThat(units).hasSize(1).extracting(Unit::getId).containsExactly(whiteArcher.getId());
        assertThat(units.get(0)).isInstanceOf(Archer.class);
    }


    @Test
    void findByGameIdAndStatus_findsAllActiveUnits() {
        List<Unit> units = unitRepository.findByGameIdAndStatus(testGame.getId(), UnitStatus.ACTIVE);
        assertThat(units).hasSize(2).extracting(Unit::getId).containsExactlyInAnyOrder(whiteArcher.getId(), blackVehicle.getId());
    }

    @Test
    void findByGameIdAndPositionAndStatus_findsUnitAtPosition() {
        Position targetPos = new Position(5, 5);
        Optional<Unit> unitOpt = unitRepository.findByGameIdAndPositionAndStatus(testGame.getId(), targetPos, UnitStatus.ACTIVE);
        assertThat(unitOpt).isPresent();
        assertThat(unitOpt.get().getId()).isEqualTo(blackVehicle.getId());
        assertThat(unitOpt.get()).isInstanceOf(Vehicle.class);
    }

    @Test
    void findByGameIdAndPositionAndStatus_whenNoUnitAtPosition_returnsEmpty() {
        Position emptyPos = new Position(0, 0);
        Optional<Unit> unitOpt = unitRepository.findByGameIdAndPositionAndStatus(testGame.getId(), emptyPos, UnitStatus.ACTIVE);
        assertThat(unitOpt).isNotPresent();
    }

    @Test
    void findByGameIdAndPositionAndStatus_whenUnitDestroyed_returnsEmptyForActiveStatus() {
        Position destroyedPos = new Position(2, 2);
        Optional<Unit> unitOpt = unitRepository.findByGameIdAndPositionAndStatus(testGame.getId(), destroyedPos, UnitStatus.ACTIVE);
        assertThat(unitOpt).isNotPresent();
    }

    @Test
    void findByIdAndGameId_findsCorrectUnit() {
        Optional<Unit> unitOpt = unitRepository.findByIdAndGameId(whiteArcher.getId(), testGame.getId());
        assertThat(unitOpt).isPresent();
        assertThat(unitOpt.get().getId()).isEqualTo(whiteArcher.getId());
    }

    @Test
    void findByIdAndGameId_whenWrongGameId_returnsEmpty() {
        Optional<Unit> unitOpt = unitRepository.findByIdAndGameId(whiteArcher.getId(), testGame.getId() + 99);
        assertThat(unitOpt).isNotPresent();
    }

}
