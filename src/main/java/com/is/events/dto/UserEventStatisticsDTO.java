package com.is.events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEventStatisticsDTO {
    private int totalEvents;
    private int eventsAsOrganizer;
    private int eventsAsParticipant;
} 