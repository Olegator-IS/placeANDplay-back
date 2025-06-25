package com.is.events.api;

import com.is.events.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final WebSocketService webSocketService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

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

    // Периодически отправлять статус всем подписчикам /topic/health
    @Scheduled(fixedRate = 10000) // каждые 10 секунд
    public void sendHealthStatus() {
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", "UP");
        healthStatus.put("timestamp", LocalDateTime.now());
        healthStatus.put("websocket", "UP");
        healthStatus.put("service", "ws-service");
        healthStatus.put("version", "1.0");
        messagingTemplate.convertAndSend("/topic/health", healthStatus);
    }

    // Клиент может отправить ping на /app/health.ping и получить pong
    @MessageMapping("/health.ping")
    public void healthPing() {
        Map<String, Object> pong = new HashMap<>();
        pong.put("status", "UP");
        pong.put("timestamp", LocalDateTime.now());
        pong.put("websocket", "UP");
        pong.put("service", "ws-service");
        pong.put("version", "1.0");
        messagingTemplate.convertAndSend("/topic/health", pong);
    }
} 