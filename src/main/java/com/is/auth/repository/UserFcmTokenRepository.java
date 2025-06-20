package com.is.auth.repository;

import com.is.auth.model.UserFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, Long> {
    
    List<UserFcmToken> findByUserId(Long userId);
    
    Optional<UserFcmToken> findByToken(String token);
    
    @Query(value = """
            SELECT DISTINCT t.* FROM user_fcm_token t 
            WHERE t.user_id IN (
                SELECT CAST((p->>'participantId') AS bigint) 
                FROM events.events e,
                jsonb_array_elements(e.current_participants->'participants') as p 
                WHERE e.event_id = :eventId
            )
            AND t.user_id != :excludeUserId
            """, nativeQuery = true)
    List<UserFcmToken> findByEventIdAndUserIdNot(
        @Param("eventId") Long eventId, 
        @Param("excludeUserId") Long excludeUserId);
        
    void deleteByToken(String token);
} 