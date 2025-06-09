package com.is.events.exception;

public class EventValidationException extends RuntimeException {
    private final String code;

    public EventValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
} 