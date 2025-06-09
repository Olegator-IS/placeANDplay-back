package com.is.events.repository;

import com.is.events.model.UserActivityTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserActivityTrackingRepository extends JpaRepository<UserActivityTracking, Long> {
    Optional<UserActivityTracking> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}