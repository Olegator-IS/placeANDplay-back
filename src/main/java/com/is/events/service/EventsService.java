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
                          String requestId,long executionTime,long currentTime,String lang) {
        if (event.getDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(returnTextToUserByLang(lang,"date_past_error"));
        }
//        logger.logRequestDetails(HttpStatus.OK,currentTime,method,url,requestId,clientIp,executionTime,loginRequest,response);
        return eventsRepository.save(event);
    }

    public Event joinEvent(Long eventId,Long userId,String userName,String lang){
        Event event = eventsRepository.findEventByEventId(eventId);
        if(event == null){
            throw new IllegalArgumentException(returnTextToUserByLang(lang,"event_not_found"));
        }
        List<CurrentParticipants> participants = event.getCurrentParticipants();

        boolean isAlreadyJoined = participants.stream().anyMatch(p -> p.getParticipantId().equals(userId));
        if(isAlreadyJoined){
            throw new IllegalArgumentException(returnTextToUserByLang(lang,"user_already_joined"));
        }

        if(!event.getStatus().equalsIgnoreCase("OPEN")){
            throw new IllegalArgumentException(returnTextToUserByLang(lang,"event_is_not_available"));
        }

        if (event.getDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(returnTextToUserByLang(lang,"event_already_expired"));
        }

        participants.add(new CurrentParticipants(userId,userName));
        event.setCurrentParticipants(participants);
        return eventsRepository.save(event);
    }

    public Event getEventById(long eventId) {
        return eventsRepository.findEventByEventId(eventId);
    }


    public Event eventAction(Long eventId,Long userId,String action,String lang){
        Event event = eventsRepository.findEventByEventId(eventId);
        if(event == null){
            throw new IllegalArgumentException(returnTextToUserByLang(lang,"event_not_found"));
        }
        if(!Objects.equals(event.getOrganizerEvent().getOrganizerId(), userId)){
            throw new IllegalArgumentException(returnTextToUserByLang(lang,"not_allowed")); // Смена состояния,если не организатор ивента
        }

        List<String> actions = new ArrayList<>();
        actions.add("OPEN");
        actions.add("CANCELLED");
        actions.add("ONGOING");
        actions.add("COMPLETED");
        actions.add("EXPIRED");


            if(!actions.contains(action)){
                throw new IllegalArgumentException(returnTextToUserByLang(lang,"action_not_available"));
            }


        switch(event.getStatus().toUpperCase()){
            case "CANCELLED":
//                throw new IllegalArgumentException("Event was already cancelled");
                throw new IllegalArgumentException(returnTextToUserByLang(lang,"event_already_cancelled"));
            case "COMPLETED":
//                throw new IllegalArgumentException("Event was already completed");
                throw new IllegalArgumentException(returnTextToUserByLang(lang,"event_already_completed"));
            case "EXPIRED":
//                throw new IllegalArgumentException("Event was already expired");
                throw new IllegalArgumentException(returnTextToUserByLang(lang,"event_already_expired"));
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

    private String returnTextToUserByLang(String lang, String action) {
        return switch (lang + "_" + action) {
            case "ru_event_not_found" -> "Событие не найдено в системе! Попробуйте еще раз!";
            case "uz_event_not_found" -> "Tizimda hodisa topilmadi. Qayta urinib ko'ring!";
            case "en_event_not_found" -> "Event not found in the system! Try again!";

            case "ru_user_already_joined" -> "Вы уже участник данного события, повторное присоединение невозможно!";
            case "uz_user_already_joined" -> "Siz allaqachon ishtirokchisiz; qayta qo'shilish mumkin emas!";
            case "en_user_already_joined" -> "You are already a participant in this event; re-joining is not possible!";

            case "ru_event_is_not_available" -> "Это мероприятие недоступно для участия, пожалуйста, выберите другое мероприятие!";
            case "uz_event_is_not_available" -> "Ushbu tadbir ishtirok etish uchun mavjud emas, iltimos, boshqa tadbirni tanlang!";
            case "en_event_is_not_available" -> "This event is not available to join, please wait for a new event.";

            case "ru_event_already_expired" -> "Событие было просрочено по времени.";
            case "uz_event_already_expired" -> "Tadbir muddati o'tib ketgan edi.";
            case "en_event_already_expired" -> "The event was overdue.";

            case "ru_event_already_cancelled" -> "Событие уже отменено!";
            case "uz_event_already_cancelled" -> "Hodisa allaqachon bekor qilingan!";
            case "en_event_already_cancelled" -> "Event already cancelled!";

            case "ru_event_already_completed" -> "Событие уже завершено!";
            case "uz_event_already_completed" -> "Hodisa allaqachon tugallangan!";
            case "en_event_already_completed" -> "Event already completed!";

            case "ru_date_past_error" -> "Дата события не может быть в прошлом";
            case "uz_date_past_error" -> "Tadbir sanasi o'tmishda bo'lishi mumkin emas";
            case "en_date_past_error" -> "The date of the event cannot be in the past";

            case "ru_not_allowed" -> "Вы не имеете права выполнять это действие!";
            case "uz_not_allowed" -> "Siz ushbu harakatni bajarishga ruxsatingiz yo'q!";
            case "en_not_allowed" -> "You are not allowed to perform this action.";

            case "ru_action_not_available" -> "Действие недоступно!";
            case "uz_action_not_available" -> "Harakat mavjud emas!";
            case "en_action_not_available" -> "This action is not available!";

            default -> throw new IllegalArgumentException("Unsupported language/action: " + lang + "_" + action);
        };
    }

}
