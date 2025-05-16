package com.is.auth.model.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_activity_stats", schema = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityStats {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "events_played")
    private Integer eventsPlayed = 0;

    @Column(name = "events_organized")
    private Integer eventsOrganized = 0;

    @Column(name = "last_active")
    private LocalDateTime lastActive;
} 