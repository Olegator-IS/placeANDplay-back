package com.is.events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearestEventDTO {
    private Long eventId;
    private String title;
    private String description;
    private LocalDateTime dateTime;
    private String status;
    private Long placeId;
    private OrganizerDTO organizer;
    private int participantsCount;
    private int maxParticipants;
    private String eventType;
    private String location;
    private Double price;
    private String additionalInfo;
    
    // Информация о времени до события
    private String timeUntilEvent; // например: "2 дня 5 часов 30 минут" или "45 минут"
    private long totalMinutesUntilEvent; // общее количество минут до события
    private boolean isUpcoming; // true если событие в будущем
    private String timeFormat; // "DAYS", "HOURS", "MINUTES" - для определения формата отображения
} 