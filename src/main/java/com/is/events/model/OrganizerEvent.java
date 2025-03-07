package com.is.events.model;

import lombok.Data;

@Data
public class OrganizerEvent {
    private Long organizerId;
    private String organizerName;
    private String email;
    private String phoneNumber;
    private String organizationType;
    private Double rating;
}
