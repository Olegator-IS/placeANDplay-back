package com.is.auth.repository;

import com.is.auth.model.email.EmailVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {

    Optional<EmailVerificationCode> findByEmailAndCodeAndIsVerifiedFalse(String email, int code);

    @Transactional
    @Modifying
    @Query("DELETE FROM EmailVerificationCode e WHERE e.expiresAt < CURRENT_TIMESTAMP")
    void deleteExpiredCodes();

    @Query("SELECT COUNT(e) FROM EmailVerificationCode e WHERE e.email = :email AND e.createdAt > :timeLimit")
    long countRecentRequests(@Param("email") String email, @Param("timeLimit") LocalDateTime timeLimit);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isEmailVerified = true WHERE u.email = :email")
    void updateUserIsEmailVerified(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email AND u.isEmailVerified = true")
    boolean checkIsEmailVerified(@Param("email") String email);


}
