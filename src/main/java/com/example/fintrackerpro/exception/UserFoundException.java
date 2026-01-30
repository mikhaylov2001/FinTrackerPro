package com.example.fintrackerpro.exception;

public class UserFoundException extends RuntimeException {
    public UserFoundException(String message) {
        super(message);
    }

    public UserFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
