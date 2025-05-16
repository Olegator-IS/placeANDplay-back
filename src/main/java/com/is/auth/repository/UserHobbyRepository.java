package com.is.auth.repository;

import com.is.auth.model.user.UserHobby;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserHobbyRepository extends JpaRepository<UserHobby, Long> {
    List<UserHobby> findByUserId(Long userId);
    void deleteByUserIdAndHobby(Long userId, String hobby);
    void deleteByUserId(Long userId);
} 