package com.is.events.repository;

import com.is.events.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventsRepository extends JpaRepository<Event, Long> {
    
    Event findEventByEventId(Long eventId);
    
    Page<Event> findAllByPlaceId(Long placeId, Pageable pageable);
    
    List<Event> findAllByPlaceId(Long placeId);
    
    @Query("SELECT e FROM Event e WHERE e.status = 'OPEN' AND DATE(e.dateTime) = :today")
    List<Event> findOpenEventsForToday(@Param("today") LocalDate today);

    List<Event> findByStatusAndDateTimeBefore(String status, LocalDateTime dateTime);
    
    List<Event> findByStatus(String status);
}
