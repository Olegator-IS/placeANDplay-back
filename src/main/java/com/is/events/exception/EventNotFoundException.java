package com.is.events.exception;

public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException(String lang) {
        super("Event not found");
    }
} 