package com.is.events.repository;

import com.is.events.model.Event;
import com.is.events.model.enums.EventStatus;
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
import java.util.Set;

@Repository
public interface EventsRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    Event findEventByEventId(Long eventId);

    Page<Event> findAllByPlaceId(Long placeId, Pageable pageable);

    List<Event> findAllByPlaceId(Long placeId);

    @Query("SELECT e FROM Event e WHERE e.status = :status AND DATE(e.dateTime) = :today")
    List<Event> findOpenEventsForToday(@Param("status") EventStatus status, @Param("today") LocalDate today);

    List<Event> findByStatusAndDateTimeBefore(EventStatus status, LocalDateTime dateTime);

    List<Event> findByStatus(EventStatus status);

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
            WHERE e.status NOT IN ('EXPIRED', 'REJECTED')
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

    @Query(value = """
            SELECT COUNT(e.*) FROM events.events e 
            WHERE CAST((e.organizer_event->>'organizerId') AS bigint) = :organizerId
            AND DATE(e.date_time) = :date
            AND e.status NOT IN ('REJECTED', 'EXPIRED')
            """, nativeQuery = true)
    int countEventsByOrganizerAndDate(@Param("organizerId") Long organizerId, @Param("date") LocalDate date);

    @Query(value = """
            SELECT e.* FROM events.events e 
            WHERE CAST((e.organizer_event->>'organizerId') AS bigint) = :organizerId
            AND DATE(e.date_time) = :date
            AND e.status NOT IN ('REJECTED', 'EXPIRED')
            ORDER BY e.date_time DESC
            """, nativeQuery = true)
    List<Event> findEventsByOrganizerAndDate(@Param("organizerId") Long organizerId, @Param("date") LocalDate date);

    @Query(value = """
            SELECT COUNT(DISTINCT e.*) FROM events.events e 
            WHERE EXISTS (
                SELECT 1 
                FROM jsonb_array_elements(e.current_participants->'participants') as p 
                WHERE CAST((p->>'participantId') AS bigint) = :userId
            )
            AND CAST((e.organizer_event->>'organizerId') AS bigint) != :userId
            """, nativeQuery = true)
    int countEventsWhereUserIsParticipant(@Param("userId") Long userId);

    @Query(value = """
            SELECT COUNT(DISTINCT e.*) FROM events.events e 
            WHERE CAST((e.organizer_event->>'organizerId') AS bigint) = :userId
            """, nativeQuery = true)
    int countEventsWhereUserIsOrganizer(@Param("userId") Long userId);

    @Query(value = """
            SELECT COUNT(DISTINCT e.*) FROM events.events e 
            WHERE (
                CAST((e.organizer_event->>'organizerId') AS bigint) = :userId
                OR EXISTS (
                    SELECT 1 
                    FROM jsonb_array_elements(e.current_participants->'participants') as p 
                    WHERE CAST((p->>'participantId') AS bigint) = :userId
                )
            )
            """, nativeQuery = true)
    int countAllUserEvents(@Param("userId") Long userId);

    @Query(value = """
            SELECT e.* FROM events.events e 
            WHERE e.place_id = :placeId
            AND DATE(e.date_time) BETWEEN :startDate AND :endDate
            AND e.status NOT IN ('REJECTED', 'EXPIRED')
            ORDER BY e.date_time DESC
            """, nativeQuery = true)
    Page<Event> findEventsByPlaceAndDateRange(
        @Param("placeId") Long placeId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable
    );

    @Query(value = """
            SELECT COUNT(DISTINCT e.event_id) 
            FROM events.events e 
            WHERE CAST((e.organizer_event->>'organizerId') AS bigint) = :userId
            AND DATE(e.date_time) = :date
            AND e.status NOT IN ('REJECTED', 'EXPIRED', 'CANCELLED', 'COMPLETED')
            """, nativeQuery = true)
    int countUserEventsAsOrganizerForDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query(value = """
            SELECT COUNT(DISTINCT e.event_id) 
            FROM events.events e 
            WHERE EXISTS (
                SELECT 1 
                FROM jsonb_array_elements(e.current_participants->'participants') as p 
                WHERE CAST((p->>'participantId') AS bigint) = :userId
            )
            AND DATE(e.date_time) = :date
            AND e.status NOT IN ('REJECTED', 'EXPIRED', 'CANCELLED', 'COMPLETED')
            """, nativeQuery = true)
    int countUserEventsAsParticipantForDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query(value = """
            SELECT COUNT(DISTINCT e.event_id) 
            FROM events.events e 
            WHERE CAST((e.organizer_event->>'organizerId') AS bigint) = :userId
            AND EXISTS (
                SELECT 1 
                FROM jsonb_array_elements(e.current_participants->'participants') as p 
                WHERE CAST((p->>'participantId') AS bigint) = :userId
            )
            AND DATE(e.date_time) = :date
            AND e.status NOT IN ('REJECTED', 'EXPIRED', 'CANCELLED', 'COMPLETED')
            """, nativeQuery = true)
    int countUserEventsAsBothRolesForDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query(value = """
            SELECT e.* FROM events.events e 
            WHERE e.place_id = :placeId
            AND (:statuses IS NULL OR e.status = ANY(:statuses))
            """, nativeQuery = true)
    Page<Event> findOrganizationEventsByStatus(
        @Param("placeId") Long placeId,
        @Param("statuses") String[] statuses,
        Pageable pageable
    );

//    List<Event> findByStatusAndStartDateTimeBefore(String status, LocalDateTime dateTime);
    
//    List<Event> findByStatusAndEndDateTimeBefore(String status, LocalDateTime dateTime);
    
//    @Query("SELECT e FROM Event e WHERE e.organizerId = :organizerId AND DATE(e.startDateTime) = DATE(:date)")
//    List<Event> findByOrganizerAndDate(@Param("organizerId") Long organizerId, @Param("date") LocalDateTime date);
}