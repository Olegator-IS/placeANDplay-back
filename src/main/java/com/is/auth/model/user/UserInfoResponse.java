package com.is.auth.model.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.is.auth.model.dto.ActivityStatsDTO;
import com.is.auth.model.dto.ReputationDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserInfoResponse {
    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("role")
    private String role;

    @JsonProperty("is_email_verified")
    private Boolean isEmailVerified;

    @JsonProperty("profile_picture_url")
    private String profilePictureUrl;

    @JsonProperty("hobbies")
    private List<String> hobbies;

    @JsonProperty("favorite_sports")
    private List<FavoriteSport> favoriteSports;

    @JsonProperty("bio")
    private String bio;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("birth_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @JsonProperty("birth_year")
    private Integer birthYear;

    @JsonProperty("city")
    private Long city;

    @JsonProperty("country")
    private Long country;

    @JsonProperty("availability")
    private Map<String, Object> availability;

    @JsonProperty("looking_for")
    private String lookingFor;

    @JsonProperty("open_to_new_connections")
    private Boolean openToNewConnections;

    @JsonProperty("contacts")
    private Map<String, String> contacts;

    @JsonProperty("activity_stats")
    private ActivityStatsDTO activityStats;

    @JsonProperty("reputation")
    private ReputationDTO reputation;

    @JsonProperty("date_registered")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateRegistered;
} 