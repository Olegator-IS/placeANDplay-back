package com.is.events.service;

import com.is.events.dto.EventDTO;
import com.is.events.dto.OrganizerDTO;
import com.is.events.dto.ParticipantDTO;
import com.is.events.exception.EventNotFoundException;
import com.is.events.exception.EventValidationException;
import com.is.events.model.CurrentParticipants;
import com.is.events.model.Event;
import com.is.events.model.UserActivityTracking;
import com.is.events.model.enums.EventStatus;
import com.is.events.model.enums.EventMessageType;
import com.is.events.repository.EventsRepository;
import com.is.events.repository.UserActivityTrackingRepository;
import com.is.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

import com.is.events.dto.EventAvailabilityDTO;
import com.is.events.dto.EventCreationAvailabilityResponse;
import com.is.events.dto.EventStatusUpdateRequest;

import com.is.events.service.EventMessageService;
import com.is.events.dto.UserEventStatisticsDTO;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventsService {

    private final EventsRepository eventsRepository;
    private final UserActivityTrackingRepository userActivityTrackingRepository;
    private final LocalizationService localizationService;
    private final UserProfileService userProfileService;
    private final WebSocketService webSocketService;
    private final UserRepository userRepository;
    private final EventMessageService eventMessageService;

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
        dto.setStatus(event.getStatus().name());
        dto.setPlaceId(event.getPlaceId());

        // Проверяем, есть ли запись об активности пользователя
        if (event.getOrganizerEvent() != null) {
            UserActivityTracking userActivity = userActivityTrackingRepository
                    .findByUserId(event.getOrganizerEvent().getOrganizerId())
                    .orElse(null);
            dto.setFirstEventCreation(userActivity != null && userActivity.isFirstEventCreation());
        }

        // Конвертация организатора
        if (event.getOrganizerEvent() != null) {
            OrganizerDTO organizerDTO = new OrganizerDTO();
            organizerDTO.setOrganizerId(event.getOrganizerEvent().getOrganizerId());
            organizerDTO.setName(event.getOrganizerEvent().getOrganizerName());
            organizerDTO.setEmail(event.getOrganizerEvent().getEmail());
            organizerDTO.setPhoneNumber(event.getOrganizerEvent().getPhoneNumber());
            organizerDTO.setOrganizationType("INDIVIDUAL");
            organizerDTO.setRating(4.8);

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
        dto.setJoinable(event.getStatus() == EventStatus.PENDING_APPROVAL);
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
        log.info("Creating new event: {}", event);

        Long organizerId = event.getOrganizerEvent().getOrganizerId();
        if (!userRepository.existsById(organizerId)) {
            throw new EventValidationException("user_not_found",
                    returnTextToUserByLang(lang, "user_not_found"));
        }

        boolean isFirstEventCreation = !userActivityTrackingRepository.existsByUserId(organizerId);

        if (isFirstEventCreation) {
            UserActivityTracking userActivity = new UserActivityTracking();
            userActivity.setUserId(organizerId);
            userActivity.setFirstEventCreation(true);
            userActivity.setCreatedAt(LocalDateTime.now());
            userActivity.setUpdatedAt(LocalDateTime.now());
            userActivityTrackingRepository.save(userActivity);
            log.info("Created first event tracking for user {}", organizerId);
        }

        event.setFirstTimeEventCreation(isFirstEventCreation);
        event.setStatus(EventStatus.PENDING_APPROVAL);
        Event savedEvent = eventsRepository.save(event);

        // Отправляем системное сообщение о создании ивента
        eventMessageService.sendEventMessage(savedEvent, EventMessageType.EVENT_CREATED, null,lang);

        // Отправляем уведомление через WebSocket
        webSocketService.notifyEventUpdate(event.getPlaceId());
        webSocketService.sendEventUpdate(convertToDTO(savedEvent));

        EventDTO resultDto = convertToDTO(savedEvent);
        resultDto.setFirstEventCreation(isFirstEventCreation);
        return resultDto;
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

            // Отправляем системное сообщение о присоединении участника
            eventMessageService.sendEventMessage(updatedEvent, EventMessageType.PARTICIPANT_JOINED, userName,lang);

            // Отправляем уведомление через WebSocket
            webSocketService.notifyEventUpdate(event.getPlaceId());
            webSocketService.sendEventUpdate(convertToDTO(updatedEvent));

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
        EventStatus currentStatus = event.getStatus();

        // Проверка времени для перехода в IN_PROGRESS
        if (newStatus == EventStatus.IN_PROGRESS) {
            LocalDateTime now = LocalDateTime.now();
            if (event.getDateTime().isAfter(now)) {
                throw new EventValidationException(
                    "event_time_not_reached",
                    String.format("Cannot start event before its scheduled time. Event time: %s, Current time: %s",
                        event.getDateTime(), now)
                );
            }
        }

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new EventValidationException("status_transition_error",
                    String.format("Cannot transition from %s to %s", currentStatus, newStatus));
        }

        event.setStatus(newStatus);
        log.info("Event {} status changed to {} by organizer {}", eventId, newStatus, userId);
        Event savedEvent = eventsRepository.save(event);

        // Отправляем системное сообщение о смене статуса
        if (newStatus == EventStatus.IN_PROGRESS) {
            eventMessageService.sendEventMessage(savedEvent, EventMessageType.EVENT_STARTED, null,lang);
        } else {
            eventMessageService.sendEventMessage(savedEvent, EventMessageType.STATUS_CHANGED, null,lang);
        }

        // Отправляем уведомление через WebSocket
        webSocketService.notifyEventUpdate(event.getPlaceId());
        webSocketService.sendEventUpdate(convertToDTO(savedEvent));

        return savedEvent;
    }

    @Transactional
    public List<Event> getEventsForToday(LocalDate today) {
        try {
            log.info("Processing events for today: {}", today);
            List<Event> events = eventsRepository.findOpenEventsForToday(EventStatus.PENDING_APPROVAL, today);
            LocalDateTime now = LocalDateTime.now();

            events.stream()
                    .filter(event -> event.getDateTime() != null && event.getDateTime().isBefore(now))
                    .forEach(event -> {
                        try {
                            log.info("Marking event {} as EXPIRED because its datetime {} is before now {}",
                                    event.getEventId(), event.getDateTime(), now);
                            event.setStatus(EventStatus.EXPIRED);
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

            String participantName = event.getCurrentParticipants().getParticipantName(participantId);
            event.getCurrentParticipants().removeParticipant(participantId);

            Event updatedEvent = eventsRepository.save(event);

            // Отправляем системное сообщение о выходе участника
            eventMessageService.sendEventMessage(updatedEvent, EventMessageType.PARTICIPANT_LEFT, participantName,lang);

            // Отправляем уведомление через WebSocket
            webSocketService.notifyEventUpdate(event.getPlaceId());
            webSocketService.sendEventUpdate(convertToDTO(updatedEvent));

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
        if (event.getStatus() != EventStatus.PENDING_APPROVAL) {
            if (event.getStatus() == EventStatus.IN_PROGRESS) {
                throw new EventValidationException("event_in_progress",
                        returnTextToUserByLang(lang, "event_in_progress"));
            }

        }

        if(event.getStatus() == EventStatus.IN_PROGRESS){
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
        boolean isOrganizer = Objects.equals(event.getOrganizerEvent().getOrganizerId(), participantId);
        System.out.println(event.getStatus());

        if (isOrganizer) {
            if (event.getStatus() != EventStatus.CANCELLED) {
                throw new EventValidationException("organizer_must_cancel_first",
                        returnTextToUserByLang(lang, "organizer_must_cancel_first"));
            }
        } else {
            if (event.getStatus() == EventStatus.IN_PROGRESS) {
                throw new EventValidationException("cannot_leave_in_progress",
                        returnTextToUserByLang(lang, "cannot_leave_in_progress"));
            }

            if (event.getStatus() == EventStatus.CONFIRMED) {
                LocalDateTime now = LocalDateTime.now();
                Duration timeUntilEvent = Duration.between(now, event.getDateTime());
                if (timeUntilEvent.toHours() < 2) {
                    throw new EventValidationException("cannot_leave_before_start",
                            returnTextToUserByLang(lang, "cannot_leave_before_start"));
                }
            }

//            if (event.getStatus() != EventStatus.PENDING_APPROVAL ||
//                event.getStatus() != EventStatus.CONFIRMED) {
//                throw new EventValidationException("event_not_open_for_leaving",
//                        returnTextToUserByLang(lang, "event_not_open_for_leaving"));
//            }
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

            case "ru_user_not_found" -> "Пользователь не найден в системе!";
            case "uz_user_not_found" -> "Foydalanuvchi tizimda topilmadi!";
            case "en_user_not_found" -> "User not found in the system!";

            case "ru_too_many_events_per_day" -> "Вы не можете создать больше 3 событий в один день!";
            case "uz_too_many_events_per_day" -> "Bir kunda 3 tadan ortiq tadbir yarata olmaysiz!";
            case "en_too_many_events_per_day" -> "You cannot create more than 3 events in one day!";

            case "ru_time_too_close" -> "Между событиями должно быть не менее 3 часов!";
            case "uz_time_too_close" -> "Tadbirlar orasida kamida 3 soat bo'lishi kerak!";
            case "en_time_too_close" -> "There must be at least 3 hours between events!";

            case "ru_can_create_event" -> "Вы можете создать событие!";
            case "uz_can_create_event" -> "Siz tadbir yaratishingiz mumkin!";
            case "en_can_create_event" -> "You can create an event!";

            case "ru_event_in_progress" -> "Нельзя присоединиться к событию, которое уже началось!";
            case "uz_event_in_progress" -> "Allaqachon boshlangan tadbirga qo'shilish mumkin emas!";
            case "en_event_in_progress" -> "Cannot join an event that is already in progress.";

            case "ru_cannot_leave_in_progress" -> "Нельзя покинуть событие, которое уже началось!";
            case "uz_cannot_leave_in_progress" -> "Boshlangan tadbirni tark etish mumkin emas!";
            case "en_cannot_leave_in_progress" -> "Cannot leave an event that is in progress.";

            case "ru_cannot_leave_before_start" -> "Нельзя покинуть событие менее чем за 2 часа до его начала!";
            case "uz_cannot_leave_before_start" -> "Tadbirni boshlanishiga 2 soatdan kam vaqt qolganda tark etish mumkin emas!";
            case "en_cannot_leave_before_start" -> "Cannot leave the event less than 2 hours before it starts.";

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

    public List<EventAvailabilityDTO> getEventAvailability(Long placeId, LocalDate startDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = startDate.plusDays(30).atTime(23, 59, 59);

        List<Event> events = eventsRepository.findAll(
            (root, query, cb) -> {
                return cb.and(
                    cb.equal(root.get("placeId"), placeId),
                    cb.equal(root.get("status"), EventStatus.PENDING_APPROVAL),
                    cb.greaterThanOrEqualTo(root.get("dateTime"), startDateTime),
                    cb.lessThanOrEqualTo(root.get("dateTime"), endDateTime)
                );
            }
        );

        Map<LocalDate, Long> eventCountByDate = events.stream()
            .map(event -> event.getDateTime().toLocalDate())
            .collect(Collectors.groupingBy(
                date -> date,
                Collectors.counting()
            ));

        return startDate.datesUntil(startDate.plusDays(30))
            .map(date -> EventAvailabilityDTO.builder()
                .date(date)
                .eventCount(eventCountByDate.getOrDefault(date, 0L).intValue())
                .build())
            .collect(Collectors.toList());
    }

    public EventCreationAvailabilityResponse checkEventCreationAvailability(Long organizerId, LocalDate date, String lang) {
        int eventCount = eventsRepository.countEventsByOrganizerAndDate(organizerId, date);
        
        if (eventCount >= 3) {
            return EventCreationAvailabilityResponse.builder()
                    .available(false)
                    .message(returnTextToUserByLang(lang, "too_many_events_per_day"))
                    .build();
        }
        
        return EventCreationAvailabilityResponse.builder()
                .available(true)
                .message(returnTextToUserByLang(lang, "can_create_event"))
                .build();
    }

    public EventCreationAvailabilityResponse checkEventTimeAvailability(Long organizerId, LocalDate date, LocalDateTime proposedTime, String lang) {
        // First check the daily limit
        int eventCount = eventsRepository.countEventsByOrganizerAndDate(organizerId, date);
        if (eventCount >= 3) {
            return EventCreationAvailabilityResponse.builder()
                    .available(false)
                    .message(returnTextToUserByLang(lang, "too_many_events_per_day"))
                    .build();
        }

        // Then check time conflicts
        List<Event> existingEvents = eventsRepository.findEventsByOrganizerAndDate(organizerId, date);
        for (Event event : existingEvents) {
            Duration timeDifference = Duration.between(event.getDateTime(), proposedTime).abs();
            if (timeDifference.toMinutes() < 180) { // 3 hours minimum difference
                return EventCreationAvailabilityResponse.builder()
                        .available(false)
                        .message(returnTextToUserByLang(lang, "time_too_close"))
                        .build();
            }
        }

        return EventCreationAvailabilityResponse.builder()
                .available(true)
                .message(returnTextToUserByLang(lang, "can_create_event"))
                .build();
    }

    public Event confirmEvent(Long eventId, Long organizationId) {
        Event event = findEventById(eventId);
        validateOrganizationAccess(event, organizationId);
        
        if (!event.getStatus().equals(EventStatus.PENDING_APPROVAL) && 
            !event.getStatus().equals(EventStatus.CHANGES_REQUESTED)) {
            throw new IllegalStateException("Event can only be confirmed when in PENDING_APPROVAL or CHANGES_REQUESTED status");
        }
        
        event.confirm();
        return eventsRepository.save(event);
    }

    public Event rejectEvent(Long eventId, Long organizationId, EventStatusUpdateRequest request) {
        Event event = findEventById(eventId);
        validateOrganizationAccess(event, organizationId);
        
        if (!event.getStatus().equals(EventStatus.PENDING_APPROVAL) && 
            !event.getStatus().equals(EventStatus.CHANGES_REQUESTED)) {
            throw new IllegalStateException("Event can only be rejected when in PENDING_APPROVAL or CHANGES_REQUESTED status");
        }
        
        event.reject(request.getReason(), organizationId);
        return eventsRepository.save(event);
    }

    public Event requestEventChanges(Long eventId, Long organizationId, EventStatusUpdateRequest request) {
        Event event = findEventById(eventId);
        validateOrganizationAccess(event, organizationId);
        
        if (!event.getStatus().equals(EventStatus.PENDING_APPROVAL)) {
            throw new IllegalStateException("Changes can only be requested for events in PENDING_APPROVAL status");
        }
        
        event.requestChanges(request.getRequestedChanges(), organizationId);
        return eventsRepository.save(event);
    }

    public Event startEvent(Long eventId) {
        Event event = findEventById(eventId);
        event.startEvent();
        return eventsRepository.save(event);
    }

    public Event completeEvent(Long eventId) {
        Event event = findEventById(eventId);
        event.complete();
        return eventsRepository.save(event);
    }

    public Event cancelEvent(Long eventId, Long organizationId) {
        Event event = findEventById(eventId);
        validateOrganizationAccess(event, organizationId);
        event.cancel();
        return eventsRepository.save(event);
    }

    private void validateOrganizationAccess(Event event, Long organizationId) {
        if (!event.getPlaceId().equals(organizationId)) {
            throw new AccessDeniedException("Organization does not have access to this event");
        }
    }

    private Event findEventById(Long eventId) {
        return eventsRepository.findEventByEventId(eventId);
    }

    // Scheduled task to check and update event statuses
    @Scheduled(cron = "0 */5 * * * *") // Runs every 5 minutes
    public void updateEventStatuses() {
        LocalDateTime now = LocalDateTime.now();
        
        // Find confirmed events that should be started
        List<Event> confirmedEvents = eventsRepository.findByStatusAndDateTimeBefore(
            EventStatus.CONFIRMED, now);
        confirmedEvents.forEach(event -> {
            try {
                // Дополнительная проверка времени для большей точности
                if (!event.getDateTime().isAfter(now)) {
                    event.startEvent();
                    eventsRepository.save(event);
                    log.info("Event {} automatically started at {}. Event time was: {}", 
                        event.getEventId(), now, event.getDateTime());
                }
            } catch (Exception e) {
                log.error("Error starting event {}: {}", event.getEventId(), e.getMessage());
            }
        });
    }

    public UserEventStatisticsDTO getUserEventStatistics(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EventValidationException("user_not_found",
                    returnTextToUserByLang("en", "user_not_found"));
        }

        int eventsAsParticipant = eventsRepository.countEventsWhereUserIsParticipant(userId);
        int eventsAsOrganizer = eventsRepository.countEventsWhereUserIsOrganizer(userId);
        int totalEvents = eventsRepository.countAllUserEvents(userId);

        return UserEventStatisticsDTO.builder()
                .totalEvents(totalEvents)
                .eventsAsOrganizer(eventsAsOrganizer)
                .eventsAsParticipant(eventsAsParticipant)
                .build();
    }
}