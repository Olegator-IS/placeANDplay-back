package com.is.auth.api;

import com.is.auth.model.UserFcmToken;
import com.is.auth.repository.UserFcmTokenRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fcm")
@Api(tags = "FCM Token Management", description = "Endpoints for managing Firebase Cloud Messaging tokens")
public class FcmTokenController {

    private final UserFcmTokenRepository userFcmTokenRepository;

    @PostMapping("/register")
    @ApiOperation(value = "Register a new FCM token for a user", notes = "Registers a new FCM token for push notifications")
    public ResponseEntity<?> registerToken(
            @ApiParam(value = "User ID", required = true) @RequestHeader("userId") Long userId,
            @ApiParam(value = "FCM token", required = true) @RequestParam String token,
            @ApiParam(value = "Device type (android/ios)", required = true) @RequestParam String deviceType) {
        try {
            // Проверяем, существует ли уже такой токен у другого пользователя
            Optional<UserFcmToken> existingToken = userFcmTokenRepository.findByToken(token);
            if (existingToken.isPresent() && !existingToken.get().getUserId().equals(userId)) {
                log.warn("Token {} is already registered for another user", token);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "status", "token_conflict",
                    "message", "Token is already registered for another user",
                    "code", "TOKEN_CONFLICT"
                ));
            }

            // Ищем существующий токен для пользователя
            List<UserFcmToken> userTokens = userFcmTokenRepository.findByUserId(userId);
            
            if (!userTokens.isEmpty()) {
                // Если у пользователя уже есть токен, обновляем его
                UserFcmToken userToken = userTokens.get(0);
                userToken.setToken(token);
                userToken.setDeviceType(deviceType);
                userFcmTokenRepository.save(userToken);
                log.info("Successfully updated FCM token for user {}", userId);
            } else {
                // Если у пользователя нет токена, создаем новый
                UserFcmToken fcmToken = UserFcmToken.builder()
                        .userId(userId)
                        .token(token)
                        .deviceType(deviceType)
                        .build();
                userFcmTokenRepository.save(fcmToken);
                log.info("Successfully registered new FCM token for user {}", userId);
            }
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Token registered successfully"
            ));
        } catch (Exception e) {
            log.error("Error registering FCM token for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Failed to register token: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/unregister")
    @ApiOperation(value = "Unregister an FCM token", notes = "Removes a registered FCM token")
    public ResponseEntity<?> unregisterToken(
            @ApiParam(value = "FCM token to unregister", required = true) @RequestParam String token) {
        try {
            userFcmTokenRepository.deleteByToken(token);
            log.info("Successfully unregistered FCM token");
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Token unregistered successfully"
            ));
        } catch (Exception e) {
            log.error("Error unregistering FCM token: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Failed to unregister token: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/tokens")
    @ApiOperation(value = "Get all FCM tokens for a user", notes = "Retrieves all registered FCM tokens for a specific user")
    public ResponseEntity<?> getUserTokens(
            @ApiParam(value = "User ID", required = true) @RequestHeader("userId") Long userId) {
        try {
            List<UserFcmToken> tokens = userFcmTokenRepository.findByUserId(userId);
            log.info("Retrieved {} FCM tokens for user {}", tokens.size(), userId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "tokens", tokens
            ));
        } catch (Exception e) {
            log.error("Error retrieving FCM tokens for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Failed to retrieve tokens: " + e.getMessage()
            ));
        }
    }
} 