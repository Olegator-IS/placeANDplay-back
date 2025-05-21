package com.is.events.repository;

import com.is.events.model.chat.EventMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventMessageRepository extends JpaRepository<EventMessage, Long> {
    List<EventMessage> findByEventIdOrderBySentAtAsc(Long eventId);
    
    @Query("SELECT em FROM EventMessage em WHERE em.eventId = :eventId AND em.sentAt > :since ORDER BY em.sentAt ASC")
    List<EventMessage> findByEventIdAndSentAtAfterOrderBySentAtAsc(
        @Param("eventId") Long eventId, 
        @Param("since") LocalDateTime since
    );

    Page<EventMessage> findByEventId(Long eventId, Pageable pageable);
} 