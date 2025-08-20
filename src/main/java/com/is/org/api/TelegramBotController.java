package com.is.org.api;

import com.is.org.model.OrganizationLoginRequest;
import com.is.org.model.OrganizationTelegramChatRequest;
import com.is.org.service.OrganizationService;
import com.is.org.service.OrganizationTelegramChatService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "Telegram Bot APIs", description = "APIs for Telegram bot integration")
@RestController
@RequestMapping("/api/bot/organizations")
@RequiredArgsConstructor
@Slf4j
public class TelegramBotController {

    private final OrganizationService organizationService;
    private final OrganizationTelegramChatService telegramChatService;

    @PostMapping("/login")
    @ApiOperation(value = "Bot login organization", notes = "Authenticate organization for telegram bot using email and password")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully logged in"),
            @ApiResponse(code = 401, message = "Invalid email or password")
    })
    public ResponseEntity<?> botLogin(@RequestBody OrganizationLoginRequest loginRequest) throws Exception {
        log.info("Received bot login request for email: {}", loginRequest.getEmail());
        return organizationService.botLogin(loginRequest);
    }

    @PostMapping("/orgInfo")
    @ApiOperation(value = "Get organization info for bot", notes = "Retrieve organization information using access token in request body")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully retrieved organization info"),
            @ApiResponse(code = 401, message = "Invalid access token"),
            @ApiResponse(code = 404, message = "Organization not found")
    })
    public ResponseEntity<?> botOrgInfo(@RequestBody Map<String, String> request) throws Exception {
        String accessToken = request.get("accessToken");
        String refreshToken = request.get("refreshToken");
        
        if (accessToken == null || accessToken.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Access token is required");
        }
        
        log.info("Received bot request for organization info");
        return organizationService.orgInfo(accessToken, refreshToken);
    }

    @PostMapping("/telegram-chat")
    @ApiOperation(value = "Save organization telegram chat", notes = "Save telegram chat ID for organization")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Telegram chat saved successfully"),
        @ApiResponse(code = 400, message = "Invalid input data"),
        @ApiResponse(code = 409, message = "Chat ID already exists for this organization")
    })
    public ResponseEntity<?> saveTelegramChat(@RequestBody OrganizationTelegramChatRequest request) {
        log.info("Received telegram chat save request for organization: {}", request.getOrgId());
        try {
            var result = telegramChatService.saveTelegramChat(request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @GetMapping("/telegram-chats/{orgId}")
    @ApiOperation(value = "Get organization telegram chats", notes = "Get all active telegram chat IDs for organization")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Telegram chats retrieved successfully"),
        @ApiResponse(code = 404, message = "Organization not found")
    })
    public ResponseEntity<?> getTelegramChats(@PathVariable Long orgId) {
        log.info("Received request for telegram chats of organization: {}", orgId);
        var chats = telegramChatService.findByOrgId(orgId);
        return ResponseEntity.ok(chats);
    }

    @DeleteMapping("/telegram-chat/{orgId}/{chatId}")
    @ApiOperation(value = "Deactivate organization telegram chat", notes = "Deactivate specific telegram chat for organization")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Telegram chat deactivated successfully"),
        @ApiResponse(code = 404, message = "Chat not found")
    })
    public ResponseEntity<?> deactivateTelegramChat(
            @PathVariable Long orgId,
            @PathVariable String chatId) {
        log.info("Received request to deactivate telegram chat {} for organization: {}", chatId, orgId);
        boolean result = telegramChatService.deactivateChatByOrgIdAndChatId(orgId, chatId);
        if (result) {
            return ResponseEntity.ok("Chat deactivated successfully");
        } else {
            return ResponseEntity.status(404).body("Chat not found");
        }
    }
}
