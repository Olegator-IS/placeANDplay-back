package com.is.org.api;

import com.is.org.service.TelegramNotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "Telegram Notification APIs", description = "APIs for managing Telegram notifications")
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/notifications/telegram")
@RequiredArgsConstructor
@Slf4j
public class TelegramNotificationController {

    private final TelegramNotificationService telegramNotificationService;

    @PostMapping("/send/organization/{orgId}")
    @ApiOperation(value = "Send notification to organization", notes = "Send notification to all telegram chats of specific organization")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Notification sent successfully"),
        @ApiResponse(code = 404, message = "Organization not found"),
        @ApiResponse(code = 500, message = "Failed to send notification")
    })
    public ResponseEntity<?> sendNotificationToOrganization(
            @PathVariable Long orgId,
            @RequestBody Map<String, String> request) {
        
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message is required");
        }

        log.info("Sending notification to organization: {}", orgId);
        try {
            telegramNotificationService.sendNotificationToOrganization(orgId, message);
            return ResponseEntity.ok("Notification sent successfully");
        } catch (Exception e) {
            log.error("Failed to send notification to organization {}: {}", orgId, e.getMessage());
            return ResponseEntity.status(500).body("Failed to send notification: " + e.getMessage());
        }
    }

    @PostMapping("/send/chat/{chatId}")
    @ApiOperation(value = "Send notification to specific chat", notes = "Send notification to specific telegram chat ID")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Notification sent successfully"),
        @ApiResponse(code = 500, message = "Failed to send notification")
    })
    public ResponseEntity<?> sendNotificationToChat(
            @PathVariable String chatId,
            @RequestBody Map<String, String> request) {
        
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message is required");
        }

        log.info("Sending notification to chat: {}", chatId);
        try {
            telegramNotificationService.sendNotificationToChat(chatId, message);
            return ResponseEntity.ok("Notification sent successfully");
        } catch (Exception e) {
            log.error("Failed to send notification to chat {}: {}", chatId, e.getMessage());
            return ResponseEntity.status(500).body("Failed to send notification: " + e.getMessage());
        }
    }

    @PostMapping("/broadcast")
    @ApiOperation(value = "Broadcast notification to all organizations", notes = "Send notification to all active telegram chats")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Broadcast sent successfully"),
        @ApiResponse(code = 500, message = "Failed to send broadcast")
    })
    public ResponseEntity<?> broadcastNotification(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message is required");
        }

        log.info("Broadcasting notification to all organizations");
        try {
            telegramNotificationService.sendNotificationToAllOrganizations(message);
            return ResponseEntity.ok("Broadcast sent successfully");
        } catch (Exception e) {
            log.error("Failed to send broadcast: {}", e.getMessage());
            return ResponseEntity.status(500).body("Failed to send broadcast: " + e.getMessage());
        }
    }

    @GetMapping("/test-connection")
    @ApiOperation(value = "Test bot connection", notes = "Test if the telegram bot is accessible")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Connection test completed"),
        @ApiResponse(code = 500, message = "Connection test failed")
    })
    public ResponseEntity<?> testBotConnection(@RequestParam(required = false) String botToken) {
        log.info("Testing bot connection");
        try {
            boolean isConnected = telegramNotificationService.testBotConnection(botToken);
            if (isConnected) {
                return ResponseEntity.ok("Bot connection successful");
            } else {
                return ResponseEntity.status(500).body("Bot connection failed");
            }
        } catch (Exception e) {
            log.error("Bot connection test failed: {}", e.getMessage());
            return ResponseEntity.status(500).body("Connection test failed: " + e.getMessage());
        }
    }
}
