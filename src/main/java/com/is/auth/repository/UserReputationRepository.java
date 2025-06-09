package com.is.auth.repository;

import com.is.auth.model.user.UserReputation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserReputationRepository extends JpaRepository<UserReputation, Long> {
    UserReputation findByUserId(Long userId);
} 