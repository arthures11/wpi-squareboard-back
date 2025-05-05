package com.bryja.wpisquareboardback.dto;

import com.bryja.wpisquareboardback.model.GameStatus;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class GameDTO {
    private Long id;
    private Instant createdAt;
    private Instant finishedAt;
    private int boardWidth;
    private int boardHeight;
    private GameStatus status;
    // private List<UnitDTO> units;
}
