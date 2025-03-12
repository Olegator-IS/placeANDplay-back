package com.is.events.dto;

import lombok.Data;

@Data
public class OrganizerDTO {
    private Long organizerId;
    private String name;
    private String email;
    private String phoneNumber;
    private String organizationType; // например: INDIVIDUAL, COMPANY, CLUB
    private Double rating;
    private String profilePictureUrl;
} 