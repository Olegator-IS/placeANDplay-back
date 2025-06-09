package com.is.auth.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
public class ActivityStatsDTO {
    private Integer eventsPlayed;
    private Integer eventsOrganized;
    
//    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
//    private LocalDateTime lastActive;
} 