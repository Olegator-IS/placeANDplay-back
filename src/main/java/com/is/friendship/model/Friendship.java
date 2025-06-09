package com.is.friendship.model;

import com.is.auth.model.user.User;
import com.is.friendship.model.enums.FriendshipStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "friendships")
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendshipStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void validateFriendship() {
        if (user1 != null && user2 != null && user1.equals(user2)) {
            throw new IllegalArgumentException("Users cannot be friends with themselves");
        }
    }

    // Метод для проверки, является ли пользователь частью дружбы
    public boolean involvesUser(User user) {
        return user1.equals(user) || user2.equals(user);
    }

    // Метод для получения другого пользователя
    public User getOtherUser(User user) {
        if (user1.equals(user)) return user2;
        if (user2.equals(user)) return user1;
        throw new IllegalArgumentException("User is not part of this friendship");
    }

    // Метод для проверки, является ли пользователь инициатором
    public boolean isInitiator(User user) {
        return initiator.equals(user);
    }
} 