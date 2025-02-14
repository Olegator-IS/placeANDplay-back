package com.is.events.api;

import com.is.events.model.Event;
import com.is.events.model.EventResponse;
import com.is.events.service.EventsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    public ResponseEntity<EventResponse> getEventByEventId(@RequestParam long eventId,
                                                   @RequestParam long userId) {
        Event events = eventsService.getEventById(eventId);
        boolean letTheUserJoin = false;
        // Проверяем наличие нашего юзера в списках участников
        boolean isJoined = events.getCurrentParticipants().stream()
                .anyMatch(p -> p.getParticipantId().equals(userId));

        // Проверяем доступен ли ивент
        boolean isAvailableToJoin = events.getStatus().equals("OPEN");
        // Проверяем на expired
        boolean isExpired = events.getDateTime().isBefore(LocalDateTime.now());


        
        if(!isJoined && isAvailableToJoin && !isExpired) {
            letTheUserJoin =true;
        }

        EventResponse eventResponse = new EventResponse(events,letTheUserJoin);

        return ResponseEntity.ok(eventResponse);
    }

    @PostMapping("/addEvent")
    public ResponseEntity<Event> addEvent(@RequestBody Event event,
                                          @RequestHeader("accessToken") String accessToken,
                                          @RequestHeader("refreshToken") String refreshToken,
                                          @RequestAttribute("clientIp") String clientIp,
                                          @RequestAttribute("url") String url,
                                          @RequestAttribute("method") String method,
                                          @RequestAttribute("Request-Id") String requestId,
                                          @RequestAttribute("startTime") long startTime) {
        long currentTime = System.currentTimeMillis(); // Это необходимо для время выполнения запроса
        long executionTime = currentTime - startTime; // Время выполнения запроса
        Event createdEvent = eventsService.addEvent(event,accessToken,refreshToken,clientIp,url,method,requestId,
                executionTime,currentTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent);
    }

    @GetMapping("/join")
    public ResponseEntity<Event> joinEvent(@RequestParam Long eventId,
                                            @RequestParam Long userId,
                                            @RequestParam String userName) {
        Event createdEvent = eventsService.joinEvent(eventId,userId,userName);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent);
    }

    @GetMapping("/action")
    public ResponseEntity<Event> action(@RequestParam Long eventId,
                                        @RequestParam Long userId,
                                        @RequestParam String action){
        /*
        Разрешенные ивенты
        OPEN - Ивент открыт для регистрации участников
        CANCELLED - Организатор отменил ивент.
        ONGOING - Ивент начался
        COMPLETED - Ивен завершён
         */
        Event eventAction = eventsService.eventAction(eventId,userId,action);
        return ResponseEntity.status(HttpStatus.OK).body(eventAction);
    }

    @Scheduled(fixedRate = 60000) // Каждую минуту
    public void updateExpiredEvents() {
        LocalDate today = LocalDate.now();

        eventsService.getEventsForToday(today);


    }
}