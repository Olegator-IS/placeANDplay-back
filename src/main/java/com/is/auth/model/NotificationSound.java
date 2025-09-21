package com.is.auth.model;

/**
 * Enum для типов звуков уведомлений
 */
public enum NotificationSound {
    
    // Звуки для чата
    CHAT_MESSAGE("chat_message.wav", "Звук нового сообщения в чате"),
    SYSTEM_NOTIFICATION("system_notification.wav", "Звук системного уведомления"),
    
    // Звуки для событий
    NEW_EVENT("new_event.wav", "Звук нового события"),
    EVENT_CONFIRMED("event_confirmed.wav", "Звук подтверждения события"),
    EVENT_CANCELLED("event_cancelled.wav", "Звук отмены события"),
    EVENT_STARTED("event_started.wav", "Звук начала события"),
    
    // Звуки для участников
    PARTICIPANT_JOINED("participant_joined.wav", "Звук присоединения участника"),
    PARTICIPANT_LEFT("participant_left.wav", "Звук выхода участника"),
    
    // Звуки для дружбы
    FRIEND_REQUEST("friend_request.wav", "Звук запроса дружбы"),
    FRIEND_ACCEPTED("friend_accepted.wav", "Звук принятия дружбы"),
    
    // Общие звуки
    DEFAULT("default.wav", "Стандартный звук уведомления"),
    SILENT("silent.wav", "Тихий звук"),
    VIBRATION_ONLY("vibration_only.wav", "Только вибрация");
    
    private final String fileName;
    private final String description;
    
    NotificationSound(String fileName, String description) {
        this.fileName = fileName;
        this.description = description;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Получить звук по типу уведомления
     */
    public static NotificationSound getSoundForNotificationType(String notificationType) {
        return switch (notificationType) {
                case "NEW_CHAT_MESSAGE" -> CHAT_MESSAGE;
                case "SYSTEM_CHAT_MESSAGE" -> SYSTEM_NOTIFICATION;
                case "NEW_EVENT" -> NEW_EVENT;
                case "EVENT_CONFIRMED" -> EVENT_CONFIRMED;
                case "EVENT_CANCELLED" -> EVENT_CANCELLED;
                case "EVENT_STARTED" -> EVENT_STARTED;
                case "PARTICIPANT_JOINED" -> PARTICIPANT_JOINED;
                case "PARTICIPANT_LEFT" -> PARTICIPANT_LEFT;
                case "FRIEND_REQUEST" -> FRIEND_REQUEST;
                case "FRIEND_ACCEPTED" -> FRIEND_ACCEPTED;
            default -> DEFAULT;
        };
    }
}
