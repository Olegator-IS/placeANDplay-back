package com.is.events.repository;

import com.is.events.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventsRepository extends JpaRepository<Event, Integer> {
    List<Event> findAllByPlaceId(long placeId);
    Event findEventByEventId(long eventId);

    @Query("SELECT e FROM Event e WHERE e.status = 'OPEN' AND DATE(e.dateTime) = :today")
    List<Event> findOpenEventsForToday(@Param("today") LocalDate today);
}
