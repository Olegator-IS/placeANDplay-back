package com.is.events.scheduler;

import com.is.auth.service.EmailService;
import com.is.events.model.Event;
import com.is.events.model.enums.EventMessageType;
import com.is.events.model.enums.EventStatus;
import com.is.events.repository.EventsRepository;
import com.is.events.service.WebSocketService;
import com.is.events.service.EventMessageService;
import com.is.places.model.Place;
import com.is.places.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventScheduler {

    private final EventsRepository eventsRepository;
    private final WebSocketService webSocketService;
    private final EventMessageService eventMessageService;
    private final PlaceRepository placeRepository;
    private final EmailService emailService;

    @Scheduled(fixedRate = 600000) // 600000 ms = 10 минут
    @Transactional
    public void checkEvents() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Starting scheduled event check at {}", now);

        try {
            List<Event> events = eventsRepository.findAll();
            
            events.forEach(event -> {
                try {
                    // Перевод в IN_PROGRESS только если статус CONFIRMED и время наступило
                    if (event.getStatus() == EventStatus.CONFIRMED && !event.getDateTime().isAfter(now)) {
                        event.setStatus(EventStatus.IN_PROGRESS);
                        Event savedEvent = eventsRepository.save(event);
                        eventMessageService.sendEventMessage(savedEvent, EventMessageType.EVENT_STARTED, null, "ru");
                        webSocketService.notifyEventUpdate(event.getPlaceId());
                        webSocketService.sendEventUpdate(savedEvent);
                        log.info("Event {} moved to IN_PROGRESS", event.getEventId());
                    }
                    // Перевод в EXPIRED только если статус OPEN, PENDING_APPROVAL, CHANGES_REQUESTED и время прошло
                    if ((event.getStatus() == EventStatus.OPEN ||
                         event.getStatus() == EventStatus.PENDING_APPROVAL ||
                         event.getStatus() == EventStatus.CHANGES_REQUESTED)
                        && event.getDateTime().isBefore(now)) {
                        event.forceExpire();
                        Event savedEvent = eventsRepository.save(event);
                        eventMessageService.sendEventMessage(savedEvent, EventMessageType.EVENT_EXPIRED, null, "ru");
                        webSocketService.notifyEventUpdate(event.getPlaceId());
                        webSocketService.sendEventUpdate(savedEvent);
                        Place getPlace = placeRepository.findPlaceByPlaceId(event.getPlaceId());
                        emailService.sendEventStatusChangeNotification(event,"ru",getPlace.getName(),getPlace.getPhone());
                        log.info("Event {} expired due to time", event.getEventId());
                    }
                } catch (Exception e) {
                    log.error("Error checking event {}: {}", event.getEventId(), e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error in scheduled event check: {}", e.getMessage());
        }
    }
} 