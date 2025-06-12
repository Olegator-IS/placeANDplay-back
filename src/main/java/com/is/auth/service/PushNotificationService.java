package com.is.auth.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.is.auth.model.UserFcmToken;
import com.is.auth.repository.UserFcmTokenRepository;
import com.is.events.model.Event;
import com.is.events.model.EventParticipant;
import com.is.events.model.enums.EventStatus;
import com.is.places.model.Place;
import com.is.places.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {
    
    private final FirebaseMessaging firebaseMessaging;
    private final UserFcmTokenRepository userFcmTokenRepository;
    private final PlaceRepository placeRepository;

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

            List<UserFcmToken> organizerTokens = userFcmTokenRepository.findByUserId(event.getOrganizerEvent().getOrganizerId());
            
            String title = "Статус события изменен";
            String body = switch (newStatus) {
                case CONFIRMED -> String.format("Событие по игре в \"%s\" подтверждено организацией \"%s\" ", event.getSportEvent().getSportName(),place.getName());
                case REJECTED -> String.format("Событие по игре \"%s\" отклонено организацией \"%s\" ", event.getSportEvent().getSportName(),place.getName());
                case CHANGES_REQUESTED -> String.format("Организация \"%s\" запросила изменения в событии \"%s\"", place.getName(),event.getSportEvent().getSportName());
                default -> String.format("Статус события \"%s\" изменен на %s", event.getSportEvent().getSportName(), newStatus);
            };

            for (UserFcmToken token : organizerTokens) {
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
                log.info("Successfully sent event status change notification: {}", response);
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
} 