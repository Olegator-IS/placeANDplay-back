package com.is.events.model;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizerEvent {
    private Long organizerId;
    private String organizerName;
}
