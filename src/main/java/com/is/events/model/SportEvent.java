package com.is.events.model;

import lombok.Data;

@Data
public class SportEvent {
    private Long sportId;
    private String sportName;
    private String sportType;
    private Integer maxParticipants;
    private Double price;
    private String location;
    private String additionalInfo;
}