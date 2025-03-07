package com.is.auth.service;

import com.is.auth.model.logger.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RequestLogger {
    private final Logger logger;

    public void logRequest(HttpStatus status, 
                         long currentTime,
                         String method,
                         String url,
                         String requestId,
                         String clientIp,
                         long executionTime,
                         Object request,
                         Object response) {
        logger.logRequestDetails(
            status,
            currentTime,
            method,
            url,
            requestId,
            clientIp,
            executionTime,
            request,
            response
        );
    }
} 