package com.is.events.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {
    
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    public UserProfileService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public String getProfilePictureUrl(Long userId) {
        String sql = "SELECT profile_picture_url FROM placeand_play.users.user_details WHERE user_id = ?";
        return jdbcTemplate.queryForObject(sql, String.class, userId);
    }
} 