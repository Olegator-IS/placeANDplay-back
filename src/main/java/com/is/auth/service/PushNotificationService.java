package com.is.auth.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.is.auth.model.UserFcmToken;
import com.is.auth.repository.UserFcmTokenRepository;
import com.is.auth.repository.UserAdditionalInfoRepository;
import com.is.auth.model.user.UserAdditionalInfo;
import com.is.events.model.Event;
import com.is.events.model.EventParticipant;
import com.is.events.model.enums.EventStatus;
import com.is.places.model.Place;
import com.is.places.repository.PlaceRepository;
import com.is.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {
    
    private FirebaseMessaging firebaseMessaging;
    private final UserFcmTokenRepository userFcmTokenRepository;
    private final PlaceRepository placeRepository;
    private final UserAdditionalInfoRepository userAdditionalInfoRepository;
    @Autowired
    private NotificationService notificationService;
    
    @Value("${firebase.service-account.path:firebase-service-account.json}")
    private String serviceAccountPath;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @PostConstruct
    public void initialize() {
        try {
            log.info("Initializing Firebase Admin SDK with service account from: {}", serviceAccountPath);
            
            InputStream serviceAccount = new ClassPathResource(serviceAccountPath).getInputStream();
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();
            
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase App initialized successfully");
            } else {
                log.info("Firebase App already initialized");
            }
            
            this.firebaseMessaging = FirebaseMessaging.getInstance();
            log.info("Firebase Messaging instance created successfully");
        } catch (IOException e) {
            log.error("Failed to initialize Firebase Admin SDK. Error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Firebase Admin SDK", e);
        }
    }

    // Уведомление о присоединении к ивенту
    public void sendParticipantJoinedNotification(Event event, EventParticipant participant) {
        try {
            // Получаем токены организатора ивента
            List<UserFcmToken> organizerTokens = userFcmTokenRepository.findByUserId(event.getOrganizerEvent().getOrganizerId());
            
            for (UserFcmToken token : organizerTokens) {
                Message message = Message.builder()
                    .setToken(token.getToken())
                    .setNotification(Notification.builder()
                        .setTitle("Новый участник")
                        .setBody(String.format("%s присоединился к вашему событию \"%s\"", 
                            participant.getUser().getFirstName(), 
                            event.getSportEvent().getSportName()))
                        .build())
                    .putData("type", "PARTICIPANT_JOINED")
                    .putData("eventId", event.getEventId().toString())
                    .putData("participantId", participant.getUser().getUserId().toString())
                    .build();

                String response = firebaseMessaging.send(message);
                log.info("Successfully sent participant joined notification: {}", response);
            }
        } catch (Exception e) {
            log.error("Error sending participant joined notification", e);
        }
    }

    // Уведомление о выходе из ивента
    public void sendParticipantLeftNotification(Event event, EventParticipant participant) {
        try {
            List<UserFcmToken> organizerTokens = userFcmTokenRepository.findByUserId(event.getOrganizerEvent().getOrganizerId());
            
            for (UserFcmToken token : organizerTokens) {
                Message message = Message.builder()
                    .setToken(token.getToken())
                    .setNotification(Notification.builder()
                        .setTitle("Участник вышел")
                        .setBody(String.format("%s %s вышел из вашего события \"%s\"", 
                            participant.getUser().getFirstName(),
                            participant.getUser().getLastName(),
                            event.getSportEvent().getSportName()))
                        .build())
                    .putData("type", "PARTICIPANT_LEFT")
                    .putData("eventId", event.getEventId().toString())
                    .putData("participantId", participant.getUser().getUserId().toString())
                    .build();

                String response = firebaseMessaging.send(message);
                log.info("Successfully sent participant left notification: {}", response);
            }
        } catch (Exception e) {
            log.error("Error sending participant left notification", e);
        }
    }

    // Уведомление об изменении статуса ивента
    public void sendEventStatusChangeNotification(Event event, EventStatus newStatus) {
        try {
            Place place = placeRepository.findPlaceByPlaceId(event.getPlaceId());

            // Собираем всех участников (включая организатора)
            Set<Long> userIds = new HashSet<>();
            if (event.getCurrentParticipants() != null && event.getCurrentParticipants().getParticipants() != null) {
                event.getCurrentParticipants().getParticipants().forEach(p -> userIds.add(p.getParticipantId()));
            }
            userIds.add(event.getOrganizerEvent().getOrganizerId());

            // Форматируем дату и время
            String date = event.getDateTime().format(DATE_FORMATTER);
            String time = event.getDateTime().format(TIME_FORMATTER);

            // Мультиязычные тексты
            Map<String, String> confirmedTexts = Map.of(
                "ru", String.format("Отличные новости! Заведение %s подтвердило ваше бронирование на %s в %s. Готовьтесь к игре! ⚽️", place.getName(), date, time),
                "uz", String.format("Ajoyib yangilik! %s joyi sizning broningizni %s kuni, soat %s ga tasdiqladi. O‘yin uchun tayyorlaning! ⚽️", place.getName(), date, time),
                "en", String.format("Great news! Venue %s has confirmed your booking for %s at %s. Get ready to play! ⚽️", place.getName(), date, time)
            );
            Map<String, String> rejectedTexts = Map.of(
                "ru", String.format("К сожалению, заведение %s отклонило ваше бронирование. Попробуйте выбрать другую дату или место.", place.getName()),
                "uz", String.format("Afsuski, %s joyi sizning broningizni rad etdi. Iltimos, boshqa sana yoki joy tanlang.", place.getName()),
                "en", String.format("Unfortunately, venue %s has rejected your booking. Please try another date or place.", place.getName())
            );
            Map<String, String> changesTexts = Map.of(
                "ru", String.format("Заведение %s запросило изменения в вашем событии. Проверьте детали и подтвердите!", place.getName()),
                "uz", String.format("%s joyi tadbiringizda o‘zgarishlarni so‘radi. Tafsilotlarni tekshirib, tasdiqlang!", place.getName()),
                "en", String.format("Venue %s has requested changes to your event. Please check the details and confirm!", place.getName())
            );
            Map<String, String> pendingTexts = Map.of(
                "ru", String.format("Вы на шаг ближе к игре! Ваша заявка на проведение игры отправлена в %s. Ждём ответа от заведения. Держим кулачки! 🤞", place.getName()),
                "uz", String.format("O‘yinga bir qadam yaqinroq! O‘yin o‘tkazish uchun so‘rovingiz %s joyiga yuborildi. Javobni kutamiz. Omad tilaymiz! 🤞", place.getName()),
                "en", String.format("You’re one step closer to the game! Your request to host a game has been sent to %s. Waiting for the venue’s response. Fingers crossed! 🤞", place.getName())
            );
            Map<String, String> inProgressTexts = Map.of(
                "ru", "Ну что ж, этот момент настал! Ваша игра начинается прямо сейчас. Желаем крутых эмоций и отличной компании! Удачи на поле! ⚽️🔥",
                "uz", "Mana shu lahza yetib keldi! O‘yin hozir boshlanadi. Zo‘r kayfiyat va yaxshi jamoa tilaymiz! Omad! ⚽️🔥",
                "en", "The moment has come! Your game starts now. Wishing you great vibes and awesome teammates! Good luck on the field! ⚽️🔥"
            );

            for (Long userId : userIds) {
                // Получаем язык пользователя
                String lang = "ru";
                try {
                    UserAdditionalInfo info = userAdditionalInfoRepository.findById(userId).orElse(null);
                    if (info != null && info.getLanguage() != null) {
                        lang = info.getLanguage();
                    }
                } catch (Exception ignored) {}

                String title = switch (lang) {
                    case "uz" -> "Tadbir holati o‘zgardi";
                    case "en" -> "Event status updated";
                    default -> "Статус события изменен";
                };
                String body = switch (newStatus) {
                    case CONFIRMED -> confirmedTexts.getOrDefault(lang, confirmedTexts.get("ru"));
                    case REJECTED -> rejectedTexts.getOrDefault(lang, rejectedTexts.get("ru"));
                    case CHANGES_REQUESTED -> changesTexts.getOrDefault(lang, changesTexts.get("ru"));
                    case CANCELLED -> rejectedTexts.getOrDefault(lang, changesTexts.get("ru"));
                    case PENDING_APPROVAL -> pendingTexts.getOrDefault(lang, pendingTexts.get("ru"));
                    case IN_PROGRESS -> inProgressTexts.getOrDefault(lang, inProgressTexts.get("ru"));
                    default -> switch (lang) {
                        case "uz" -> String.format("Tadbir holati %s ga o‘zgardi", newStatus.name());
                        case "en" -> String.format("Event status changed to %s", newStatus.name());
                        default -> String.format("Статус события изменен на %s", newStatus.name());
                    };
                };

                List<UserFcmToken> tokens = userFcmTokenRepository.findByUserId(userId);
                for (UserFcmToken token : tokens) {
                    Message message = Message.builder()
                        .setToken(token.getToken())
                        .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                        .putData("type", "EVENT_STATUS_CHANGED")
                        .putData("eventId", event.getEventId().toString())
                        .putData("newStatus", newStatus.name())
                        .build();

                    String response = firebaseMessaging.send(message);
                    log.info("Successfully sent event status change notification to user {}: {}", userId, response);
                }
            }
        } catch (Exception e) {
            log.error("Error sending event status change notification", e);
        }
    }

    // Уведомление о новом сообщении в чате
    public void sendNewChatMessageNotification(Event event, String senderName, String messageText, Long senderId) {
        try {
            // Получаем токены всех участников ивента, кроме отправителя
            List<UserFcmToken> participantTokens = userFcmTokenRepository.findByEventIdAndUserIdNot(
                event.getEventId(), senderId);

            for (UserFcmToken token : participantTokens) {
                Message message = Message.builder()
                    .setToken(token.getToken())
                    .setNotification(Notification.builder()
                        .setTitle(String.format("Новое сообщение в \"%s\"", event.getSportEvent().getSportName()))
                        .setBody(String.format("%s: %s", senderName, messageText))
                        .build())
                    .putData("type", "NEW_CHAT_MESSAGE")
                    .putData("eventId", event.getEventId().toString())
                    .putData("senderId", senderId.toString())
                    .build();

                String response = firebaseMessaging.send(message);
                log.info("Successfully sent chat message notification: {}", response);
            }
        } catch (Exception e) {
            log.error("Error sending chat message notification", e);
        }
    }

    public void sendNewEventNotification(Event event) {
        try {
            // Получаем токены пользователей, у которых этот вид спорта в избранном
            List<UserFcmToken> interestedUserTokens = userFcmTokenRepository.findByFavoriteSportId(
                event.getSportEvent().getSportId(),
                event.getOrganizerEvent().getOrganizerId() // Исключаем организатора
            );
            Place place = placeRepository.findPlaceByPlaceId(event.getPlaceId());
            LocalDateTime eventDateTime = event.getDateTime();
            String formattedDate = eventDateTime.format(DATE_FORMATTER);
            String formattedTime = eventDateTime.format(TIME_FORMATTER);
            String title = "🎯 Новое событие по вашему любимому виду спорта!";
            String body = String.format("👋 Эй! Кто-то хочет поиграть в %s!\n\n🏟️ Место: %s\n📅 Дата: %s\n⏰ Время: %s\n\nПрисоединяйся к игре! 🎮",
                event.getSportEvent().getSportName(),
                place.getName(),
                formattedDate,
                formattedTime);
            for (UserFcmToken token : interestedUserTokens) {
                Message message = Message.builder()
                    .setToken(token.getToken())
                    .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                    .putData("type", "NEW_EVENT")
                    .putData("eventId", event.getEventId().toString())
                    .putData("sportId", event.getSportEvent().getSportId().toString())
                    .putData("placeId", event.getPlaceId().toString())
                    .putData("deepLink", String.format("placeandplay://event/%d", event.getEventId()))
                    .putData("click_action", "FLUTTER_NOTIFICATION_CLICK")
                    .build();
                String response = firebaseMessaging.send(message);
                log.info("Successfully sent new event notification: {}", response);
                // История уведомлений
                notificationService.createNotification(
                    token.getUserId(),
                    "NEW_EVENT",
                    title,
                    body,
                    Map.of(
                        "eventId", event.getEventId(),
                        "sportId", event.getSportEvent().getSportId(),
                        "placeId", event.getPlaceId()
                    )
                );
            }
        } catch (Exception e) {
            log.error("Error sending new event notification", e);
        }
    }

    public void sendSimpleNotification(Long userId, String title, String body, String type) {
        try {
            List<UserFcmToken> tokens = userFcmTokenRepository.findByUserId(userId);
            for (UserFcmToken token : tokens) {
                Message message = Message.builder()
                    .setToken(token.getToken())
                    .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                    .putData("type", type)
                    .build();
                String response = firebaseMessaging.send(message);
                log.info("Successfully sent simple notification: {}", response);
            }
        } catch (Exception e) {
            log.error("Error sending simple notification", e);
        }
    }
} 