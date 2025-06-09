package com.is.events.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void notifyEventUpdate(Long placeId) {
        log.info("Sending WebSocket notification for place {}", placeId);
        messagingTemplate.convertAndSend("/topic/place/" + placeId, "update");
    }

    public void sendEventUpdate(Object payload) {
        log.debug("Sending event update: {}", payload);
        messagingTemplate.convertAndSend("/topic/events", payload);
    }

    public void sendPlaceUpdate(Object payload) {
        log.debug("Sending place update: {}", payload);
        messagingTemplate.convertAndSend("/topic/places", payload);
    }
}

record EventUpdateMessage(Long placeId) {
    // Можно добавить дополнительные поля, если нужно передавать больше информации
}