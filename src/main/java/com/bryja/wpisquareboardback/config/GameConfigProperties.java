package com.bryja.wpisquareboardback.config;

import com.bryja.wpisquareboardback.model.CommandType;
import com.bryja.wpisquareboardback.model.UnitType;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "game")
@Getter
@Setter
@Validated
public class GameConfigProperties {

    private Board board = new Board();
    private Units units = new Units();

    @Getter @Setter
    public static class Board {
        @Min(1) private int width = 10;
        @Min(1) private int height = 10;
    }

    @Getter @Setter
    public static class Units {
        private Initial initial = new Initial();
        private Archer archer = new Archer();
        private Vehicle vehicle = new Vehicle();
        private Cannon cannon = new Cannon();

        @Getter @Setter
        public static class Initial {
            @Min(0) private int archers = 4;
            @Min(0) private int vehicles = 5;
            @Min(0) private int cannons = 2;
        }

        @Getter @Setter
        public static class Archer {
            @Min(1) private int moveCooldownSeconds = 5;
            @Min(1) private int shootCooldownSeconds = 10;
            @Min(1) private int shootRange = 4;
        }

        @Getter @Setter
        public static class Vehicle {
            @Min(1) private int moveCooldownSeconds = 7;
        }

        @Getter @Setter
        public static class Cannon {
            @Min(1) private int shootCooldownSeconds = 13;
            @Min(1) private int shootRangeX = 5;
            @Min(1) private int shootRangeY = 5;
        }
    }

    public int getTotalInitialUnits() {
        return units.getInitial().getArchers() +
                units.getInitial().getVehicles() +
                units.getInitial().getCannons();
    }

    public int getCooldownSeconds(UnitType type, CommandType action) {
        if (action == CommandType.MOVE) {
            switch (type) {
                case ARCHER: return units.getArcher().getMoveCooldownSeconds();
                case VEHICLE: return units.getVehicle().getMoveCooldownSeconds();
                case CANNON: return Integer.MAX_VALUE;
                default: throw new IllegalArgumentException("Unknown unit type for move: " + type);
            }
        } else if (action == CommandType.SHOOT) {
            switch (type) {
                case ARCHER: return units.getArcher().getShootCooldownSeconds();
                case CANNON: return units.getCannon().getShootCooldownSeconds();
                case VEHICLE: return Integer.MAX_VALUE;
                default: throw new IllegalArgumentException("Unknown unit type for shoot: " + type);
            }
        }
        return 0;
    }
}
