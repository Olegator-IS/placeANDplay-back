package com.is.events.scheduler;

import com.is.events.model.Event;
import com.is.events.model.enums.EventStatus;
import com.is.events.repository.EventsRepository;
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

    @Scheduled(fixedRate = 600000) // 600000 ms = 10 минут
    @Transactional
    public void checkEvents() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Starting events check at {}", now);

        try {
            // Проверяем все OPEN события
            List<Event> openEvents = eventsRepository.findByStatus(EventStatus.OPEN.name());
            
            if (!openEvents.isEmpty()) {
                log.info("Found {} open events to check", openEvents.size());
                
                openEvents.forEach(event -> {
                    try {
                        // Если время события прошло, переводим в EXPIRED
                        if (event.getDateTime().isBefore(now)) {
                            event.setStatus(EventStatus.EXPIRED.name());
                            eventsRepository.save(event);
                            log.info("Updated event {} status to EXPIRED. Event time was: {}", 
                                event.getEventId(), event.getDateTime());
                        } else {
                            log.debug("Event {} is still valid. Current time: {}, Event time: {}", 
                                event.getEventId(), now, event.getDateTime());
                        }
                    } catch (Exception e) {
                        log.error("Error checking event {}", event.getEventId(), e);
                    }
                });
            } else {
                log.info("No open events found to check");
            }
        } catch (Exception e) {
            log.error("Error during events check", e);
        }
    }
} 