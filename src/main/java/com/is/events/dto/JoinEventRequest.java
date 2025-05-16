package com.is.events.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinEventRequest {
    @NotNull(message = "participantId is required")
    private Long participantId;

    @NotBlank(message = "participantName is required")
    private String participantName;
} 