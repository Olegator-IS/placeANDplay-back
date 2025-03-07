package com.is.events.model.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

public enum EventStatus {
    OPEN(Set.of("COMPLETED", "CANCELLED")),
    COMPLETED(Set.of()),
    CANCELLED(Set.of()),
    EXPIRED(Set.of());

    private final Set<String> allowedTransitions;

    EventStatus(Set<String> allowedTransitions) {
        this.allowedTransitions = allowedTransitions;
    }

    public boolean canTransitionTo(EventStatus newStatus) {
        return allowedTransitions.contains(newStatus.name());
    }

    public static boolean isValid(String status) {
        return Arrays.stream(EventStatus.values())
                .map(Enum::name)
                .anyMatch(s -> s.equals(status.toUpperCase()));
    }
} 