package com.is.events.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentParticipants {
    private Long participantId;
    private String participantName;
}