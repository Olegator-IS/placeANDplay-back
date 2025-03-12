package com.is.events.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ParticipantDTO {
    private Long participantId;
    private String participantName;
    private LocalDateTime joinedAt;
    private String profilePictureUrl;
    private String status; // например: ACTIVE, CANCELLED
    
    // Конструктор для удобства создания
    public ParticipantDTO(Long participantId, String participantName, LocalDateTime joinedAt) {
        this.participantId = participantId;
        this.participantName = participantName;
        this.joinedAt = joinedAt;
        this.status = "ACTIVE";
    }
    
    // Конструктор для создания с профилем
    public ParticipantDTO(Long participantId, String participantName, LocalDateTime joinedAt, String profilePictureUrl) {
        this.participantId = participantId;
        this.participantName = participantName;
        this.joinedAt = joinedAt;
        this.profilePictureUrl = profilePictureUrl;
        this.status = "ACTIVE";
    }
    
    // Пустой конструктор для Jackson
    public ParticipantDTO() {
    }
} 