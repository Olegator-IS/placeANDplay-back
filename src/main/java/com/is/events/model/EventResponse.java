package com.is.events.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {
    private Long eventId;
    private Long placeId;
    private SportEvent sportEvent;
    private OrganizerEvent organizerEvent;
    private List<CurrentParticipants> currentParticipants;
    private String status;
    private String description;
    private String skillLevel;
    private LocalDateTime dateTime;
    private boolean availableToJoin;

    public EventResponse(Event event, boolean availableToJoin) {
        this.eventId = event.getEventId();
        this.placeId = event.getPlaceId();
        this.sportEvent = event.getSportEvent();
        this.organizerEvent = event.getOrganizerEvent();
        this.currentParticipants = Collections.singletonList(event.getCurrentParticipants());
        this.status = String.valueOf(event.getStatus());
        this.description = event.getDescription();
        this.skillLevel = event.getSkillLevel();
        this.dateTime = event.getDateTime();
        this.availableToJoin = availableToJoin;
    }
}