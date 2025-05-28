package com.is.events.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventJoinAvailabilityResponse {
    private boolean available;
    private String message;
    private int eventsAsOrganizer;
    private int eventsAsParticipant;
    private int uniqueEvents;
    private int totalEvents;
} 