package com.is.auth.model.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "user_favorite_sports", schema = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(UserFavoriteSportId.class)
public class UserFavoriteSport {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "sport_id")
    private Integer sportId;

    @Column(name = "skill_id")
    private Integer skillId;

    @Column(name = "ready_to_teach")
    private Boolean readyToTeach;
} 