package com.is.events.model;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_activity_tracking", schema = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityTracking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    @Column(name = "user_id")
    long userId;
    @Column(name = "is_first_event_creation")
    boolean isFirstEventCreation;
    @Column(name = "is_first_event_join")
    boolean isFirstEventJoin;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
