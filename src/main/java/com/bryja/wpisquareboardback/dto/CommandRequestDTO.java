package com.bryja.wpisquareboardback.dto;

import com.bryja.wpisquareboardback.model.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommandRequestDTO {
    @NotNull(message = "Player color is required")
    private PlayerColor playerColor;

    @NotNull(message = "Command type is required")
    private CommandType commandType;

    private Integer targetX;
    private Integer targetY;

}
