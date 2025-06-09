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
                    if (event.getDateTime().isBefore(now)) {
                        if (event.getStatus() == EventStatus.PENDING_APPROVAL ||
                            event.getStatus() == EventStatus.CHANGES_REQUESTED) {
                            event.forceExpire(); // Новый метод для принудительного перевода в EXPIRED
                            Event savedEvent = eventsRepository.save(event);
                            
                            // Отправляем системное сообщение о просрочке события
                            eventMessageService.sendEventMessage(savedEvent, EventMessageType.EVENT_EXPIRED, null, "ru");
                            
                            // Отправляем уведомление через WebSocket
                            webSocketService.notifyEventUpdate(event.getPlaceId());
                            webSocketService.sendEventUpdate(savedEvent);

                            Place getPlace = placeRepository.findPlaceByPlaceId(event.getPlaceId());


                            emailService.sendEventStatusChangeNotification(event,"ru",getPlace.getName(),getPlace.getPhone());
                            
                            log.info("Event {} expired due to time", event.getEventId());
                        }
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