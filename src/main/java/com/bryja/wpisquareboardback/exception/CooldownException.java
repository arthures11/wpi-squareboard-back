package com.bryja.wpisquareboardback.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class CooldownException extends RuntimeException {
    public CooldownException(String message) {
        super(message);
    }
}
