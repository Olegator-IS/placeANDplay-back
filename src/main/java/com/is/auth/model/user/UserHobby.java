package com.is.auth.model.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "user_hobbies", schema = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(UserHobbyId.class)
public class UserHobby {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "hobby")
    private String hobby;
} 