package com.is.events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInEventDTO {
    private Long eventId;
    private String title;
    private String description;
    private LocalDateTime dateTime;
    private String status;
    private String userRole; // "ORGANIZER" или "PARTICIPANT"
    private boolean canStart; // только для организатора
    private boolean canCheckIn; // для участника
    private String userStatus; // "ACTIVE", "PRESENT" - статус пользователя в ивенте
    private List<ParticipantDTO> participants;
    private OrganizerDTO organizer;
    private String location;
    private Double price;
    private String additionalInfo;
} 