package com.is.events.service;

import com.is.events.model.Event;
import com.is.events.repository.EventsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventsService {

    private final EventsRepository eventsRepository;

    public List<Event> getAllEvents(long placeId) {
        return eventsRepository.findAll();
    }

    public List<Event> getAllEventsByCity(long placeId) {
        return eventsRepository.findAllByPlaceId(placeId);
    }

    public Event addEvent(Event event) {
        if (event.getDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Дата события не может быть в прошлом");
        }
        return eventsRepository.save(event);
    }

    public Event getEventById(long eventId) {
        return eventsRepository.findEventByEventId(eventId);
    }
}
