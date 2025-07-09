package com.is.events.service;

import com.is.events.model.Event;
import com.is.events.model.enums.EventMessageType;
import com.is.events.model.enums.EventStatus;
import com.is.events.model.chat.ChatMessageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventMessageService {

    private final ChatService chatService;

    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("dd MMMM yyyy 'в' HH:mm", new Locale("ru"));

    public void sendEventMessage(Event event, EventMessageType messageType, String participantName, String language) {
        String message;
        switch (messageType) {
            case EVENT_CREATED -> message = String.format(
                "Организатор %s создал ивент на %s. Ожидается подтверждение со стороны %s.",
                event.getOrganizerEvent().getOrganizerName(),
                event.getDateTime().format(DATE_FORMATTER),
                getPlaceName(event.getPlaceId())
            );
            case STATUS_CHANGED -> {
                if (event.getStatus() == EventStatus.CONFIRMED) {
                    message = String.format(
                        "Статус изменен на ПОДТВЕРЖДЁН. Ивент начнется %s. " +
                        "Покинуть событие можно будет не позже чем за 2 часа до начала!",
                        event.getDateTime().format(DATE_FORMATTER)
                    );
                } else {
                    message = String.format(
                        "Статус ивента изменен на %s",
                        event.getStatus().name()
                    );
                }
            }
            case EVENT_EXPIRED -> message = String.format(
                    "Событие было переведено в просроченные,т.к организатор не ответил на это событие.",
                    participantName
            );
            case PARTICIPANT_JOINED -> message = String.format(
                "%s присоединился к ивенту.",
                participantName
            );
            case PARTICIPANT_LEFT -> message = String.format(
                "%s покинул ивент.",
                participantName
            );
            case EVENT_STARTED -> message = "🎉 Ивент начался! Желаем всем участникам отличной игры и спортивного настроения! 🏆";
            case PARTICIPANT_CHECKED_IN -> message = String.format(
                "%s отметил своё присутствие на ивенте!",
                participantName
            );
            default -> message = "Системное сообщение";
        }

        Long systemId = 0L;
        String accessTokenSystem = "SYSTEM";
        String refreshTokenSystem = "SYSTEM";
        
        ChatMessageRequest chatRequest = new ChatMessageRequest();
        chatRequest.setContent(message);
        chatService.sendMessage(event.getEventId(), systemId, chatRequest, accessTokenSystem, refreshTokenSystem, language);
        log.info("Sent event message: {}", message);
    }

    private String getPlaceName(Long placeId) {
        // Здесь нужно реализовать получение названия заведения по ID
        // Предполагаем, что у вас есть сервис для работы с заведениями
        return "название заведения"; // Временная заглушка
    }
} 