package com.is.auth.repository;

import com.is.auth.model.user.UserFavoriteSport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFavoriteSportRepository extends JpaRepository<UserFavoriteSport, Long> {
    List<UserFavoriteSport> findByUserId(Long userId);
    void deleteByUserIdAndSportId(Long userId, Integer sportId);
} 