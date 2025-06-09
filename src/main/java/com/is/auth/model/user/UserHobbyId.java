package com.is.auth.model.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserHobbyId implements Serializable {
    private Long userId;
    private String hobby;
} 