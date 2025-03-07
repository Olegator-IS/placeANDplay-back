package com.is.events.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class EventException extends RuntimeException {
    private final String messageKey;
    private final HttpStatus status;

    public EventException(String messageKey, String message, HttpStatus status) {
        super(message);
        this.messageKey = messageKey;
        this.status = status;
    }
} 