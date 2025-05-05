package com.bryja.wpisquareboardback.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("VEHICLE")
@NoArgsConstructor
public class Vehicle extends Unit {
    public Vehicle(Game game, PlayerColor playerColor, Position position) {
        super(game, playerColor, position);
        this.setUnitType(UnitType.VEHICLE);
    }
}
