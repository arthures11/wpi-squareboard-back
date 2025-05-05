package com.bryja.wpisquareboardback.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class PositionOccupiedException extends RuntimeException {
    public PositionOccupiedException(String message) {
        super(message);
    }
}
