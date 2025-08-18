package com.is.auth.model.phoneNumber;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "phone_number_verification_codes", schema = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PhoneNumberVerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false, length = 10)
    private int code;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean isVerified = false;

    public PhoneNumberVerificationCode(String phoneNumber, int code, int expirationMinutes) {
        this.phoneNumber = phoneNumber;
        this.code = code;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = createdAt.plusMinutes(expirationMinutes);
    }
}
