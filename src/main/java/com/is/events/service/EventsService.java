package com.is.events.service;

import com.is.events.model.CurrentParticipants;
import com.is.events.model.Event;
import com.is.events.repository.EventsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventsService {

    private final EventsRepository eventsRepository;

//    @Autowired
//    private Logger logger;

    public List<Event> getAllEvents(long placeId) {
        return eventsRepository.findAll();
    }

    public List<Event> getAllEventsByCity(long placeId) {
        return eventsRepository.findAllByPlaceId(placeId);
    }

    public Event addEvent(Event event,String accessToken,String refreshToken,String clientIp,String url,String method,
                          String requestId,long executionTime,long currentTime) {
        if (event.getDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Дата события не может быть в прошлом");
        }
//        logger.logRequestDetails(HttpStatus.OK,currentTime,method,url,requestId,clientIp,executionTime,loginRequest,response);
        return eventsRepository.save(event);
    }

    public Event joinEvent(Long eventId,Long userId,String userName){
        Event event = eventsRepository.findEventByEventId(eventId);
        if(event == null){
            throw new IllegalArgumentException("Event not found");
        }
        List<CurrentParticipants> participants = event.getCurrentParticipants();

        boolean isAlreadyJoined = participants.stream().anyMatch(p -> p.getParticipantId().equals(userId));
        if(isAlreadyJoined){
            throw new IllegalArgumentException("User already joined");
        }

        if(!event.getStatus().equalsIgnoreCase("OPEN")){
            throw new IllegalArgumentException("This event is not available to join,please wait for new event");
        }

        if (event.getDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("This event was expired");
        }

        participants.add(new CurrentParticipants(userId,userName));

        event.setCurrentParticipants(participants);

        log.info("User has joined the event -> name {}, userId {}",userName,userId);
        return eventsRepository.save(event);
    }

    public Event getEventById(long eventId) {
        return eventsRepository.findEventByEventId(eventId);
    }


    public Event eventAction(Long eventId,Long userId,String action){
        Event event = eventsRepository.findEventByEventId(eventId);
        if(event == null){
            throw new IllegalArgumentException("Event not found");
        }
        if(!Objects.equals(event.getOrganizerEvent().getOrganizerId(), userId)){
            throw new IllegalArgumentException("You are not allowed to perform this action");
        }

        List<String> actions = new ArrayList<>();
        actions.add("OPEN");
        actions.add("CANCELLED");
        actions.add("ONGOING");
        actions.add("COMPLETED");
        actions.add("EXPIRED");


            if(!actions.contains(action)){
                throw new IllegalArgumentException("This action is not available");
            }


        switch(event.getStatus().toUpperCase()){
            case "CANCELLED":
                throw new IllegalArgumentException("Event was already cancelled");
            case "COMPLETED":
                throw new IllegalArgumentException("Event was already completed");
            case "EXPIRED":
                throw new IllegalArgumentException("Event was already expired");
        }
                /*
        Разрешенные ивенты
        OPEN - Ивент открыт для регистрации участников
        CANCELLED - Организатор отменил ивент.
        ONGOING - Ивент начался
        COMPLETED - Ивент завершён
                */
        event.setStatus(action);
        log.info("Organizer has changed event status to {}",action);
        return eventsRepository.save(event);
    }

    public void getEventsForToday(LocalDate today) {
        List<Event> events = eventsRepository.findOpenEventsForToday(today);


        for (Event event : events) {
            System.out.println(event.getDateTime());
            if (event.getDateTime().isBefore(LocalDateTime.now())) {
                event.setStatus("EXPIRED");
                eventsRepository.save(event);
            }
        }
    }

}
