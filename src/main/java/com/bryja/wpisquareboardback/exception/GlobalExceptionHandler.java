package com.bryja.wpisquareboardback.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.context.support.DefaultMessageSourceResolvable;


import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleGameNotFoundException(GameNotFoundException ex) {
        log.warn("Game not found: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UnitNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUnitNotFoundException(UnitNotFoundException ex) {
        log.warn("Unit not found: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({InvalidCommandException.class, OutOfBoundsException.class, PositionOccupiedException.class})
    public ResponseEntity<ErrorResponse> handleBadRequestExceptions(RuntimeException ex) {
        log.warn("Invalid request/command: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ActionNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleActionNotAllowedException(ActionNotAllowedException ex) {
        log.warn("Action forbidden: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN);
    }


    @ExceptionHandler(CooldownException.class)
    public ResponseEntity<ErrorResponse> handleCooldownException(CooldownException ex) {
        log.warn("Cooldown restriction: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.TOO_MANY_REQUESTS); // 429
    }

    @ExceptionHandler(ConcurrencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleConcurrencyConflictException(ConcurrencyConflictException ex) {
        log.warn("Concurrency conflict: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList());
        String errorMessage = "Validation failed: " + String.join("; ", errors);
        log.warn(errorMessage);
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation Error", errorMessage);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred.");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class) // Catch this exception
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(OptimisticLockingFailureException ex) {
        String message = "Action failed due to a conflict with another simultaneous command. Please refresh and try again.";
        log.warn("OptimisticLockingFailureException: {} - Cause: {}", message, ex.getMessage()); // Log the warning
        return buildErrorResponse(ex, HttpStatus.CONFLICT, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        String specificMessage = "Invalid request body format or value."; // Default message
        HttpStatus status = HttpStatus.BAD_REQUEST;

        Throwable cause = ex.getRootCause();

        if (cause instanceof InvalidFormatException) {
            InvalidFormatException ife = (InvalidFormatException) cause;
            // Check if the target type was an enum
            if (ife.getTargetType() != null && ife.getTargetType().isEnum()) {
                String enumValues = Arrays.stream(ife.getTargetType().getEnumConstants())
                        .map(Object::toString)
                        .collect(Collectors.joining("', '", "'", "'")); // Format as 'VALUE1', 'VALUE2'

                String fieldPath = ife.getPath().stream()
                        .map(JsonMappingException.Reference::getFieldName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining("."));

                specificMessage = String.format("Invalid value '%s' for field '%s'. Must be one of: [%s]",
                        ife.getValue(),
                        fieldPath.isEmpty() ? "enum property" : fieldPath, // Use field path if available
                        enumValues);
            }
        } else if (cause instanceof JsonMappingException) {
            JsonMappingException jme = (JsonMappingException) cause;
            String fieldPath = jme.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("."));
            specificMessage = String.format("Invalid format or type for field '%s'. Check request body structure.",
                    fieldPath.isEmpty() ? "property" : fieldPath);
        }

        log.warn("Bad Request - Message Not Readable. Root cause: {} - Field: {}",
                cause != null ? cause.getClass().getSimpleName() : "N/A",
                specificMessage); // Log useful info
        return buildErrorResponse(ex, status, specificMessage);
    }


    private ResponseEntity<ErrorResponse> buildErrorResponse(Exception ex, HttpStatus status) {
        return buildErrorResponse(ex, status, ex.getMessage());
    }


    private ResponseEntity<ErrorResponse> buildErrorResponse(Exception ex, HttpStatus status, String message) {
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    private static class ErrorResponse {
        private final int status;
        private final String error;
        private final String message;


        public ErrorResponse(int status, String error, String message) {
            this.status = status;
            this.error = error;
            this.message = message;
        }

        public int getStatus() { return status; }
        public String getError() { return error; }
        public String getMessage() { return message; }
    }
}
