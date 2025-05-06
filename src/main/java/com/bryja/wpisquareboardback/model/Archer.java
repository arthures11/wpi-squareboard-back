package com.bryja.wpisquareboardback.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("ARCHER")
@NoArgsConstructor
public class Archer extends Unit {
    public Archer(Game game, PlayerColor playerColor, Position position) {
        super(game, playerColor, position);
        this.setUnitType(UnitType.ARCHER);
    }
}
