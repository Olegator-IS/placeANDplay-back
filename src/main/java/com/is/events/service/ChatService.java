package com.is.events.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.is.auth.model.ResponseAnswers.Response;
import com.is.auth.model.user.UserService;
import com.is.auth.service.PushNotificationService;
import com.is.events.model.Event;
import com.is.events.model.chat.EventMessage;
import com.is.events.model.chat.ChatMessagesRequest;
import com.is.events.model.chat.MessageType;
import com.is.events.repository.EventMessageRepository;
import com.is.events.repository.EventsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.is.events.model.chat.ChatMessageDTO;
import com.is.events.model.chat.ChatMessageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final EventMessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper;
    private final PushNotificationService pushNotificationService;
    private final EventsRepository eventsRepository;

    public Page<ChatMessageDTO> getEventMessages(Long eventId, ChatMessagesRequest request,
                                               String accessToken, String refreshToken, String lang) {
        log.info("Starting getEventMessages for eventId: {}, request: {}", eventId, request);
        validateUserAccess(accessToken, refreshToken, lang);
        
        Sort.Direction direction = Sort.Direction.fromString(request.getSortDirection().toUpperCase());
        PageRequest pageRequest = PageRequest.of(
            request.getPage(),
            request.getSize(),
            Sort.by(direction, "sentAt")
        );
        
        log.info("Fetching messages from repository with pageRequest: {}", pageRequest);
        Page<EventMessage> messages = messageRepository.findByEventId(eventId, pageRequest);
        log.info("Found {} messages", messages.getTotalElements());
        
        // Get all unique sender IDs
        List<Long> senderIds = messages.getContent().stream()
            .map(EventMessage::getSenderId)
            .filter(id -> id != null && id != 0)
            .distinct()
            .collect(Collectors.toList());
            
        // Get profile pictures for all senders
        Map<Long, String> userAvatars = userService.getUsersProfilePicturesForChat(senderIds);
        log.info("Retrieved {} user avatars", userAvatars.size());
        
        // Get user names for all senders
        Map<Long, String> userNames = new HashMap<>();
        for (Long senderId : senderIds) {
            try {
                ResponseEntity<Response> userResponse = userService.getUserProfile(senderId, lang);
                if (userResponse.getBody() != null && userResponse.getBody().getResult() != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userInfo = (Map<String, Object>) userResponse.getBody().getResult();
                    String firstName = userInfo.get("first_name") != null ? userInfo.get("first_name").toString() : "";
                    String lastName = userInfo.get("last_name") != null ? userInfo.get("last_name").toString() : "";
                    userNames.put(senderId, firstName + " " + lastName);
                }
            } catch (Exception e) {
                log.warn("Failed to get user name for ID {}: {}", senderId, e.getMessage());
                userNames.put(senderId, "User " + senderId);
            }
        }
        
        Page<ChatMessageDTO> result = messages.map(message -> {
            ChatMessageDTO dto = convertToDTO(message);
            if (message.getSenderId() != null && message.getSenderId() != 0) {
                dto.setSenderAvatarUrl(userAvatars.getOrDefault(message.getSenderId(), ""));
                dto.setSenderName(userNames.getOrDefault(message.getSenderId(), "User " + message.getSenderId()));
            }
            return dto;
        });
        
        log.info("Returning {} DTOs", result.getTotalElements());
        return result;
    }

    public List<ChatMessageDTO> getLatestEventMessages(Long eventId, int limit,
                                                      String accessToken, String refreshToken, String lang) {
        // Validate user access
        validateUserAccess(accessToken, refreshToken, lang);
        
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "sentAt"));
        List<EventMessage> messages = messageRepository.findByEventId(eventId, pageRequest).getContent();
        
        // Get all unique sender IDs
        List<Long> senderIds = messages.stream()
            .map(EventMessage::getSenderId)
            .filter(id -> id != null && id != 0)
            .distinct()
            .collect(Collectors.toList());
            
        // Get profile pictures for all senders
        Map<Long, String> userAvatars = userService.getUsersProfilePicturesForChat(senderIds);
        
        // Get user names for all senders
        Map<Long, String> userNames = new HashMap<>();
        for (Long senderId : senderIds) {
            try {
                ResponseEntity<Response> userResponse = userService.getUserProfile(senderId, lang);
                if (userResponse.getBody() != null && userResponse.getBody().getResult() != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userInfo = (Map<String, Object>) userResponse.getBody().getResult();
                    String firstName = userInfo.get("firstName") != null ? userInfo.get("firstName").toString() : "";
                    String lastName = userInfo.get("lastName") != null ? userInfo.get("lastName").toString() : "";
                    userNames.put(senderId, firstName + " " + lastName);
                }
            } catch (Exception e) {
                log.warn("Failed to get user name for ID {}: {}", senderId, e.getMessage());
                userNames.put(senderId, "User " + senderId);
            }
        }
        
        return messages.stream()
            .map(message -> {
                ChatMessageDTO dto = convertToDTO(message);
                if (message.getSenderId() != null && message.getSenderId() != 0) {
                    dto.setSenderAvatarUrl(userAvatars.getOrDefault(message.getSenderId(), ""));
                    dto.setSenderName(userNames.getOrDefault(message.getSenderId(), "User " + message.getSenderId()));
                }
                return dto;
            })
            .collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageDTO sendMessage(Long eventId, Long userId, ChatMessageRequest request, String accessToken, String refreshToken, String language) {
        try {
            if (userId == 0) {
                EventMessage systemMessage = new EventMessage();
                systemMessage.setEventId(eventId);
                systemMessage.setSenderId(userId);
                systemMessage.setMessage(request.getContent());
                systemMessage.setContent(request.getContent());
                systemMessage.setType(MessageType.SYSTEM);
                systemMessage.setTimestamp(LocalDateTime.now());
                systemMessage.setSentAt(LocalDateTime.now());
                systemMessage.setSenderName("System");
                systemMessage.setIsEdited(false);
                systemMessage.setIsDeleted(false);
                systemMessage.setReadBy(new ArrayList<>());
                systemMessage.setReactions(new HashMap<>());
                if (request.getQuotedMessageId() != null) {
                    systemMessage.setParentMessageId(request.getQuotedMessageId());
                }
                EventMessage savedMessage = messageRepository.save(systemMessage);
                ChatMessageDTO messageDTO = convertToDTO(savedMessage);
                messagingTemplate.convertAndSend("/topic/chat/" + eventId, messageDTO);
                return messageDTO;
            }

            validateUserAccess(accessToken, refreshToken, language);

            ResponseEntity<Response> userResponse = userService.validateTokenAndGetSubject(accessToken, refreshToken, language);
            EventMessage userMessage = new EventMessage();
            userMessage.setEventId(eventId);
            userMessage.setSenderId(userId);
            userMessage.setMessage(request.getContent());
            userMessage.setContent(request.getContent());
            userMessage.setSentAt(LocalDateTime.now());
            userMessage.setSenderName("User " + userId);
            userMessage.setType(MessageType.TEXT);
            userMessage.setIsEdited(false);
            userMessage.setIsDeleted(false);
            userMessage.setReadBy(new ArrayList<>());
            userMessage.setReactions(new HashMap<>());
            if (request.getQuotedMessageId() != null) {
                userMessage.setParentMessageId(request.getQuotedMessageId());
            }
            if (userResponse.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userInfo = (Map<String, Object>) userResponse.getBody().getResult();
                String firstName = userInfo.get("firstName") != null ? userInfo.get("firstName").toString() : "";
                String lastName = userInfo.get("lastName") != null ? userInfo.get("lastName").toString() : "";
                userMessage.setSenderName(firstName + " " + lastName);
            }
            EventMessage savedMessage = messageRepository.save(userMessage);
            ChatMessageDTO messageDTO = convertToDTO(savedMessage);
            Map<Long, String> userAvatars = userService.getUsersProfilePicturesForChat(List.of(userId));
            messageDTO.setSenderAvatarUrl(userAvatars.getOrDefault(userId, ""));

            // Отправляем сообщение через WebSocket
            messagingTemplate.convertAndSend("/topic/chat/" + eventId, messageDTO);

            // Отправляем push-уведомления участникам
            Event event = eventsRepository.findEventByEventId(eventId);
            if (event != null) {
                String senderName = messageDTO.getSenderName();
                pushNotificationService.sendNewChatMessageNotification(
                    event,
                    senderName,
                    messageDTO.getContent(),
                    userId
                );
            }

            return messageDTO;
        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage());
            throw new RuntimeException("Error sending message", e);
        }
    }

    @Transactional
    public ChatMessageDTO editMessage(Long messageId, Long userId, String newContent) {
        EventMessage message = messageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Message not found"));
        if (!message.getSenderId().equals(userId)) {
            throw new RuntimeException("Not authorized to edit this message");
        }
        message.setContent(newContent);
        message.setIsEdited(true);
        message.setEditedAt(LocalDateTime.now());
        EventMessage savedMessage = messageRepository.save(message);
        ChatMessageDTO dto = convertToDTO(savedMessage);
        messagingTemplate.convertAndSend("/topic/chat/" + message.getEventId(), dto);
        return dto;
    }

    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        EventMessage message = messageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Message not found"));
        if (!message.getSenderId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this message");
        }
        message.setIsDeleted(true);
        message.setDeletedAt(LocalDateTime.now());
        messageRepository.save(message);
        ChatMessageDTO dto = convertToDTO(message);
        messagingTemplate.convertAndSend("/topic/chat/" + message.getEventId(), dto);
    }

    @Transactional
    public void markMessageAsRead(Long messageId, Long userId) {
        EventMessage message = messageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Message not found"));
        List<Long> readBy = message.getReadBy();
        if (readBy == null) readBy = new ArrayList<>();
        if (!readBy.contains(userId)) {
            readBy.add(userId);
            message.setReadBy(readBy);
            messageRepository.save(message);
        }
    }

    public Page<ChatMessageDTO> searchMessages(Long eventId, String query, Pageable pageable) {
        return messageRepository.findByEventIdAndContentContainingIgnoreCase(
            eventId, query, pageable)
            .map(this::convertToDTO);
    }

    @Transactional
    public void addReaction(Long messageId, Long userId, String reactionType) {
        EventMessage message = messageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Message not found"));
        Map<String, List<Long>> reactions = message.getReactions();
        if (reactions == null) reactions = new HashMap<>();
        reactions.computeIfAbsent(reactionType, k -> new ArrayList<>());
        if (!reactions.get(reactionType).contains(userId)) {
            reactions.get(reactionType).add(userId);
            message.setReactions(reactions);
            messageRepository.save(message);
            ChatMessageDTO dto = convertToDTO(message);
            messagingTemplate.convertAndSend("/topic/chat/" + message.getEventId(), dto);
        }
    }

    @Transactional
    public void removeReaction(Long messageId, Long userId, String reactionType) {
        EventMessage message = messageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Message not found"));
        Map<String, List<Long>> reactions = message.getReactions();
        if (reactions != null && reactions.containsKey(reactionType)) {
            reactions.get(reactionType).remove(userId);
            if (reactions.get(reactionType).isEmpty()) {
                reactions.remove(reactionType);
            }
            message.setReactions(reactions);
            messageRepository.save(message);
            ChatMessageDTO dto = convertToDTO(message);
            messagingTemplate.convertAndSend("/topic/chat/" + message.getEventId(), dto);
        }
    }

    private ChatMessageDTO convertToDTO(EventMessage message) {
        ChatMessageDTO dto = ChatMessageDTO.builder()
                .messageId(message.getMessageId())
                .eventId(message.getEventId())
                .senderId(message.getSenderId())
                .senderName(message.getSenderName())
                .content(message.getContent() != null ? message.getContent() : message.getMessage())
                .sentAt(message.getSentAt() != null ? message.getSentAt() : message.getTimestamp())
                .type(message.getType())
                .isEdited(message.getIsEdited())
                .editedAt(message.getEditedAt())
                .isDeleted(message.getIsDeleted())
                .build();

        if (message.getReadBy() != null) {
            dto.setReadBy(message.getReadBy());
        }
        
        if (message.getReactions() != null) {
            dto.setReactions(message.getReactions());
        }

        if (message.getParentMessageId() != null) {
            dto.setParentMessageId(message.getParentMessageId());
            Optional<EventMessage> parentMessage = messageRepository.findById(message.getParentMessageId());
            parentMessage.ifPresent(parent -> {
                dto.setParentMessageContent(parent.getContent());
                dto.setParentMessageSender(parent.getSenderName());
                dto.setParentMessageSentAt(parent.getSentAt());
            });
        }

        return dto;
    }

    private void validateUserAccess(String accessToken, String refreshToken, String lang) {
        ResponseEntity<Response> userResponse = userService.validateTokenAndGetSubject(accessToken, refreshToken, lang);
        if (userResponse.getStatusCode().is4xxClientError()) {
            throw new RuntimeException("Unauthorized access");
        }
    }

    public List<EventMessage> getRawMessagesDebug(Long eventId) {
        return messageRepository.findByEventIdOrderBySentAtAsc(eventId);
    }
} 