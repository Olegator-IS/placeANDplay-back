package com.is.events.api;

import com.is.events.model.Event;
import com.is.events.service.EventsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/private/events")
@Slf4j
public class EventsController {
    private final EventsService eventsService;

    @Autowired
    public EventsController(EventsService eventsService) {
        this.eventsService = eventsService;
    }
    @GetMapping("/getAllEvents")
    public ResponseEntity<List<Event>> getAllEventsByCity(@RequestParam long currentCity) {
        List<Event> events = eventsService.getAllEvents(currentCity);
        return events.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(events);
    }

    @GetMapping("/getEvents")
    public ResponseEntity<List<Event>> getEventsByPlaceId(@RequestParam long placeId) {
        List<Event> events = eventsService.getAllEventsByCity(placeId);
        return events.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(events);
    }

    @GetMapping("/getEvent")
    public ResponseEntity<Event> getEventByEventId(@RequestParam long eventId) {
        Event events = eventsService.getEventById(eventId);
        return events==null ? ResponseEntity.noContent().build() : ResponseEntity.ok(events);
    }

    @PostMapping("/addEvent")
    public ResponseEntity<Event> addEvent(@RequestBody Event event) {
        Event createdEvent = eventsService.addEvent(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent);
    }
}
