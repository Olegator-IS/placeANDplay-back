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
            SELECT e.* FROM events.events e 
            WHERE e.status = 'CONFIRMED' 
            AND e.date_time BETWEEN :startTime AND :endTime
            ORDER BY e.date_time ASC
            """, nativeQuery = true)
    List<Event> findConfirmedEventsStartingInHour(@Param("startTime") LocalDateTime startTime, 
                                                  @Param("endTime") LocalDateTime endTime);

    @Query(value = """
            SELECT e.* FROM events.events e 
            WHERE e.status = 'IN_PROGRESS' 
            AND (
                CAST((e.organizer_event->>'organizerId') AS bigint) = :userId
                OR EXISTS (
                    SELECT 1 
                    FROM jsonb_array_elements(e.current_participants->'participants') as p 
                    WHERE CAST((p->>'participantId') AS bigint) = :userId
                )
            )
            ORDER BY e.date_time ASC 
            LIMIT 1
            """, nativeQuery = true)
    Event findCurrentInProgressEventForUser(@Param("userId") Long userId);

    @Query(value = """
            SELECT e.* FROM events.events e 
            WHERE DATE(e.date_time) = :today
            AND (
                CAST((e.organizer_event->>'organizerId') AS bigint) = :userId
                OR EXISTS (
                    SELECT 1 
                    FROM jsonb_array_elements(e.current_participants->'participants') as p 
                    WHERE CAST((p->>'participantId') AS bigint) = :userId
                )
            )
            AND e.status NOT IN ('EXPIRED', 'CANCELLED')
            ORDER BY e.date_time ASC
            """, nativeQuery = true)
    List<Event> findEventsForTodayByUser(@Param("userId") Long userId, @Param("today") LocalDate today);

    @Query(value = """
            SELECT e.* FROM events.events e 
            WHERE e.date_time > :currentTime
            AND (
                CAST((e.organizer_event->>'organizerId') AS bigint) = :userId
                OR EXISTS (
                    SELECT 1 
                    FROM jsonb_array_elements(e.current_participants->'participants') as p 
                    WHERE CAST((p->>'participantId') AS bigint) = :userId
                )
            )
            AND e.status IN ('CONFIRMED', 'IN_PROGRESS')
            ORDER BY e.date_time ASC 
            LIMIT 1
            """, nativeQuery = true)
    Event findNearestEventForUser(@Param("userId") Long userId, @Param("currentTime") LocalDateTime currentTime);

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
            """, 
            countQuery = """
            SELECT COUNT(*) FROM events.events e 
            WHERE e.place_id = :placeId
            AND (:statuses IS NULL OR e.status = ANY(:statuses))
            """,
            nativeQuery = true)
    Page<Event> findOrganizationEventsByStatus(
        @Param("placeId") Long placeId,
        @Param("statuses") String[] statuses,
        Pageable pageable
    );

    @Query(value = """
            SELECT e.* FROM events.events e 
            WHERE e.date_time > :currentTime
            AND e.status IN ('OPEN', 'CONFIRMED', 'PENDING_APPROVAL')
            AND (
                CAST((e.organizer_event->>'organizerId') AS bigint) = :userId
                OR EXISTS (
                    SELECT 1 
                    FROM jsonb_array_elements(e.current_participants->'participants') as p 
                    WHERE CAST((p->>'participantId') AS bigint) = :userId
                )
            )
            ORDER BY e.date_time ASC 
            LIMIT 1
            """, nativeQuery = true)
    Event findNearestUpcomingEventByUser(@Param("userId") Long userId, @Param("currentTime") LocalDateTime currentTime);

    @Query(value = """
            SELECT e.* FROM events.events e 
            WHERE e.place_id = :placeId
            AND DATE(e.date_time) = DATE(:currentDate)
            AND e.status IN ('OPEN', 'CONFIRMED', 'PENDING_APPROVAL')
            AND (
                CAST((e.organizer_event->>'organizerId') AS bigint) = :userId
                OR EXISTS (
                    SELECT 1 
                    FROM jsonb_array_elements(e.current_participants->'participants') as p 
                    WHERE CAST((p->>'participantId') AS bigint) = :userId
                )
            )
            ORDER BY e.date_time ASC
            """, nativeQuery = true)
    List<Event> findUserEventsByPlaceAndDate(@Param("placeId") Long placeId, @Param("userId") Long userId, @Param("currentDate") LocalDate currentDate);

    @Query(value = """
        SELECT e.* FROM events.events e
        WHERE e.place_id = :placeId
          AND e.status NOT IN ('EXPIRED', 'COMPLETED', 'CANCELLED')
          AND (
            CAST((e.organizer_event->>'organizerId') AS bigint) = :userId
            OR EXISTS (
              SELECT 1
              FROM jsonb_array_elements(e.current_participants->'participants') as p
              WHERE CAST((p->>'participantId') AS bigint) = :userId
            )
          )
          AND (
            (e.date_time >= :now AND e.date_time <= :now_plus_30)
            OR (e.date_time < :now)
          )
        ORDER BY e.date_time DESC
        """, nativeQuery = true)
    List<Event> findEventsForCheckIn(
        @Param("placeId") Long placeId,
        @Param("userId") Long userId,
        @Param("now") java.time.LocalDateTime now,
        @Param("now_plus_30") java.time.LocalDateTime nowPlus30
    );

    @Query(value = """
        SELECT e.* FROM events.events e
        WHERE e.place_id = :placeId
          AND DATE(e.date_time) = :today
          AND (
            CAST((e.organizer_event->>'organizerId') AS bigint) = :userId
            OR EXISTS (
              SELECT 1
              FROM jsonb_array_elements(e.current_participants->'participants') as p
              WHERE CAST((p->>'participantId') AS bigint) = :userId
            )
          )
          AND (
            (
              e.status NOT IN ('EXPIRED', 'COMPLETED', 'CANCELLED')
              AND e.date_time >= :now
              AND e.date_time <= :now_plus_30
            )
            OR (
              e.status = 'IN_PROGRESS'
              AND e.date_time < :now
            )
          )
        ORDER BY e.date_time DESC
        """, nativeQuery = true)
    List<Event> findEventsForCheckInToday(
        @Param("placeId") Long placeId,
        @Param("userId") Long userId,
        @Param("today") java.time.LocalDate today,
        @Param("now") java.time.LocalDateTime now,
        @Param("now_plus_30") java.time.LocalDateTime nowPlus30
    );

    @Query(value = """
        SELECT e.* FROM events.events e
        WHERE e.place_id = :placeId
          AND DATE(e.date_time) = :today
          AND e.status NOT IN ('EXPIRED', 'COMPLETED', 'CANCELLED')
          AND e.date_time <= :now_plus_30
        ORDER BY e.date_time DESC
        """, nativeQuery = true)
    List<Event> findEventsForCheckInTodaySimple(
        @Param("placeId") Long placeId,
        @Param("today") java.time.LocalDate today,
        @Param("now_plus_30") java.time.LocalDateTime nowPlus30
    );

//    List<Event> findByStatusAndStartDateTimeBefore(String status, LocalDateTime dateTime);
    
//    List<Event> findByStatusAndEndDateTimeBefore(String status, LocalDateTime dateTime);
    
//    @Query("SELECT e FROM Event e WHERE e.organizerId = :organizerId AND DATE(e.startDateTime) = DATE(:date)")
//    List<Event> findByOrganizerAndDate(@Param("organizerId") Long organizerId, @Param("date") LocalDateTime date);
}