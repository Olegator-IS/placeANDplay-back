package com.is.auth.repository;

import com.is.auth.model.user.UserActivityStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserActivityStatsRepository extends JpaRepository<UserActivityStats, Long> {
    UserActivityStats findByUserId(Long userId);
} 