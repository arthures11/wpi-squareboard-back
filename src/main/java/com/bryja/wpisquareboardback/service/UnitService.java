package com.bryja.wpisquareboardback.service;

import com.bryja.wpisquareboardback.repository.*;
import com.bryja.wpisquareboardback.model.*;
import com.bryja.wpisquareboardback.config.*;
import com.bryja.wpisquareboardback.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UnitService {

    private final UnitRepository unitRepository;
    private final GameRepository gameRepository;

    @Transactional(readOnly = true)
    public List<Unit> getActiveUnitsForPlayer(Long gameId, PlayerColor playerColor) {
        if (!gameRepository.existsById(gameId)) {
            throw new GameNotFoundException("Game not found with ID: " + gameId);
        }
        return unitRepository.findByGameIdAndPlayerColorAndStatus(gameId, playerColor, UnitStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Unit> getAllActiveUnits(Long gameId) {
        if (!gameRepository.existsById(gameId)) {
            throw new GameNotFoundException("Game not found with ID: " + gameId);
        }
        return unitRepository.findByGameIdAndStatus(gameId, UnitStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Unit findUnitByIdAndGameId(Long unitId, Long gameId) {
        return unitRepository.findByIdAndGameId(unitId, gameId)
                .orElseThrow(() -> new UnitNotFoundException("Unit not found with ID: " + unitId + " in game ID: " + gameId));
    }

}
