package com.is.events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventCreationAvailabilityResponse {
    private boolean available;
    private String message;
    private int eventsAsOrganizer;
    private int eventsAsParticipant;
    private int uniqueEvents;
    private int totalEvents;
} 