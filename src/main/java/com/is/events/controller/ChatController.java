package com.is.events.controller;

import com.is.events.model.chat.ChatMessageDTO;
import com.is.events.model.chat.ChatMessageRequest;
import com.is.events.model.chat.ChatMessagesRequest;
import com.is.events.service.ChatService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
@Api(tags = "Chat API", description = "Endpoints for chat functionality")
@Slf4j
public class ChatController {
    private final ChatService chatService;

    @MessageMapping("/chat.send/{eventId}")
    @SendTo("/topic/chat/{eventId}")
    public ChatMessageDTO sendMessage(@Payload ChatMessageRequest request,
                                    @DestinationVariable Long eventId,
                                    @Header("userId") Long userId,
                                    @Header("accessToken") String accessToken,
                                    @Header("refreshToken") String refreshToken,
                                    @Header("language") String lang) {
        log.info("Received chat message for event {}: {}", eventId, request);
        return chatService.sendMessage(eventId, userId, request.getContent(), accessToken, refreshToken, lang);
    }

    @GetMapping("/{eventId}/messages")
    @ApiOperation(value = "Get messages for an event", notes = "Retrieves paginated messages for a specific event")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Successfully retrieved messages"),
        @ApiResponse(code = 401, message = "Unauthorized - Invalid token"),
        @ApiResponse(code = 403, message = "Forbidden - Not allowed to access this event"),
        @ApiResponse(code = 404, message = "Event not found")
    })
    public ResponseEntity<Page<ChatMessageDTO>> getEventMessages(
            @PathVariable Long eventId,
            @ModelAttribute ChatMessagesRequest request,
            @RequestHeader("accessToken") String accessToken,
            @RequestHeader("refreshToken") String refreshToken,
            @RequestHeader("language") String lang) {
        log.info("Fetching messages for event {} with params: {}", eventId, request);
        return ResponseEntity.ok(chatService.getEventMessages(eventId, request, accessToken, refreshToken, lang));
    }

    @GetMapping("/{eventId}/messages/latest")
    @ApiOperation(value = "Get latest messages for an event", notes = "Retrieves the most recent messages for a specific event")
    public ResponseEntity<List<ChatMessageDTO>> getLatestEventMessages(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestHeader("accessToken") String accessToken,
            @RequestHeader("refreshToken") String refreshToken,
            @RequestHeader("language") String lang) {
        log.info("Fetching latest {} messages for event {}", limit, eventId);
        return ResponseEntity.ok(chatService.getLatestEventMessages(eventId, limit, accessToken, refreshToken, lang));
    }

    @SubscribeMapping("/chat/{eventId}")
    public List<ChatMessageDTO> subscribeToChat(
            @DestinationVariable Long eventId,
            @Header("accessToken") String accessToken,
            @Header("refreshToken") String refreshToken,
            @Header("language") String lang) {
        log.info("New subscription to chat for event {}", eventId);
        return chatService.getLatestEventMessages(eventId, 50, accessToken, refreshToken, lang);
    }
} 