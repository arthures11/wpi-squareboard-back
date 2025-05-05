package com.bryja.wpisquareboardback.dto;

import com.bryja.wpisquareboardback.model.*;
import lombok.Data;
import java.time.Instant;


@Data
public class UnitDTO {
    private Long id;
    private Long gameId;
    private PlayerColor playerColor;
    private UnitType unitType;
    private Position position;
    private UnitStatus status;
    private int moveCount;
    private Instant lastActionTimestamp;
    private Long version;
}
