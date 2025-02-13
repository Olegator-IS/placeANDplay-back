package com.is.events.repository;

import com.is.events.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventsRepository extends JpaRepository<Event, Integer> {
    List<Event> findAllByPlaceId(long placeId);
    Event findEventByEventId(long eventId);
}
