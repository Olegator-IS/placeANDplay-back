package com.is.events.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyEventUpdate(Long placeId) {
        try {
            String destination = String.format("/topic/events/%d", placeId);
            messagingTemplate.convertAndSend(destination, new EventUpdateMessage(placeId));
            log.debug("Sent WebSocket notification to {}", destination);
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification for placeId {}: {}", placeId, e.getMessage());
            throw e;
        }
    }
}

record EventUpdateMessage(Long placeId) {
    // Можно добавить дополнительные поля, если нужно передавать больше информации
}