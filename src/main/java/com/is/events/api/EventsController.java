package com.is.events.api;

import com.is.events.dto.*;
import com.is.events.model.Event;
import com.is.events.model.EventFilterDTO;
import com.is.events.model.EventSpecification;
import com.is.events.model.enums.EventStatus;
import com.is.events.service.EventsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @GetMapping("/pagination")
    @Operation(summary = "Get events with filtering and pagination")
    public ResponseEntity<Page<EventDTO>> getEvents(
            @RequestParam(required = false) Long placeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateTime") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        EventFilterDTO filter = new EventFilterDTO();
        filter.setPlaceId(placeId != null ? placeId.toString() : null);
        filter.setDate(date);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortDirection(sortDirection);

        // Создаем объект сортировки
        Sort sort = Sort.by(
                filter.getSortDirection().equalsIgnoreCase("ASC") ?
                        Sort.Direction.ASC : Sort.Direction.DESC,
                filter.getSortBy()
        );

        // Создаем объект пагинации с сортировкой
        PageRequest pageRequest = PageRequest.of(
                filter.getPage(),
                filter.getSize(),
                sort
        );

        // Применяем фильтры и возвращаем результат
        Page<Event> events = eventsService.findAll(
                EventSpecification.withFilters(filter),
                pageRequest
        );

        return ResponseEntity.ok(events.map(eventsService::convertToDTO));
    }

    @GetMapping("/place/{placeId}")
    @Operation(summary = "Get all events for a place with optional date filtering")
    public ResponseEntity<Page<EventDTO>> getAllEvents(
            @PathVariable Long placeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateTime,desc") String[] sort) {
        
        List<Sort.Order> orders = new ArrayList<>();
        if (sort[0].contains(",")) {
            // will sort more than 2 fields
            for (String sortOrder : sort) {
                String[] _sort = sortOrder.split(",");
                orders.add(new Sort.Order(
                    _sort[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
                    _sort[0]));
            }
        } else {
            // sort=[field, direction]
            orders.add(new Sort.Order(
                sort[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
                sort[0]));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(orders));
        return ResponseEntity.ok(eventsService.getAllEvents(placeId, startDate, endDate, pageable));
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

    @GetMapping("/last-completed")
    public ResponseEntity<List<EventDTO>> getLastCompletedEvents(
            @RequestParam Long userId,
            @RequestHeader(value = "language", defaultValue = "ru") String lang) {
        log.info("GET /last-completed request received for user {}", userId);
        return ResponseEntity.ok(eventsService.getLastCompletedEventsForUser(userId));
    }

    @GetMapping("/user-activity-events")
    public ResponseEntity<List<EventDTO>> getUserActivityEvents(
            @RequestParam Long userId,
            @RequestHeader(value = "language", defaultValue = "ru") String lang) {
        log.info("GET /last-completed request received for user {}", userId);
        return ResponseEntity.ok(eventsService.getUserActivityEvents(userId));
    }

    @Operation(summary = "Получить доступность событий на 30 дней")
    @GetMapping("/availability")
    public ResponseEntity<List<EventAvailabilityDTO>> getEventAvailability(
            @RequestParam Long placeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestHeader(defaultValue = "ru") String language) {
        log.info("Getting event availability for place {} from date {}", placeId, startDate);
        return ResponseEntity.ok(eventsService.getEventAvailability(placeId, startDate));
    }

    @GetMapping("/check-availability")
    @Operation(summary = "Check if user can create an event on a specific date")
    public ResponseEntity<EventCreationAvailabilityResponse> checkEventCreationAvailability(
            @RequestParam Long organizerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime proposedTime,
            @RequestHeader(defaultValue = "ru") String language) {
        
        log.info("Checking event creation availability for organizer {} on date {} with proposedTime {}", 
                organizerId, date, proposedTime);

        if (proposedTime == null) {
            return ResponseEntity.ok(eventsService.checkEventCreationAvailability(organizerId, date, language));
        } else {
            return ResponseEntity.ok(eventsService.checkEventTimeAvailability(organizerId, date, proposedTime, language));
        }
    }

    @PutMapping("/{eventId}/confirm")
    @Operation(summary = "Confirm an event")
    public ResponseEntity<Event> confirmEvent(
            @PathVariable Long eventId,
            @RequestParam Long organizationId) {
        return ResponseEntity.ok(eventsService.confirmEvent(eventId, organizationId));
    }

    @PutMapping("/{eventId}/reject")
    @Operation(summary = "Reject an event")
    public ResponseEntity<Event> rejectEvent(
            @PathVariable Long eventId,
            @RequestParam Long organizationId,
            @RequestBody EventStatusUpdateRequest request) {
        return ResponseEntity.ok(eventsService.rejectEvent(eventId, organizationId, request));
    }

    @PutMapping("/{eventId}/request-changes")
    @Operation(summary = "Request changes for an event")
    public ResponseEntity<Event> requestEventChanges(
            @PathVariable Long eventId,
            @RequestParam Long organizationId,
            @RequestBody EventStatusUpdateRequest request) {
        return ResponseEntity.ok(eventsService.requestEventChanges(eventId, organizationId, request));
    }

    @PutMapping("/{eventId}/cancel")
    @Operation(summary = "Cancel an event")
    public ResponseEntity<Event> cancelEvent(
            @PathVariable Long eventId,
            @RequestParam Long organizationId) {
        return ResponseEntity.ok(eventsService.cancelEvent(eventId, organizationId));
    }

    @GetMapping("/user-statistics/{userId}")
    public ResponseEntity<UserEventStatisticsDTO> getUserEventStatistics(@PathVariable Long userId) {
        return ResponseEntity.ok(eventsService.getUserEventStatistics(userId));
    }

    @GetMapping("/check-join-availability")
    @Operation(summary = "Check if user can join an event on a specific date")
    public ResponseEntity<EventJoinAvailabilityResponse> checkEventJoinAvailability(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader(defaultValue = "ru") String language) {
        
        log.info("Checking event join availability for user {} on date {}", userId, date);
        return ResponseEntity.ok(eventsService.checkEventJoinAvailability(userId, date, language));
    }

    @GetMapping("/organization/{placeId}/events")
    @Operation(summary = "Get organization events with status filtering")
    @ApiResponse(responseCode = "200", description = "Events retrieved successfully")
    public ResponseEntity<Page<EventDTO>> getOrganizationEvents(
            @PathVariable Long placeId,
            @RequestParam(required = false) List<EventStatus> statuses,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateTime,desc") String[] sort) {
        
        log.info("Received request for organization events. PlaceId: {}, Statuses: {}, Page: {}, Size: {}, Sort: {}", 
            placeId, statuses, page, size, Arrays.toString(sort));

        List<Sort.Order> orders = new ArrayList<>();
        if (sort[0].contains(",")) {
            for (String sortOrder : sort) {
                String[] _sort = sortOrder.split(",");
                orders.add(new Sort.Order(
                    _sort[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
                    _sort[0]));
            }
        } else {
            orders.add(new Sort.Order(
                sort[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
                sort[0]));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(orders));
        
        Page<EventDTO> events = eventsService.getOrganizationEvents(placeId, statuses, pageable);
        
        return ResponseEntity.ok(events);
    }
}