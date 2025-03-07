package com.is.events.exception;

import org.springframework.http.HttpStatus;

public class EventNotFoundException extends EventException {
    public EventNotFoundException(String lang) {
        super("event_not_found", "Event not found in the system", HttpStatus.NOT_FOUND);
    }
} 