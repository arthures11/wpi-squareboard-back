package com.bryja.wpisquareboardback.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("CANNON")
@NoArgsConstructor
public class Cannon extends Unit {
    public Cannon(Game game, PlayerColor playerColor, Position position) {
        super(game, playerColor, position);
        this.setUnitType(UnitType.CANNON);
    }
}
