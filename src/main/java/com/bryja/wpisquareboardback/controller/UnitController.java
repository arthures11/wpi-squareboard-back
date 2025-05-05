package com.bryja.wpisquareboardback.controller;

import com.bryja.wpisquareboardback.dto.*;
import com.bryja.wpisquareboardback.mapper.*;
import com.bryja.wpisquareboardback.model.*;
import com.bryja.wpisquareboardback.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games/{gameId}/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;
    private final CommandService commandService;
    private final DtoMapper dtoMapper;

    @GetMapping
    public ResponseEntity<List<UnitDTO>> listUnits(
            @PathVariable Long gameId,
            @RequestParam(required = false) PlayerColor playerColor) {

        List<Unit> units;
        if (playerColor != null) {
            units = unitService.getActiveUnitsForPlayer(gameId, playerColor);
        } else {
            units = unitService.getAllActiveUnits(gameId);
        }
        return ResponseEntity.ok(dtoMapper.toUnitDTOList(units));
    }

    @GetMapping("/{unitId}")
    public ResponseEntity<UnitDTO> getUnitDetails(
            @PathVariable Long gameId,
            @PathVariable Long unitId) {
        Unit unit = unitService.findUnitByIdAndGameId(unitId, gameId);
        return ResponseEntity.ok(dtoMapper.toUnitDTO(unit));
    }


    @PostMapping("/{unitId}/command")
    public ResponseEntity<UnitDTO> executeSpecificCommand(
            @PathVariable Long gameId,
            @PathVariable Long unitId,
            @Valid @RequestBody CommandRequestDTO commandRequest) {

        Unit updatedUnit = commandService.executeCommand(gameId, unitId, commandRequest);
        return ResponseEntity.ok(dtoMapper.toUnitDTO(updatedUnit));
    }

    @PostMapping("/{unitId}/command/random")
    public ResponseEntity<UnitDTO> executeRandomCommand(
            @PathVariable Long gameId,
            @PathVariable Long unitId,
            @Valid @RequestBody RandomCommandRequestDTO randomRequest) {

        Unit updatedUnit = commandService.executeRandomCommand(gameId, unitId, randomRequest.getPlayerColor());
        return ResponseEntity.ok(dtoMapper.toUnitDTO(updatedUnit));
    }
}
