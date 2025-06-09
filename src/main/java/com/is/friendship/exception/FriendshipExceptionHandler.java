package com.is.friendship.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice(basePackages = {"com.is.friendship.controller", "com.is.friendship.service"})
public class FriendshipExceptionHandler {

    @ExceptionHandler(FriendshipException.class)
    public ResponseEntity<Map<String, Object>> handleFriendshipException(FriendshipException ex) {
        log.error("Friendship error occurred: {}", ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", ex.getErrorType().getStatus());
        body.put("error", HttpStatus.valueOf(ex.getErrorType().getStatus()).getReasonPhrase());
        body.put("message", ex.getMessage());
        
        return new ResponseEntity<>(body, HttpStatus.valueOf(ex.getErrorType().getStatus()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Unexpected error occurred: ", ex);
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "An unexpected error occurred: " + ex.getMessage());
        
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
} 