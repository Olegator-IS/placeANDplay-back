package com.is.events.scheduler;

import com.is.auth.service.EmailService;
import com.is.auth.service.PushNotificationService;
import com.is.auth.repository.UserRepository;
import com.is.auth.model.user.User;
import com.is.auth.model.user.UserAdditionalInfo;
import com.is.auth.repository.UserAdditionalInfoRepository;
import com.is.events.model.Event;
import com.is.events.model.enums.EventMessageType;
import com.is.events.model.enums.EventStatus;
import com.is.events.repository.EventsRepository;
import com.is.events.service.WebSocketService;
import com.is.events.service.EventMessageService;
import com.is.events.service.LocalizationService;
import com.is.places.model.Place;
import com.is.places.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventScheduler {

    private final EventsRepository eventsRepository;
    private final WebSocketService webSocketService;
    private final EventMessageService eventMessageService;
    private final PlaceRepository placeRepository;
    private final EmailService emailService;
    private final PushNotificationService pushNotificationService;
    private final LocalizationService localizationService;
    private final UserRepository userRepository;
    private final UserAdditionalInfoRepository userAdditionalInfoRepository;

    // Кэш для отслеживания уже отправленных уведомлений
    private final Set<Long> sentHourNotifications = ConcurrentHashMap.newKeySet();

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

    // Уведомления за час до события
    @Scheduled(fixedRate = 300000) // 300000 ms = 5 минут
    @Transactional
    public void sendHourBeforeNotifications() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourFromNow = now.plusHours(1);
        log.info("Checking for events starting in 1 hour at {}", oneHourFromNow);

        try {
            // Находим события со статусом CONFIRMED, которые начнутся через час
            List<Event> eventsStartingInHour = eventsRepository.findConfirmedEventsStartingInHour(now, oneHourFromNow);
            
            eventsStartingInHour.forEach(event -> {
                try {
                    // Проверяем, не отправляли ли мы уже уведомление для этого события
                    if (!sentHourNotifications.contains(event.getEventId())) {
                        log.info("Sending hour-before notification for event {}", event.getEventId());
                        
                        // Отправляем уведомления всем участникам и организатору
                        sendHourBeforeNotificationToEvent(event);
                        
                        // Добавляем событие в кэш отправленных уведомлений
                        sentHourNotifications.add(event.getEventId());
                    } else {
                        log.debug("Hour-before notification already sent for event {}", event.getEventId());
                    }
                    
                } catch (Exception e) {
                    log.error("Error sending hour-before notification for event {}: {}", event.getEventId(), e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error in hour-before notification check: {}", e.getMessage());
        }
    }

    private void sendHourBeforeNotificationToEvent(Event event) {
        try {
            String eventName = event.getSportEvent() != null ? event.getSportEvent().getSportName() : "Событие";

            // Уведомление организатору
            if (event.getOrganizerEvent() != null) {
                sendPersonalizedHourBeforeNotification(
                    event.getOrganizerEvent().getOrganizerId(),
                    eventName,
                    "ru" // По умолчанию русский, можно добавить получение языка из профиля
                );
            }

            // Уведомления участникам
            if (event.getCurrentParticipants() != null && event.getCurrentParticipants().getParticipants() != null) {
                event.getCurrentParticipants().getParticipants().forEach(participant -> {
                    sendPersonalizedHourBeforeNotification(
                        participant.getParticipantId(),
                        eventName,
                        "ru" // По умолчанию русский, можно добавить получение языка из профиля
                    );
                });
            }
        } catch (Exception e) {
            log.error("Error sending hour-before notification for event {}: {}", event.getEventId(), e.getMessage());
        }
    }

    private void sendPersonalizedHourBeforeNotification(Long userId, String eventName, String defaultLanguage) {
        try {
            // Получаем язык пользователя из профиля
            String userLanguage = getUserLanguage(userId, defaultLanguage);
            log.debug("User {} language: {}", userId, userLanguage);
            
            String title = localizationService.getMessage("hour_before.title", userLanguage);
            String message = localizationService.getMessage("hour_before.message", userLanguage)
                    .replace("{0}", eventName);
            String notificationType = localizationService.getMessage("hour_before.notification_type", userLanguage);

            log.debug("Notification for user {}: title='{}', message='{}', type='{}'", 
                     userId, title, message, notificationType);

            pushNotificationService.sendSimpleNotification(userId, title, message, notificationType);
            
            log.debug("Sent hour-before notification to user {} for event: {} in language: {}", 
                     userId, eventName, userLanguage);
        } catch (Exception e) {
            log.error("Error sending personalized hour-before notification to user {}: {}", userId, e.getMessage());
        }
    }

    private String getUserLanguage(Long userId, String defaultLanguage) {
        try {
            // Получаем язык пользователя из профиля
            Optional<UserAdditionalInfo> userInfo = userAdditionalInfoRepository.findById(userId);
            if (userInfo.isPresent() && userInfo.get().getLanguage() != null) {
                return userInfo.get().getLanguage();
            }
            return defaultLanguage;
        } catch (Exception e) {
            log.warn("Could not get user language for user {}, using default: {}", userId, defaultLanguage);
            return defaultLanguage;
        }
    }

    // Очистка кэша отправленных уведомлений каждые 24 часа
    @Scheduled(cron = "0 0 0 * * ?") // Каждый день в полночь
    public void clearSentNotificationsCache() {
        log.info("Clearing sent notifications cache");
        sentHourNotifications.clear();
    }

    // Тестовый метод для проверки локализации (выполняется при запуске)
    @Scheduled(initialDelay = 10000, fixedRate = Long.MAX_VALUE) // Выполняется через 10 секунд после запуска
    public void testLocalization() {
        log.info("Testing localization...");
        try {
            String title = localizationService.getMessage("hour_before.title", "ru");
            String message = localizationService.getMessage("hour_before.message", "ru");
            String notificationType = localizationService.getMessage("hour_before.notification_type", "ru");
            
            log.info("Localization test - title: '{}', message: '{}', type: '{}'", title, message, notificationType);
        } catch (Exception e) {
            log.error("Localization test failed: {}", e.getMessage());
        }
    }
} 