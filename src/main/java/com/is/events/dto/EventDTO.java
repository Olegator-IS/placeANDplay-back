package com.is.events.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class EventDTO {
    private Long eventId;
    private String title;
    private String description;
    private LocalDateTime dateTime;
    private String status;
    private Long placeId;
    private OrganizerDTO organizer;
    private List<ParticipantDTO> participants = new ArrayList<>();

    // Дополнительные поля для отображения
    private int participantsCount;
    private boolean isJoinable;
    private int maxParticipants;
    private String eventType;
    private String location;
    private Double price;
    private boolean isFirstEventCreation;
    private String additionalInfo;


    public void addParticipant(ParticipantDTO participant) {
        if (this.participants == null) {
            this.participants = new ArrayList<>();
        }
        this.participants.add(participant);
        this.participantsCount = this.participants.size();
    }

    public void removeParticipant(Long participantId) {
        if (this.participants != null) {
            this.participants.removeIf(p -> p.getParticipantId().equals(participantId));
            this.participantsCount = this.participants.size();
        }
    }
} 