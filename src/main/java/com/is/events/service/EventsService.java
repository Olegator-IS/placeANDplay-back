package com.is.events.service;

import com.is.auth.service.EmailService;
import com.is.auth.service.PushNotificationService;
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
import com.is.places.model.Place;
import com.is.places.repository.PlaceRepository;
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
import java.util.*;
import java.util.stream.Collectors;

import com.is.events.dto.EventAvailabilityDTO;
import com.is.events.dto.EventCreationAvailabilityResponse;
import com.is.events.dto.EventStatusUpdateRequest;

import com.is.events.service.EventMessageService;
import com.is.events.dto.UserEventStatisticsDTO;
import com.is.events.dto.EventJoinAvailabilityResponse;
import com.is.events.model.EventParticipant;
import com.is.events.dto.NearestEventDTO;
import com.is.events.dto.CheckInEventDTO;
import com.is.auth.repository.UserAdditionalInfoRepository;
import com.is.auth.model.user.UserAdditionalInfo;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventsService {

    private final EventsRepository eventsRepository;
    private final UserActivityTrackingRepository userActivityTrackingRepository;
    private final LocalizationService localizationService;
    private final UserProfileService userProfileService;
    private final WebSocketService webSocketService;
    private final UserRepository userRepository;
    private final EventMessageService eventMessageService;
    private final EmailService emailService;
    private final PlaceRepository placeRepository;
    private final PushNotificationService pushNotificationService;
    private final UserAdditionalInfoRepository userAdditionalInfoRepository;

//    @Autowired
//    private Logger logger;

    @Cacheable(value = "events", key = "#placeId + '_' + #startDate + '_' + #endDate + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort")
    public Page<EventDTO> getAllEvents(long placeId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        log.info("Fetching events for placeId: {} with date range: {} to {}, pagination: {}", 
            placeId, startDate, endDate, pageable);
        
        // Если даты не указаны, используем текущую дату и дату через месяц
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (endDate == null) {
            endDate = startDate.plusMonths(1);
        }
        
        return eventsRepository.findEventsByPlaceAndDateRange(placeId, startDate, endDate, pageable)
                .map(this::convertToDTO);
    }

    // Обновляем старый метод для обратной совместимости
    @Cacheable(value = "events", key = "#placeId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort")
    public Page<EventDTO> getAllEvents(long placeId, Pageable pageable) {
        return getAllEvents(placeId, LocalDate.now(), LocalDate.now().plusMonths(1), pageable);
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
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setDateTime(event.getDateTime());
        dto.setStatus(event.getStatus().name());
        dto.setPlaceId(event.getPlaceId());
        dto.setAdditionalInfo(event.getAdditionalInfo());

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
        if (event.getCurrentParticipants() != null && event.getCurrentParticipants().getParticipants() != null && !event.getCurrentParticipants().getParticipants().isEmpty()) {
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
                                profilePictureUrl,
                                participant.getStatus()
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
        dto.setJoinable(event.getStatus() == EventStatus.OPEN || event.getStatus() == EventStatus.PENDING_APPROVAL || event.getStatus() == EventStatus.CONFIRMED);
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

        // Копируем additionalInfo из sportEvent, если оно есть
        if (event.getSportEvent() != null && event.getSportEvent().getAdditionalInfo() != null) {
            event.setAdditionalInfo(event.getSportEvent().getAdditionalInfo());
        }

        // Явно выставляем size для currentParticipants, если participants не пустой
        if (event.getCurrentParticipants() != null && event.getCurrentParticipants().getParticipants() != null) {
            event.getCurrentParticipants().setSize(event.getCurrentParticipants().getParticipants().size());
        }

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
        event.setStatus(EventStatus.OPEN);
        Event savedEvent = eventsRepository.save(event);

        // Отправляем системное сообщение о создании ивента
        eventMessageService.sendEventMessage(savedEvent, EventMessageType.EVENT_CREATED, null,lang);

        // Отправляем уведомление через WebSocket
        webSocketService.notifyEventUpdate(event.getPlaceId());
        webSocketService.sendEventUpdate(convertToDTO(savedEvent));
        Place getPlace = placeRepository.findPlaceByPlaceId(event.getPlaceId());

        emailService.sendEventCreated(event,lang,getPlace.getAddress(),getPlace.getName());

        // Отправляем уведомления пользователям, у которых этот вид спорта в избранном
        pushNotificationService.sendNewEventNotification(savedEvent);

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
            eventMessageService.sendEventMessage(updatedEvent, EventMessageType.PARTICIPANT_JOINED, userName, lang);

            // Отправляем push-уведомление организатору
            EventParticipant participant = new EventParticipant(updatedEvent, userRepository.getById(userId));
            pushNotificationService.sendParticipantJoinedNotification(updatedEvent, participant);

            // Отправляем уведомление через WebSocket
            webSocketService.notifyEventUpdate(event.getPlaceId());
            webSocketService.sendEventUpdate(convertToDTO(updatedEvent));

            log.info("Successfully added participant to event: {}", eventId);
            return convertToDTO(updatedEvent);

        } catch (Exception e) {
            log.error("Error while adding participant to event: {}", eventId, e);
            throw new EventValidationException("join_error",
                    returnTextToUserByLang(lang, "join_error"));
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
            // Если до начала события больше 30 минут — не разрешаем старт
            if (event.getDateTime().minusMinutes(30).isAfter(now)) {
                throw new EventValidationException(
                    "event_time_too_early",
                    String.format(returnTextToUserByLang(lang, "event_time_too_early"), event.getDateTime(), now)
                );
            }
            // Остальная логика проверки времени старта — только в event.startEvent()
        }

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new EventValidationException("status_transition_error",
                String.format(returnTextToUserByLang(lang, "status_transition_error"), currentStatus, newStatus));
        }

        // Если статус меняется на COMPLETED, используем специальную логику
        if (newStatus == EventStatus.COMPLETED) {
            return completeEvent(eventId);
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

        // Отправляем push-уведомление организатору
        pushNotificationService.sendEventStatusChangeNotification(savedEvent, newStatus);

        // Отправляем уведомление через WebSocket
        webSocketService.notifyEventUpdate(event.getPlaceId());
        webSocketService.sendEventUpdate(convertToDTO(savedEvent));
        Place getPlace = placeRepository.findPlaceByPlaceId(event.getPlaceId());

        emailService.sendEventStatusChangeNotification(event,lang,getPlace.getName(),getPlace.getPhone());
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
            eventMessageService.sendEventMessage(updatedEvent, EventMessageType.PARTICIPANT_LEFT, participantName, lang);

            // Отправляем push-уведомление организатору
            EventParticipant participant = new EventParticipant(updatedEvent, userRepository.getById(participantId));
            pushNotificationService.sendParticipantLeftNotification(updatedEvent, participant);

            // Отправляем уведомление через WebSocket
            webSocketService.notifyEventUpdate(event.getPlaceId());
            webSocketService.sendEventUpdate(convertToDTO(updatedEvent));

            log.info("Successfully removed participant from event: {}", eventId);
            return convertToDTO(updatedEvent);
        } catch (EventValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error while removing participant from event: {}", eventId, e);
            throw new EventValidationException("leave_error",
                    returnTextToUserByLang(lang, "leave_error"));
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

            case "ru_too_many_events_per_day_for_joining" -> "Вы не можете присоединиться к более чем 3 событиям за день!";
            case "uz_too_many_events_per_day_for_joining" -> "Bir kunda 3 tadan ortiq tadbirga qo'shila olmaysiz!";
            case "en_too_many_events_per_day_for_joining" -> "You cannot join more than 3 events per day!";

            case "ru_can_join_event" -> "Вы можете присоединиться к событию";
            case "uz_can_join_event" -> "Tadbirga qo'shilishingiz mumkin";
            case "en_can_join_event" -> "You can join the event";

            case "ru_join_error" -> "Ошибка при присоединении к событию";
            case "uz_join_error" -> "Tadbirga qo'shilishda xatolik yuz berdi";
            case "en_join_error" -> "Error while joining the event";

            case "ru_leave_error" -> "Ошибка при выходе из события";
            case "uz_leave_error" -> "Tadbirlar tark etilganda xatolik yuz berdi";
            case "en_leave_error" -> "Error while leaving the event";

            case "ru_already_checked_in" -> "Вы уже отметились на этом событии";
            case "uz_already_checked_in" -> "Siz allaqachon bu tadbirda belgilangansiz";
            case "en_already_checked_in" -> "You have already checked in for this event";

            case "ru_checkin_not_confirmed" -> "Вы не можете отметиться на этом событии, так как оно еще не подтверждено организатором.";
            case "uz_checkin_not_confirmed" -> "Siz ushbu tadbirda belgilanishingiz mumkin emas, chunki tashkilotchi hali tasdiqlanmagan.";
            case "en_checkin_not_confirmed" -> "You cannot check in for this event as it has not been confirmed by the organizer.";

            case "ru_checkin_time_window" -> "Вы можете отметиться на этом событии только в течение 30 минут до его начала.";
            case "uz_checkin_time_window" -> "Siz ushbu tadbirda belgilanishingiz mumkin emas, chunki 30 daqiqadan oldin boshlanishi kerak.";
            case "en_checkin_time_window" -> "You can only check in for this event within 30 minutes before its start.";

            case "ru_event_time_too_early" -> "Нельзя начать событие раньше, чем за 30 минут до его запланированного времени. Время события: %s, Текущее время: %s";
            case "uz_event_time_too_early" -> "Tadbir rejalashtirilgan vaqtdan 30 daqiqadan oldin boshlanishi mumkin emas. Tadbir vaqti: %s, Joriy vaqt: %s";
            case "en_event_time_too_early" -> "Cannot start event earlier than 30 minutes before its scheduled time. Event time: %s, Current time: %s";

            case "ru_status_transition_error" -> "Нельзя сменить статус с %s на %s";
            case "uz_status_transition_error" -> "Statusni %s dan %s ga o'zgartirib bo'lmaydi";
            case "en_status_transition_error" -> "Cannot transition from %s to %s";

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
                (root, query, cb) -> cb.and(
                        cb.equal(root.get("placeId"), placeId),
                        cb.or(
                                cb.equal(root.get("status"), EventStatus.PENDING_APPROVAL),
                                cb.equal(root.get("status"), EventStatus.OPEN)
                        ),
                        cb.greaterThanOrEqualTo(root.get("dateTime"), startDateTime),
                        cb.lessThanOrEqualTo(root.get("dateTime"), endDateTime)
                )
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
        // Получаем количество событий по ролям
        int eventsAsOrganizer = eventsRepository.countUserEventsAsOrganizerForDate(organizerId, date);
        int eventsAsParticipant = eventsRepository.countUserEventsAsParticipantForDate(organizerId, date);
        int uniqueEvents = eventsRepository.countUserEventsAsBothRolesForDate(organizerId, date);
        
        // Вычисляем общее количество уникальных событий
        // Формула: organizer + participant - unique (чтобы не считать дважды события, где пользователь и организатор, и участник)
        int totalEvents = eventsAsOrganizer + eventsAsParticipant - uniqueEvents;
        
        // Проверяем общее количество уникальных событий
        if (totalEvents >= 3) {
            return EventCreationAvailabilityResponse.builder()
                    .available(false)
                    .message(returnTextToUserByLang(lang, "too_many_events_per_day"))
                    .eventsAsOrganizer(eventsAsOrganizer)
                    .eventsAsParticipant(eventsAsParticipant)
                    .uniqueEvents(uniqueEvents)
                    .totalEvents(totalEvents)
                    .build();
        }
        
        // Дополнительно проверяем количество событий как организатора
        if (eventsAsOrganizer >= 3) {
            return EventCreationAvailabilityResponse.builder()
                    .available(false)
                    .message(returnTextToUserByLang(lang, "too_many_events_per_day"))
                    .eventsAsOrganizer(eventsAsOrganizer)
                    .eventsAsParticipant(eventsAsParticipant)
                    .uniqueEvents(uniqueEvents)
                    .totalEvents(totalEvents)
                    .build();
        }
        
        return EventCreationAvailabilityResponse.builder()
                .available(true)
                .message(returnTextToUserByLang(lang, "can_create_event"))
                .eventsAsOrganizer(eventsAsOrganizer)
                .eventsAsParticipant(eventsAsParticipant)
                .uniqueEvents(uniqueEvents)
                .totalEvents(totalEvents)
                .build();
    }

    public EventCreationAvailabilityResponse checkEventTimeAvailability(Long organizerId, LocalDate date, LocalDateTime proposedTime, String lang) {
        // Получаем количество событий по ролям
        int eventsAsOrganizer = eventsRepository.countUserEventsAsOrganizerForDate(organizerId, date);
        int eventsAsParticipant = eventsRepository.countUserEventsAsParticipantForDate(organizerId, date);
        int uniqueEvents = eventsRepository.countUserEventsAsBothRolesForDate(organizerId, date);
        
        // Вычисляем общее количество уникальных событий
        int totalEvents = eventsAsOrganizer + eventsAsParticipant - uniqueEvents;

        // Проверяем общее количество уникальных событий
        if (totalEvents >= 3) {
            return EventCreationAvailabilityResponse.builder()
                    .available(false)
                    .message(returnTextToUserByLang(lang, "too_many_events_per_day"))
                    .eventsAsOrganizer(eventsAsOrganizer)
                    .eventsAsParticipant(eventsAsParticipant)
                    .uniqueEvents(uniqueEvents)
                    .totalEvents(totalEvents)
                    .build();
        }

        // Дополнительно проверяем количество событий как организатора
        if (eventsAsOrganizer >= 3) {
            return EventCreationAvailabilityResponse.builder()
                    .available(false)
                    .message(returnTextToUserByLang(lang, "too_many_events_per_day"))
                    .eventsAsOrganizer(eventsAsOrganizer)
                    .eventsAsParticipant(eventsAsParticipant)
                    .uniqueEvents(uniqueEvents)
                    .totalEvents(totalEvents)
                    .build();
        }

        // Проверяем временные конфликты
        List<Event> existingEvents = eventsRepository.findEventsByOrganizerAndDate(organizerId, date);
        for (Event event : existingEvents) {
            Duration timeDifference = Duration.between(event.getDateTime(), proposedTime).abs();
            if (timeDifference.toMinutes() < 180) { // 3 hours minimum difference
                return EventCreationAvailabilityResponse.builder()
                        .available(false)
                        .message(returnTextToUserByLang(lang, "time_too_close"))
                        .eventsAsOrganizer(eventsAsOrganizer)
                        .eventsAsParticipant(eventsAsParticipant)
                        .uniqueEvents(uniqueEvents)
                        .totalEvents(totalEvents)
                        .build();
            }
        }

        return EventCreationAvailabilityResponse.builder()
                .available(true)
                .message(returnTextToUserByLang(lang, "can_create_event"))
                .eventsAsOrganizer(eventsAsOrganizer)
                .eventsAsParticipant(eventsAsParticipant)
                .uniqueEvents(uniqueEvents)
                .totalEvents(totalEvents)
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
        Event savedEvent = eventsRepository.save(event);

        // Мультиязычные тексты уведомлений
        Map<String, String> participantTexts = Map.of(
            "ru", "🏁 Всё! Игра сделана!\nВы крутые – сегодня был настоящий движ! 💥\nНе забудьте отблагодарить организатора – он всё устроил 💼→⚽️\nОценка — это ваш лайк в реальной жизни 🌟",
            "uz", "🏁 Tamom! O‘yin tugadi!\nBugun haqiqiy o‘yin / jang / harakat bo‘ldi – sizlar zo‘rsiz! 💥\nHammasini uyushtirgan tashkilotchiga rahmat aytishni unutmang 💼→⚽️\nBaholash – bu haqiqiy hayotdagi “like” 🌟",
            "en", "🏁 That’s a wrap! Game over!\nYou rocked it — what a match / clash / epic vibe today! 💥\nDon’t forget to thank the organizer — they made it all happen 💼→⚽️\nA rating is your real-life like 🌟"
        );
        Map<String, String> organizerTexts = Map.of(
            "ru", "🎉 Миссия выполнена! Ивент на ура!\nКоманда собралась, эмоции зарядили — время выдохнуть 😮‍💨\nТеперь оцените своих игроков — кто был душой компании, а кто «тихо, но метко» 🎯\nВаш отзыв — как медаль на память 🏅",
            "uz", "🎉 Vazifa bajarildi! Tadbir zo‘r o‘tdi!\nJamoa yig‘ildi, hissiyotlar chaqnadi — endi chuqur nafas oling 😮‍💨\nEndi ishtirokchilaringizni baholang — kim kompaniyaning yuragi bo‘ldi, kim esa «jim-jit, lekin aniq» 🎯\nSizning fikringiz – bu esdalik medali 🏅",
            "en", "🎉 Mission accomplished! The event was a blast!\nThe team showed up, the energy was high — now take a deep breath 😮‍💨\nTime to rate your players — who brought the fire, and who played it cool but sharp 🎯\nYour feedback is a medal of honor 🏅"
        );

        // Отправка push-уведомлений участникам
        if (event.getCurrentParticipants() != null && event.getCurrentParticipants().getParticipants() != null) {
            event.getCurrentParticipants().getParticipants().forEach(participant -> {
                if (!participant.getParticipantId().equals(event.getOrganizerEvent().getOrganizerId())) {
                    String lang = "ru";
                    try {
                        UserAdditionalInfo info = userAdditionalInfoRepository.findById(participant.getParticipantId()).orElse(null);
                        if (info != null && info.getLanguage() != null) {
                            lang = info.getLanguage();
                        }
                    } catch (Exception ignored) {}
                    String text = participantTexts.getOrDefault(lang, participantTexts.get("ru"));
                    pushNotificationService.sendSimpleNotification(
                        participant.getParticipantId(),
                        "Ивент завершился!",
                        text,
                        "EVENT_COMPLETED"
                    );
                }
            });
        }
        // Отправка push-уведомления организатору
        String orgLang = "ru";
        try {
            UserAdditionalInfo orgInfo = userAdditionalInfoRepository.findById(event.getOrganizerEvent().getOrganizerId()).orElse(null);
            if (orgInfo != null && orgInfo.getLanguage() != null) {
                orgLang = orgInfo.getLanguage();
            }
        } catch (Exception ignored) {}
        String orgText = organizerTexts.getOrDefault(orgLang, organizerTexts.get("ru"));
        pushNotificationService.sendSimpleNotification(
            event.getOrganizerEvent().getOrganizerId(),
            "Ивент завершился!",
            orgText,
            "EVENT_COMPLETED_ORG"
        );

        return savedEvent;
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
                if (event.getDateTime().isAfter(now)) {
                    event.startEvent();
                    eventsRepository.save(event);
                    Place getPlace = placeRepository.findPlaceByPlaceId(event.getPlaceId());


                    emailService.sendEventStatusChangeNotification(event,"ru",getPlace.getName(),getPlace.getPhone());
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

    public EventJoinAvailabilityResponse checkEventJoinAvailability(Long userId, LocalDate date, String lang) {
        // Получаем количество событий по ролям
        int eventsAsOrganizer = eventsRepository.countUserEventsAsOrganizerForDate(userId, date);
        int eventsAsParticipant = eventsRepository.countUserEventsAsParticipantForDate(userId, date);
        int uniqueEvents = eventsRepository.countUserEventsAsBothRolesForDate(userId, date);
        
        // Вычисляем общее количество уникальных событий
        int totalEvents = eventsAsOrganizer + eventsAsParticipant - uniqueEvents;
        
        if (totalEvents >= 3) {
            return EventJoinAvailabilityResponse.builder()
                    .available(false)
                    .message(returnTextToUserByLang(lang, "too_many_events_per_day_for_joining"))
                    .eventsAsOrganizer(eventsAsOrganizer)
                    .eventsAsParticipant(eventsAsParticipant)
                    .uniqueEvents(uniqueEvents)
                    .totalEvents(totalEvents)
                    .build();
        }
        
        return EventJoinAvailabilityResponse.builder()
                .available(true)
                .message(returnTextToUserByLang(lang, "can_join_event"))
                .eventsAsOrganizer(eventsAsOrganizer)
                .eventsAsParticipant(eventsAsParticipant)
                .uniqueEvents(uniqueEvents)
                .totalEvents(totalEvents)
                .build();
    }

    public Page<EventDTO> getOrganizationEvents(
            Long placeId,
            List<EventStatus> statuses,
            Pageable pageable) {
        
        log.info("Fetching organization events for placeId: {} with statuses: {}, pagination: {}", 
            placeId, statuses, pageable);

        // Преобразуем список статусов в массив строк
        String[] statusArray = statuses != null && !statuses.isEmpty() 
            ? statuses.stream()
                .map(EventStatus::name)
                .toArray(String[]::new)
            : null;

        return eventsRepository.findOrganizationEventsByStatus(placeId, statusArray, pageable)
                .map(this::convertToDTO);
    }

    private boolean isValidOrganizationEventStatus(EventStatus status) {
        return switch (status) {
            case REJECTED, CONFIRMED, CHANGES_REQUESTED, IN_PROGRESS, COMPLETED, EXPIRED -> true;
            default -> false;
        };
    }

    @Transactional
    public EventDTO moveToPendingApproval(Long eventId, Long userId, String lang) {
        Event event = findAndValidateEvent(eventId, lang);
        validateEventOrganizer(event, userId, lang);
        
        if (!event.getStatus().equals(EventStatus.OPEN)) {
            throw new EventValidationException("invalid_status",
                    returnTextToUserByLang(lang, "invalid_status"));
        }
        
        event.setStatus(EventStatus.PENDING_APPROVAL);
        return convertToDTO(eventsRepository.save(event));
    }

    public NearestEventDTO getNearestEventForUser(Long userId) {
        log.info("Getting nearest event for user {}", userId);
        
        try {
            LocalDateTime currentTime = LocalDateTime.now();
            Event nearestEvent = eventsRepository.findNearestEventForUser(userId, currentTime);
            
            if (nearestEvent == null) {
                log.info("No nearest event found for user {}", userId);
                return null;
            }
            
            return convertToNearestEventDTO(nearestEvent, userId);
        } catch (Exception e) {
            log.error("Error getting nearest event for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Error getting nearest event", e);
        }
    }

    public EventDTO getCurrentInProgressEventForUser(Long userId) {
        log.info("Getting current IN_PROGRESS event for user {}", userId);
        
        try {
            Event currentEvent = eventsRepository.findCurrentInProgressEventForUser(userId);
            
            if (currentEvent == null) {
                log.info("No current IN_PROGRESS event found for user {}", userId);
                return null;
            }
            
            return convertToDTO(currentEvent);
        } catch (Exception e) {
            log.error("Error getting current IN_PROGRESS event for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Error getting current IN_PROGRESS event", e);
        }
    }

    public List<EventDTO> getEventsForToday(Long userId) {
        log.info("Getting events for today for user {}", userId);
        
        try {
            LocalDate today = LocalDate.now();
            List<Event> todayEvents = eventsRepository.findEventsForTodayByUser(userId, today);
            
            log.info("Found {} events for today for user {}", todayEvents.size(), userId);
            
            return todayEvents.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting events for today for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Error getting events for today", e);
        }
    }

    private NearestEventDTO convertToNearestEventDTO(Event event, Long userId) {
        LocalDateTime currentTime = LocalDateTime.now();
        
        // Вычисляем время до события
        Duration timeUntilEvent = Duration.between(currentTime, event.getDateTime());
        long totalMinutes = timeUntilEvent.toMinutes();
        
        // Форматируем время до события
        String timeUntilEventFormatted = formatTimeUntilEvent(timeUntilEvent);
        String timeFormat = getTimeFormat(totalMinutes);
        
        // Конвертируем в DTO
        EventDTO eventDTO = convertToDTO(event);
        
        return NearestEventDTO.builder()
                .eventId(eventDTO.getEventId())
                .title(eventDTO.getTitle())
                .description(eventDTO.getDescription())
                .dateTime(eventDTO.getDateTime())
                .status(eventDTO.getStatus())
                .placeId(eventDTO.getPlaceId())
                .organizer(eventDTO.getOrganizer())
                .participantsCount(eventDTO.getParticipantsCount())
                .maxParticipants(eventDTO.getMaxParticipants())
                .eventType(eventDTO.getEventType())
                .location(eventDTO.getLocation())
                .price(eventDTO.getPrice())
                .additionalInfo(eventDTO.getAdditionalInfo())
                .timeUntilEvent(timeUntilEventFormatted)
                .totalMinutesUntilEvent(totalMinutes)
                .isUpcoming(true)
                .timeFormat(timeFormat)
                .build();
    }
    
    private String formatTimeUntilEvent(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        
        if (days > 0) {
            return String.format("%d дн. %d ч. %d мин.", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%d ч. %d мин.", hours, minutes);
        } else {
            return String.format("%d мин.", minutes);
        }
    }
    
    private String getTimeFormat(long totalMinutes) {
        if (totalMinutes >= 1440) { // 24 часа
            return "DAYS";
        } else if (totalMinutes >= 60) { // 1 час
            return "HOURS";
        } else {
            return "MINUTES";
        }
    }

    public List<CheckInEventDTO> getUserEventsForCheckIn(Long placeId, Long userId) {
        log.info("Getting user events for check-in at place {} for user {}", placeId, userId);
        LocalDate today = LocalDate.now();
        LocalDateTime nowPlus30 = LocalDateTime.now().plusMinutes(30);
        List<Event> events = eventsRepository.findEventsForCheckInTodaySimple(placeId, today, nowPlus30);
        return events.stream()
            .filter(event -> {
                boolean isOrganizer = event.getOrganizerEvent() != null && event.getOrganizerEvent().getOrganizerId().equals(userId);
                boolean isParticipant = event.getCurrentParticipants() != null && event.getCurrentParticipants().hasParticipant(userId);
                return isOrganizer || isParticipant;
            })
            .map(event -> convertToCheckInDTO(event, userId))
            .toList();
    }

    @Transactional
    public void checkInParticipant(Long eventId, Long userId, String lang) {
        log.info("Participant {} checking in for event {}", userId, eventId);
        Event event = findAndValidateEvent(eventId, lang);

        // Проверка статуса ивента
        Set<EventStatus> allowedStatuses = EnumSet.of(EventStatus.CONFIRMED, EventStatus.IN_PROGRESS);
        if (!allowedStatuses.contains(event.getStatus())) {
            throw new EventValidationException("checkin_not_confirmed",
                    returnTextToUserByLang(lang, "checkin_not_confirmed"));
        }

        // Проверка времени до начала ивента (разрешено за 30 минут до старта) и присоединение к ивенту,которое закончено нельзя
        LocalDateTime now = LocalDateTime.now();
        long minutesBefore = Duration.between(now, event.getDateTime()).toMinutes();

        if (minutesBefore > 30) {
            throw new EventValidationException("checkin_time_window",
                    returnTextToUserByLang(lang, "checkin_time_window"));
        }

        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.COMPLETED) {
            throw new EventValidationException("checkin_not_allowed",
                    returnTextToUserByLang(lang, "checkin_not_allowed"));
        }

        boolean isOrganizer = event.getOrganizerEvent() != null && event.getOrganizerEvent().getOrganizerId().equals(userId);
        boolean isParticipant = event.getCurrentParticipants() != null && event.getCurrentParticipants().hasParticipant(userId);

        // Если пользователь не участник и не организатор — ошибка
        if (!isParticipant && !isOrganizer) {
            throw new EventValidationException("not_participant",
                    returnTextToUserByLang(lang, "not_participant"));
        }

        // Если пользователь уже участник — проверяем статус
        if (isParticipant) {
            String currentStatus = event.getCurrentParticipants().getParticipants().stream()
                    .filter(p -> p.getParticipantId().equals(userId))
                    .map(CurrentParticipants.Participant::getStatus)
                    .findFirst()
                    .orElse("ACTIVE");
            if ("PRESENT".equals(currentStatus)) {
                throw new EventValidationException("already_checked_in",
                        returnTextToUserByLang(lang, "already_checked_in"));
            }
            // Отмечаем присутствие
            event.getCurrentParticipants().checkInParticipant(userId);
        } else if (isOrganizer) {
            // Если организатор не в списке участников — добавляем его как PRESENT
            String organizerName = event.getOrganizerEvent().getOrganizerName();
            CurrentParticipants.Participant organizerParticipant = new CurrentParticipants.Participant(
                    userId, organizerName, "PRESENT", now, now
            );
            event.getCurrentParticipants().getParticipants().add(organizerParticipant);
            event.getCurrentParticipants().setSize(event.getCurrentParticipants().getParticipants().size());
        }

        eventsRepository.save(event);

        // Отправляем сообщение в чат
        String userName = isParticipant ? event.getCurrentParticipants().getParticipantName(userId) : event.getOrganizerEvent().getOrganizerName();
        eventMessageService.sendEventMessage(event, EventMessageType.PARTICIPANT_CHECKED_IN, userName, lang);

        log.info("Participant {} successfully checked in for event {}", userId, eventId);
    }

    private CheckInEventDTO convertToCheckInDTO(Event event, Long userId) {
        // Определяем роль пользователя
        boolean isOrganizer = event.getOrganizerEvent() != null && 
                             event.getOrganizerEvent().getOrganizerId().equals(userId);
        
        String userRole = isOrganizer ? "ORGANIZER" : "PARTICIPANT";
        
        // Определяем статус пользователя в ивенте
        String userStatus = "ACTIVE";
        if (event.getCurrentParticipants() != null) {
            userStatus = event.getCurrentParticipants().getParticipants().stream()
                    .filter(p -> p.getParticipantId().equals(userId))
                    .map(CurrentParticipants.Participant::getStatus)
                    .findFirst()
                    .orElse(isOrganizer ? "ORGANIZER" : "ACTIVE");
        }
        
        // Определяем возможности пользователя
        boolean canStart = isOrganizer && event.getStatus() == EventStatus.CONFIRMED
            && !event.getDateTime().minusMinutes(30).isAfter(LocalDateTime.now());
        boolean canCheckIn = "ACTIVE".equals(userStatus);
        
        // Конвертируем участников
        List<ParticipantDTO> participants = new ArrayList<>();
        if (event.getCurrentParticipants() != null) {
            participants = event.getCurrentParticipants().getParticipants().stream()
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
                                profilePictureUrl,
                                participant.getStatus()
                        );
                    })
                    .toList();
        }
        
        // Конвертируем организатора
        OrganizerDTO organizer = null;
        if (event.getOrganizerEvent() != null) {
            organizer = OrganizerDTO.builder()
                    .organizerId(event.getOrganizerEvent().getOrganizerId())
                    .name(event.getOrganizerEvent().getOrganizerName())
                    .email(event.getOrganizerEvent().getEmail())
                    .phoneNumber(event.getOrganizerEvent().getPhoneNumber())
                    .organizationType("INDIVIDUAL")
                    .rating(4.8)
                    .build();
            
            try {
                String profilePictureUrl = userProfileService.getProfilePictureUrl(event.getOrganizerEvent().getOrganizerId());
                organizer.setProfilePictureUrl(profilePictureUrl);
            } catch (Exception e) {
                log.warn("Could not fetch profile picture for organizer {}", event.getOrganizerEvent().getOrganizerId());
            }
        }
        
        return CheckInEventDTO.builder()
                .eventId(event.getEventId())
                .title(event.getTitle())
                .description(event.getDescription())
                .dateTime(event.getDateTime())
                .status(event.getStatus().name())
                .userRole(userRole)
                .canStart(canStart)
                .canCheckIn(canCheckIn)
                .userStatus(userStatus)
                .participants(participants)
                .organizer(organizer)
                .location(event.getSportEvent().getLocation())
                .price(event.getSportEvent().getPrice())
                .additionalInfo(event.getAdditionalInfo())
                .build();
    }
}