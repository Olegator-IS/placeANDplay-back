package com.is.auth.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.is.auth.model.user.FavoriteSport;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserInfoResponse {
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private boolean isEmailVerified;
    
    private String hobbies;
    private List<FavoriteSport> favoriteSports;
    private String bio;
    private String gender;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
    
    private Integer birthYear;
    
    private Integer city;
    private Integer country;
    
    private String availability;
    private String lookingFor;
    private Boolean openToNewConnections;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateRegistered;
    
    private Map<String, String> contacts;
    private ActivityStatsDTO activityStats;
    private ReputationDTO reputation;
} 