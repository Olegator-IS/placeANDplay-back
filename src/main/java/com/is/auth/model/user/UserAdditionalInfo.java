package com.is.auth.model.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "user_details", schema = "users")
@TypeDefs({
    @TypeDef(name = "json", typeClass = JsonType.class)
})
public class UserAdditionalInfo {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "hobbies", columnDefinition = "text")
    private String hobbies;

    @Type(type = "json")
    @Column(name = "favorite_sports", columnDefinition = "jsonb")
    private List<FavoriteSport> favoriteSports;

    @Column(name = "bio", columnDefinition = "text")
    private String bio;

    @Column(name = "profile_picture_url", length = 255)
    private String profilePictureUrl;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "current_location_city_id", nullable = false)
    private Integer currentLocationCityId;

    @Column(name = "current_location_country_id", nullable = false)
    private Integer currentLocationCountryId;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Type(type = "json")
    @Column(name = "availability", columnDefinition = "jsonb")
    private String availability;

    @Column(name = "looking_for", columnDefinition = "text")
    private String lookingFor;

    @Column(name = "open_to_new_connections")
    private Boolean openToNewConnections = true;

    @Column(name = "date_registered")
    private LocalDateTime dateRegistered;

    @PrePersist
    protected void onCreate() {
        dateRegistered = LocalDateTime.now();
    }

    public void setFavoriteSports(List<FavoriteSport> favoriteSports) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.favoriteSports = favoriteSports;
        } catch (Exception e) {
            throw new RuntimeException("Error setting favorite sports: " + e.getMessage());
        }
    }
}

