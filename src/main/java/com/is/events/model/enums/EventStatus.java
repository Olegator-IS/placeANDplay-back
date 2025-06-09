package com.is.events.model.enums;

import java.util.Set;
import java.util.EnumSet;

public enum EventStatus {
    PENDING_APPROVAL("Ожидает подтверждения"),
    CHANGES_REQUESTED("Требуются изменения"),
    REJECTED("Отклонено"),
    CONFIRMED("Подтверждено"),
    IN_PROGRESS("В процессе"),
    COMPLETED("Завершено"),
    CANCELLED("Отменено"),
    EXPIRED("Просрочено");

    private final String displayName;

    EventStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canTransitionTo(EventStatus newStatus) {
        return getAllowedTransitions(this).contains(newStatus);
    }

    private static Set<EventStatus> getAllowedTransitions(EventStatus currentStatus) {
        return switch (currentStatus) {
            case PENDING_APPROVAL -> EnumSet.of(
                CONFIRMED,
                REJECTED,
                CHANGES_REQUESTED,
                CANCELLED
            );
            case CHANGES_REQUESTED -> EnumSet.of(
                PENDING_APPROVAL, // После внесения изменений организатором
                REJECTED // Если изменения не были внесены вовремя
            );
            case CONFIRMED -> EnumSet.of(
                IN_PROGRESS, // Когда событие начинается
                CANCELLED // Только организация может отменить до начала события
            );
            case IN_PROGRESS -> EnumSet.of(
                COMPLETED // После завершения события
            );
            case REJECTED, COMPLETED, CANCELLED, EXPIRED -> EnumSet.noneOf(EventStatus.class);
        };
    }

    public static boolean isValid(String status) {
        try {
            EventStatus.valueOf(status.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
} 