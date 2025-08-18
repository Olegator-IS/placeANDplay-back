package com.is.auth.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String phoneNumber) {
        super("User with this phone number " + phoneNumber + " already exists");
    }
} 