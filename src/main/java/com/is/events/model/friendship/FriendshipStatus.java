package com.is.events.model.friendship;

public enum FriendshipStatus {
    PENDING,    // Ожидает подтверждения
    ACCEPTED,   // Подтверждено
    REJECTED,   // Отклонено
    BLOCKED,    // Заблокировано
    REMOVED     // Удалено
}