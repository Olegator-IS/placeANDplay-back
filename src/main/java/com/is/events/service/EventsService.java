package com.is.events.service;

import com.is.events.dto.EventDTO;
import com.is.events.dto.OrganizerDTO;
import com.is.events.dto.ParticipantDTO;
import com.is.events.exception.EventNotFoundException;
import com.is.events.exception.EventValidationException;
import com.is.events.model.CurrentParticipants;
import com.is.events.model.Event;
import com.is.events.model.enums.EventStatus;
import com.is.events.repository.EventsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventsService {

    private final EventsRepository eventsRepository;
    private final LocalizationService localizationService;
    private final UserProfileService userProfileService;
    private final WebSocketService webSocketService;

//    @Autowired
//    private Logger logger;

    @Cacheable(value = "events", key = "#placeId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<EventDTO> getAllEvents(long placeId, Pageable pageable) {
        log.info("Fetching events for placeId: {} with pagination: {}", placeId, pageable);
        return eventsRepository.findAllByPlaceId(placeId, pageable)
                .map(this::convertToDTO);
    }

    @Cacheable(value = "eventsByCity", key = "#placeId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<EventDTO> getAllEventsByCity(long placeId, Pageable pageable) {
        log.info("Fetching events by city for placeId: {} with pagination: {}", placeId, pageable);
        return eventsRepository.findAllByPlaceId(placeId, pageable)
                .map(this::convertToDTO);
    }

    public EventDTO convertToDTO(Event event) {
        EventDTO dto = new EventDTO();
        dto.setEventId(event.getEventId());
        dto.setTitle(event.getSportEvent().getSportName());
        dto.setDescription(event.getDescription());
        dto.setDateTime(event.getDateTime());
        dto.setStatus(event.getStatus());
        dto.setPlaceId(event.getPlaceId());
        
        // Конвертация организатора
        if (event.getOrganizerEvent() != null) {
            OrganizerDTO organizerDTO = new OrganizerDTO();
            organizerDTO.setOrganizerId(event.getOrganizerEvent().getOrganizerId());
            organizerDTO.setName(event.getOrganizerEvent().getOrganizerName());
            organizerDTO.setEmail(event.getOrganizerEvent().getEmail());
            organizerDTO.setPhoneNumber(event.getOrganizerEvent().getPhoneNumber());
            
            // Добавляем URL профильного изображения организатора
            try {
                String profilePictureUrl = userProfileService.getProfilePictureUrl(event.getOrganizerEvent().getOrganizerId());
                organizerDTO.setProfilePictureUrl(profilePictureUrl);
            } catch (Exception e) {
                log.warn("Could not fetch profile picture for organizer {}", event.getOrganizerEvent().getOrganizerId());
            }
            
            dto.setOrganizer(organizerDTO);
        }
        
        // Конвертация участников
        if (event.getCurrentParticipants() != null) {
            List<ParticipantDTO> participantDTOs = event.getCurrentParticipants().getParticipants().stream()
                .map(participant -> {
                    String profilePictureUrl = null;
                    try {
                        profilePictureUrl = userProfileService.getProfilePictureUrl(participant.getParticipantId());
                    } catch (Exception e) {
                        log.warn("Could not fetch profile picture for participant {}", participant.getParticipantId());
                    }
                    return new ParticipantDTO(
                        participant.getParticipantId(),
                        participant.getParticipantName(),
                        participant.getJoinedAt(),
                        profilePictureUrl
                    );
                })
                .toList();
            dto.setParticipants(participantDTOs);
            dto.setParticipantsCount(event.getCurrentParticipants().getSize());
        } else {
            dto.setParticipants(new ArrayList<>());
            dto.setParticipantsCount(0);
        }
        
        // Установка дополнительных полей
        dto.setJoinable(EventStatus.OPEN.name().equals(event.getStatus()));
        dto.setMaxParticipants(event.getSportEvent().getMaxParticipants());
        dto.setEventType(event.getSportEvent().getSportType());
        dto.setLocation(event.getSportEvent().getLocation());
        dto.setPrice(event.getSportEvent().getPrice());

        return dto;
    }

    @Transactional(readOnly = true)
    public Page<Event> findAll(Specification<Event> spec, Pageable pageable) {
        return eventsRepository.findAll(spec, pageable);
    }

    @Transactional
    public EventDTO addEvent(Event event, String lang) {
        validateEventDate(event.getDateTime(), lang);
        // Устанавливаем начальный статус OPEN для нового события
        event.setStatus(EventStatus.OPEN.name());
        log.info("Creating new event: {}", event);
        Event savedEvent = eventsRepository.save(event);
        webSocketService.notifyEventUpdate(event.getPlaceId());
        return convertToDTO(savedEvent);
    }

    @Transactional
    public EventDTO joinEvent(Long eventId, Long userId, String userName, String lang) {
        Event event = findAndValidateEvent(eventId, lang);
        validateEventJoinability(event, userId, lang);
        
        try {
            log.info("Adding participant userId: {}, userName: {} to event: {}", userId, userName, eventId);
            
            if (event.getCurrentParticipants() == null) {
                event.setCurrentParticipants(new CurrentParticipants());
            }
            
            event.getCurrentParticipants().addParticipant(userId, userName);
            event.getCurrentParticipants().setSize(event.getCurrentParticipants().getParticipants().size());
            
            log.info("Current participants state: {}", event.getCurrentParticipants());
            Event updatedEvent = eventsRepository.save(event);
            
            try {
                webSocketService.notifyEventUpdate(event.getPlaceId());
            } catch (Exception e) {
                log.error("Failed to send WebSocket notification for event {}: {}", eventId, e.getMessage());
            }
            
            log.info("Successfully added participant to event: {}", eventId);
            return convertToDTO(updatedEvent);
        } catch (Exception e) {
            log.error("Error while adding participant to event: {}", eventId, e);
            throw new RuntimeException("Error while adding participant: " + e.getMessage());
        }
    }

    public EventDTO getEventById(long eventId, String lang) {
        Event event = findAndValidateEvent(eventId, lang);
        return convertToDTO(event);
    }

    @Transactional
    public Event eventAction(Long eventId, Long userId, String action, String lang) {
        Event event = findAndValidateEvent(eventId, lang);
        validateEventOrganizer(event, userId, lang);
        
        EventStatus newStatus = validateAndGetEventStatus(action, lang);
        EventStatus currentStatus = EventStatus.valueOf(event.getStatus().toUpperCase());
        
        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new EventValidationException("status_transition_error", 
                String.format("Cannot transition from %s to %s", currentStatus, newStatus));
        }

        event.setStatus(newStatus.name());
        log.info("Event {} status changed to {} by organizer {}", eventId, newStatus, userId);
        Event savedEvent = eventsRepository.save(event);
        
        try {
            webSocketService.notifyEventUpdate(event.getPlaceId());
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification for event status change {}: {}", eventId, e.getMessage());
        }
        
        return savedEvent;
    }

    @Transactional
    public List<Event> getEventsForToday(LocalDate today) {
        try {
            log.info("Processing events for today: {}", today);
            List<Event> events = eventsRepository.findOpenEventsForToday(today);
            LocalDateTime now = LocalDateTime.now();
            
            events.stream()
                .filter(event -> event.getDateTime() != null && event.getDateTime().isBefore(now))
                .forEach(event -> {
                    try {
                        log.info("Marking event {} as EXPIRED because its datetime {} is before now {}", 
                            event.getEventId(), event.getDateTime(), now);
                        event.setStatus(EventStatus.EXPIRED.name());
                        eventsRepository.save(event);
                    } catch (Exception e) {
                        log.error("Error updating event {} status to EXPIRED", event.getEventId(), e);
                    }
                });
            
            log.info("Finished processing {} events for today", events.size());
            return events;
        } catch (Exception e) {
            log.error("Error processing events for today", e);
            throw e;
        }
    }

    @Transactional
    public EventDTO leaveEvent(Long eventId, Long participantId, String lang) {
        Event event = findAndValidateEvent(eventId, lang);
        validateEventLeaving(event, participantId, lang);
        
        try {
            log.info("Removing participant {} from event {}", participantId, eventId);
            
            if (event.getCurrentParticipants() == null || 
                !event.getCurrentParticipants().hasParticipant(participantId)) {
                throw new EventValidationException("not_participant",
                    returnTextToUserByLang(lang, "not_participant"));
            }
            
            event.getCurrentParticipants().removeParticipant(participantId);
            
            Event updatedEvent = eventsRepository.save(event);
            
            try {
                webSocketService.notifyEventUpdate(event.getPlaceId());
            } catch (Exception e) {
                log.error("Failed to send WebSocket notification for participant leaving event {}: {}", eventId, e.getMessage());
            }
            
            log.info("Successfully removed participant from event: {}", eventId);
            return convertToDTO(updatedEvent);
        } catch (EventValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error while removing participant from event: {}", eventId, e);
            throw new RuntimeException("Error while removing participant: " + e.getMessage());
        }
    }

    private Event findAndValidateEvent(Long eventId, String lang) {
        Event event = eventsRepository.findEventByEventId(eventId);
        if (event == null) {
            throw new EventNotFoundException(lang);
        }
        return event;
    }

    private void validateEventDate(LocalDateTime dateTime, String lang) {
        if (dateTime.isBefore(LocalDateTime.now())) {
            throw new EventValidationException("date_past_error",
                    returnTextToUserByLang(lang, "date_past_error"));
        }


    }

    private void validateEventJoinability(Event event, Long userId, String lang) {
        if (!event.getStatus().equalsIgnoreCase(EventStatus.OPEN.name())) {
            throw new EventValidationException("event_is_not_available", 
                returnTextToUserByLang(lang, "event_is_not_available"));
        }

        if (event.getDateTime().isBefore(LocalDateTime.now())) {
            throw new EventValidationException("event_already_expired", 
                returnTextToUserByLang(lang, "event_already_expired"));
        }

        if (event.getCurrentParticipants() != null && 
            event.getCurrentParticipants().hasParticipant(userId)) {
            throw new EventValidationException("user_already_joined", 
                returnTextToUserByLang(lang, "user_already_joined"));
        }

        if (event.getCurrentParticipants() != null && 
            event.getCurrentParticipants().getSize() >= event.getSportEvent().getMaxParticipants()) {
            throw new EventValidationException("event_is_full", 
                returnTextToUserByLang(lang, "event_is_full"));
        }
    }

    private void validateEventOrganizer(Event event, Long userId, String lang) {
        if (!Objects.equals(event.getOrganizerEvent().getOrganizerId(), userId)) {
            throw new EventValidationException("not_allowed", 
                returnTextToUserByLang(lang, "not_allowed"));
        }
    }

    private EventStatus validateAndGetEventStatus(String action, String lang) {
        if (!EventStatus.isValid(action)) {
            throw new EventValidationException("action_not_available", 
                returnTextToUserByLang(lang, "action_not_available"));
        }
        return EventStatus.valueOf(action.toUpperCase());
    }

    private void validateEventLeaving(Event event, Long participantId, String lang) {
        // Проверяем, является ли участник организатором
        boolean isOrganizer = Objects.equals(event.getOrganizerEvent().getOrganizerId(), participantId);

        if (isOrganizer) {
            // Организатор может выйти только если событие отменено
            if (!event.getStatus().equalsIgnoreCase(EventStatus.CANCELLED.name())) {
                throw new EventValidationException("organizer_must_cancel_first",
                    returnTextToUserByLang(lang, "organizer_must_cancel_first"));
            }
        } else {
            // Обычный участник может выйти только если событие открыто
            if (!event.getStatus().equalsIgnoreCase(EventStatus.OPEN.name())) {
                throw new EventValidationException("event_not_open_for_leaving",
                    returnTextToUserByLang(lang, "event_not_open_for_leaving"));
            }
        }

        if (event.getCurrentParticipants() == null ||
            !event.getCurrentParticipants().hasParticipant(participantId)) {
            throw new EventValidationException("not_participant",
                returnTextToUserByLang(lang, "not_participant"));
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

            case "ru_event_is_full" -> "Это мероприятие уже заполнено!";
            case "uz_event_is_full" -> "Ushbu tadbir allaqachon to'ldirilgan!";
            case "en_event_is_full" -> "This event is already full!";

            case "ru_not_participant" -> "Вы не являетесь участником этого события!";
            case "uz_not_participant" -> "Siz bu tadbirning ishtirokchisi emassiz!";
            case "en_not_participant" -> "You are not a participant of this event!";

            case "ru_event_not_open_for_leaving" -> "Вы не можете покинуть это событие, так как оно уже не открыто!";
            case "uz_event_not_open_for_leaving" -> "Tadbir ochiq bo'lmaganligi sababli uni tark eta olmaysiz!";
            case "en_event_not_open_for_leaving" -> "You cannot leave this event as it is no longer open!";

            case "ru_organizer_must_cancel_first" -> "Организатор должен сначала отменить событие, прежде чем выйти из него!";
            case "uz_organizer_must_cancel_first" -> "Tashkilotchi avval tadbirni bekor qilishi kerak, keyin undan chiqishi mumkin!";
            case "en_organizer_must_cancel_first" -> "The organizer must cancel the event first before leaving it!";

            default -> throw new IllegalArgumentException("Unsupported language/action: " + lang + "_" + action);
        };
    }

    public List<EventDTO> getLastCompletedEventsForUser(Long userId) {
        log.info("Getting last completed events for user {}", userId);
        
        List<Event> events = eventsRepository.findLastThreeCompletedEventsByUser(userId);
        
        return events.stream()
                .map(this::convertToDTO)
                .toList();
    }
    public List<EventDTO> getUserActivityEvents(Long userId) {
        log.info("Getting all user's activity events {}", userId);

        List<Event> events = eventsRepository.findAllActivityByUser(userId);

        return events.stream()
                .map(this::convertToDTO)
                .toList();
    }



}
