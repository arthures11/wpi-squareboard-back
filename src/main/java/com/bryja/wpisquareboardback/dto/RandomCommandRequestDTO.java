package com.bryja.wpisquareboardback.dto;

import com.bryja.wpisquareboardback.model.PlayerColor;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class RandomCommandRequestDTO {
    @NotNull(message = "Player color is required")
    private PlayerColor playerColor;
}
