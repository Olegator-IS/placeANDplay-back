package com.is.events.repository;

import com.is.events.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventsRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    
    Event findEventByEventId(Long eventId);
    
    Page<Event> findAllByPlaceId(Long placeId, Pageable pageable);
    
    List<Event> findAllByPlaceId(Long placeId);
    
    @Query("SELECT e FROM Event e WHERE e.status = 'OPEN' AND DATE(e.dateTime) = :today")
    List<Event> findOpenEventsForToday(@Param("today") LocalDate today);

    List<Event> findByStatusAndDateTimeBefore(String status, LocalDateTime dateTime);
    
    List<Event> findByStatus(String status);

    @Query(value = """
            SELECT DISTINCT e.* FROM events.events e 
            WHERE e.status = 'COMPLETED' 
            AND (
                CAST((e.organizer_event->>'organizerId') AS bigint) = :userId
                OR EXISTS (
                    SELECT 1 
                    FROM jsonb_array_elements(e.current_participants->'participants') as p 
                    WHERE CAST((p->>'participantId') AS bigint) = :userId
                )
            )
            ORDER BY e.date_time DESC 
            LIMIT 3
            """, nativeQuery = true)
    List<Event> findLastThreeCompletedEventsByUser(Long userId);

    @Query(value = """
            SELECT DISTINCT e.* FROM events.events e 
            WHERE e.status = 'OPEN'
            AND (
                CAST((e.organizer_event->>'organizerId') AS bigint) = :userId
                OR EXISTS (
                    SELECT 1 
                    FROM jsonb_array_elements(e.current_participants->'participants') as p 
                    WHERE CAST((p->>'participantId') AS bigint) = :userId
                )
            )
            ORDER BY e.date_time DESC 
            """, nativeQuery = true)
    List<Event> findAllActivityByUser(Long userId);
}
