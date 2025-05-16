package com.is.auth.model.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "user_reputation", schema = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserReputation {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "rating", precision = 2, scale = 1)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "reviews_count")
    private Integer reviewsCount = 0;
} 