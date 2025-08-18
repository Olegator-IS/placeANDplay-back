package com.is.auth.repository;

import com.is.auth.model.phoneNumber.PhoneNumberVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PhoneNumberVerificationCodeRepository extends JpaRepository<PhoneNumberVerificationCode, Long> {

    Optional<PhoneNumberVerificationCode> findByPhoneNumberAndCodeAndIsVerifiedFalse(String phoneNumber, int code);

    boolean existsByPhoneNumber(String phoneNumber);

    @Transactional
    @Modifying
    @Query("DELETE FROM PhoneNumberVerificationCode e WHERE e.expiresAt < CURRENT_TIMESTAMP")
    void deleteExpiredCodes();

    @Query("SELECT COUNT(e) FROM PhoneNumberVerificationCode e WHERE e.phoneNumber = :phoneNumber AND e.createdAt > :timeLimit")
    long countRecentRequests(@Param("phoneNumber") String phoneNumber, @Param("timeLimit") LocalDateTime timeLimit);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.phoneVerified = true WHERE u.phoneNumber = :phoneNumber")
    void updateUserIsPhoneVerified(String phoneNumber);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.phoneNumber = :phoneNumber AND u.phoneVerified = true")
    boolean checkIsPhoneNumberVerified(@Param("phoneNumber") String phoneNumber);

}
