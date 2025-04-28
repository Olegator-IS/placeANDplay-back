package com.is.events.repository;

import com.is.events.model.EventMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventMessageRepository extends JpaRepository<EventMessage, Long> {
    Page<EventMessage> findByEventIdOrderBySentAtDesc(Long eventId, Pageable pageable);
} 