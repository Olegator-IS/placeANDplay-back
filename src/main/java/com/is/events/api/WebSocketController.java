package com.is.events.api;

import com.is.events.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final WebSocketService webSocketService;

    @SubscribeMapping("/topic/place/{placeId}")
    public void subscribeToPlace(String placeId) {
        log.info("Client subscribed to place: {}", placeId);
    }

    @MessageMapping("/place.connect")
    public void connect(@Payload String message) {
        log.info("Client connected with message: {}", message);
    }

    @MessageMapping("/place.disconnect")
    public void disconnect(@Payload String message) {
        log.info("Client disconnected with message: {}", message);
    }
} 