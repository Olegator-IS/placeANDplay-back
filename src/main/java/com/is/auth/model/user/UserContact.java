package com.is.auth.model.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "user_contacts", schema = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserContact {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "telegram")
    private String telegram;

    @Column(name = "instagram")
    private String instagram;
} 