package com.is.events.exception;

import org.springframework.http.HttpStatus;

public class EventValidationException extends EventException {
    public EventValidationException(String messageKey, String message) {
        super(messageKey, message, HttpStatus.BAD_REQUEST);
    }
} 