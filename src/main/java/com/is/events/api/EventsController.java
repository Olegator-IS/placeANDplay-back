package com.is.events.api;

import com.is.events.dto.EventDTO;
import com.is.events.model.Event;
import com.is.events.service.EventsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import com.is.events.dto.JoinEventRequest;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Events API", description = "API для работы с событиями")
public class EventsController {

    private final EventsService eventsService;

    @Operation(summary = "Создать новое событие")
    @PostMapping
    public ResponseEntity<EventDTO> createEvent(
            @RequestBody @Valid Event event,
            @RequestHeader(defaultValue = "ru") String language) {
        return ResponseEntity.ok(eventsService.addEvent(event, language));
    }

    @Operation(summary = "Получить все события с пагинацией")
    @GetMapping
    public ResponseEntity<Page<EventDTO>> getAllEvents(
            @RequestParam Long placeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateTime") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestHeader(defaultValue = "ru") String language) {
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(eventsService.getAllEvents(placeId, pageRequest));
    }

    @Operation(summary = "Получить события по городу")
    @GetMapping("/city")
    public ResponseEntity<Page<EventDTO>> getAllEventsByCity(
            @RequestParam Long placeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(defaultValue = "ru") String language) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return ResponseEntity.ok(eventsService.getAllEventsByCity(placeId, pageRequest));
    }

    @Operation(summary = "Получить событие по ID")
    @GetMapping("/{eventId}")
    public ResponseEntity<EventDTO> getEventById(
            @PathVariable Long eventId,
            @RequestHeader(defaultValue = "ru") String language) {
        return ResponseEntity.ok(eventsService.getEventById(eventId, language));
    }

    @Operation(summary = "Присоединиться к событию")
    @PostMapping(value = "/{eventId}/join", consumes = "application/json")
    public ResponseEntity<EventDTO> joinEvent(
            @PathVariable Long eventId,
            @RequestBody @Valid JoinEventRequest request,
            @RequestHeader(defaultValue = "ru") String language) {
        log.info("Joining event {} with request: {}", eventId, request);
        return ResponseEntity.ok(eventsService.joinEvent(eventId, request.getParticipantId(), request.getParticipantName(), language));
    }

    @Operation(summary = "Изменить статус события")
    @PutMapping("/{eventId}/status")
    public ResponseEntity<Event> eventAction(
            @PathVariable Long eventId,
            @RequestParam Long userId,
            @RequestParam String action,
            @RequestHeader(defaultValue = "ru") String language) {
        return ResponseEntity.ok(eventsService.eventAction(eventId, userId, action, language));
    }

    @GetMapping("/today")
    @Operation(summary = "Получить события на сегодня")
    public ResponseEntity<List<EventDTO>> getEventsForToday(
            @RequestHeader(defaultValue = "ru") String language) {
        List<Event> events = eventsService.getEventsForToday(java.time.LocalDate.now());
        if (events.isEmpty()) {
            log.info("No events found for today");
            return ResponseEntity.ok(List.of());
        }
        
        List<EventDTO> eventDTOs = events.stream()
                .map(eventsService::convertToDTO)
                .collect(Collectors.toList());
        log.info("Found {} events for today", eventDTOs.size());
        return ResponseEntity.ok(eventDTOs);
    }

    @Operation(summary = "Выйти из события")
    @PostMapping("/{eventId}/leave")
    public ResponseEntity<EventDTO> leaveEvent(
            @PathVariable Long eventId,
            @RequestParam Long participantId,
            @RequestHeader(defaultValue = "ru") String language) {
        log.info("Participant {} leaving event {}", participantId, eventId);
        return ResponseEntity.ok(eventsService.leaveEvent(eventId, participantId, language));
    }
}