package com.example.fintrackerpro.exception;

import org.springframework.http.HttpStatus;

/**
 * Выбрасывается когда операция невалидна/невозможна
 */
public class InvalidOperationException extends AppException {

    public InvalidOperationException(String message) {
        super(
                message,
                "INVALID_OPERATION",
                HttpStatus.BAD_REQUEST.value()
        );
    }
}
