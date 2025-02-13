package com.is.auth.model.user;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "user_details")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)

@Getter
@Setter
public class UserAdditionalInfo {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(columnDefinition = "text")
    private String hobbies;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private String favoriteSports;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(name = "profile_picture_url", length = 255)
    private String profilePictureUrl;

    private LocalDateTime lastLogin;

    @Column(name = "current_location_city_id")
    private Integer currentLocationCityId;

    @Column(name = "current_location_country_id")
    private Integer currentLocationCountryId;

    // Вспомогательный метод для работы с JSONB
    @Transient // Это поле не будет сохраняться в базе данных
    private List<FavoriteSport> favoriteSportObjects;

    @PostLoad
    private void initFavoriteSportObjects() {
        if (this.favoriteSports != null) {
            this.favoriteSportObjects = FavoriteSport.fromJson(this.favoriteSports);
        }
    }

    @PrePersist
    @PreUpdate
    private void updateFavoriteSports() {
        if (this.favoriteSportObjects != null) {
            this.favoriteSports = FavoriteSport.toJson(this.favoriteSportObjects);
        }
    }
}

