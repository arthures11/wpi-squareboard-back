package com.bryja.wpisquareboardback.util;

import com.bryja.wpisquareboardback.model.Position;
import org.springframework.stereotype.Component;

@Component
public class BoardUtils {

    public boolean isWithinBounds(Position pos, int width, int height) {
        return pos.getX() >= 0 && pos.getX() < width &&
                pos.getY() >= 0 && pos.getY() < height;
    }

    public int calculateDistanceX(Position p1, Position p2) {
        return Math.abs(p1.getX() - p2.getX());
    }
    public int calculateDistanceY(Position p1, Position p2) {
        return Math.abs(p1.getY() - p2.getY());
    }
    public int calculateManhattanDistance(Position p1, Position p2) {
        return calculateDistanceX(p1,p2) + calculateDistanceY(p1,p2);
    }
}
