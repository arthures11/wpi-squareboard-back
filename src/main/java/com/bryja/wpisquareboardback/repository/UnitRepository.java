package com.bryja.wpisquareboardback.repository;

import com.bryja.wpisquareboardback.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType; // Ensure using Jakarta Persistence LockModeType
import java.util.List;
import java.util.Optional;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {

    List<Unit> findByGameIdAndPlayerColorAndStatus(Long gameId, PlayerColor playerColor, UnitStatus status);

    List<Unit> findByGameIdAndStatus(Long gameId, UnitStatus status);

    Optional<Unit> findByGameIdAndPositionAndStatus(Long gameId, Position position, UnitStatus status);


    Optional<Unit> findByIdAndGameId(Long unitId, Long gameId);

    // @Lock(LockModeType.PESSIMISTIC_WRITE)
    // Optional<Unit> findByGameIdAndPositionAndStatusWithLock(Long gameId, Position position, UnitStatus status);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT u FROM Unit u WHERE u.id = :unitId AND u.game.id = :gameId")
    Optional<Unit> findByIdAndGameIdForUpdate(Long unitId, Long gameId);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT u FROM Unit u WHERE u.game.id = :gameId AND u.position = :position AND u.status = :status")
    Optional<Unit> findByGameIdAndPositionAndStatusForUpdate(Long gameId, Position position, UnitStatus status);



//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    @Query("SELECT u FROM Unit u WHERE u.id = :unitId AND u.game.id = :gameId")
//    Optional<Unit> findByIdAndGameIdForCommand(Long unitId, Long gameId);
//
//
//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    @Query("SELECT u FROM Unit u WHERE u.game.id = :gameId AND u.position = :position AND u.status = :status")
//    Optional<Unit> findUnitAtPositionForUpdateWithLock(Long gameId, Position position, UnitStatus status);


}
