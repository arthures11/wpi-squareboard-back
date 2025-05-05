package com.bryja.wpisquareboardback.mapper;

import com.bryja.wpisquareboardback.dto.*;
import com.bryja.wpisquareboardback.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DtoMapper {

    public UnitDTO toUnitDTO(Unit unit) {
        if (unit == null) return null;
        UnitDTO dto = new UnitDTO();
        dto.setId(unit.getId());
        dto.setGameId(unit.getGame() != null ? unit.getGame().getId() : null);
        dto.setPlayerColor(unit.getPlayerColor());
        dto.setUnitType(unit.getUnitType());
        dto.setPosition(unit.getPosition());
        dto.setStatus(unit.getStatus());
        dto.setMoveCount(unit.getMoveCount());
        dto.setLastActionTimestamp(unit.getLastActionTimestamp());
        dto.setVersion(unit.getVersion());
        return dto;
    }

    public List<UnitDTO> toUnitDTOList(List<Unit> units) {
        return units.stream().map(this::toUnitDTO).collect(Collectors.toList());
    }

    public GameDTO toGameDTO(Game game) {
        if (game == null) return null;
        GameDTO dto = new GameDTO();
        dto.setId(game.getId());
        dto.setBoardHeight(game.getBoardHeight());
        dto.setBoardWidth(game.getBoardWidth());
        dto.setCreatedAt(game.getCreatedAt());
        dto.setFinishedAt(game.getFinishedAt());
        dto.setStatus(game.getStatus());
        return dto;
    }
    public List<GameDTO> toGameDTOList(List<Game> games) {
        return games.stream().map(this::toGameDTO).collect(Collectors.toList());
    }

}
