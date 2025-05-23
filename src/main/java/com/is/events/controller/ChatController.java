package com.is.events.controller;

import com.is.events.model.chat.ChatMessageDTO;
import com.is.events.model.chat.ChatMessageRequest;
import com.is.events.model.chat.ChatMessagesRequest;
import com.is.events.model.chat.EventMessage;
import com.is.events.service.ChatService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
        return chatService.sendMessage(eventId, userId, request, accessToken, refreshToken, lang);
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
        log.info("Received request for messages - eventId: {}, request: {}, accessToken: {}, refreshToken: {}, lang: {}", 
            eventId, request, accessToken, refreshToken, lang);
        
        Page<ChatMessageDTO> messages = chatService.getEventMessages(eventId, request, accessToken, refreshToken, lang);
        log.info("Retrieved {} messages for event {}", messages.getTotalElements(), eventId);
        
        ResponseEntity<Page<ChatMessageDTO>> response = ResponseEntity.ok(messages);
        log.info("Sending response with status: {}", response.getStatusCode());
        return response;
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

    @PutMapping("/messages/{messageId}")
    @ApiOperation(value = "Edit a message", notes = "Edits an existing message")
    public ResponseEntity<ChatMessageDTO> editMessage(
            @PathVariable Long messageId,
            @RequestBody String newContent,
            @RequestHeader("userId") Long userId) {
        log.info("Editing message {} by user {}", messageId, userId);
        return ResponseEntity.ok(chatService.editMessage(messageId, userId, newContent));
    }

    @DeleteMapping("/messages/{messageId}")
    @ApiOperation(value = "Delete a message", notes = "Marks a message as deleted")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId,
            @RequestHeader("userId") Long userId) {
        log.info("Deleting message {} by user {}", messageId, userId);
        chatService.deleteMessage(messageId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/messages/{messageId}/read")
    @ApiOperation(value = "Mark message as read", notes = "Marks a message as read by the current user")
    public ResponseEntity<Void> markMessageAsRead(
            @PathVariable Long messageId,
            @RequestHeader("userId") Long userId) {
        log.info("Marking message {} as read by user {}", messageId, userId);
        chatService.markMessageAsRead(messageId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{eventId}/search")
    @ApiOperation(value = "Search messages", notes = "Searches for messages containing the specified query")
    public ResponseEntity<Page<ChatMessageDTO>> searchMessages(
            @PathVariable Long eventId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Searching messages in event {} with query: {}", eventId, query);
        return ResponseEntity.ok(chatService.searchMessages(eventId, query, PageRequest.of(page, size)));
    }

    @PostMapping("/messages/{messageId}/reactions")
    @ApiOperation(value = "Add reaction", notes = "Adds a reaction to a message")
    public ResponseEntity<Void> addReaction(
            @PathVariable Long messageId,
            @RequestHeader("userId") Long userId,
            @RequestParam String reactionType) {
        log.info("Adding reaction {} to message {} by user {}", reactionType, messageId, userId);
        chatService.addReaction(messageId, userId, reactionType);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/messages/{messageId}/reactions")
    public ResponseEntity<Void> removeReaction(
            @PathVariable Long messageId,
            @RequestHeader("userId") Long userId,
            @RequestParam String reactionType) {
        chatService.removeReaction(messageId, userId, reactionType);
        return ResponseEntity.ok().build();
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

    @GetMapping("/debug/raw-messages")
    public List<EventMessage> getRawMessages() {
        List<EventMessage> messages = chatService.getRawMessagesDebug(581L);
        System.out.println("RAW SIZE: " + messages.size());
        for (EventMessage m : messages) {
            System.out.println("MSG: " + m.getMessageId() + " | " + m.getType() + " | " + m.getContent());
        }
        return messages;
    }
} 