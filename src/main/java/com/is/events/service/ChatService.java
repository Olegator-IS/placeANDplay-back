package com.is.events.service;

import com.is.auth.model.ResponseAnswers.Response;
import com.is.auth.model.user.UserService;
import com.is.events.model.chat.EventMessage;
import com.is.events.model.chat.ChatMessagesRequest;
import com.is.events.repository.EventMessageRepository;
import lombok.extern.slf4j.Slf4j;
import com.is.events.model.chat.ChatMessageDTO;
import com.is.events.model.chat.ChatMessageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatService {
    private final EventMessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    private final WebSocketService webSocketService;

    public ChatService(EventMessageRepository messageRepository,
                      SimpMessagingTemplate messagingTemplate,
                      UserService userService,
                      WebSocketService webSocketService) {
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
        this.webSocketService = webSocketService;
    }

    public Page<ChatMessageDTO> getEventMessages(Long eventId, ChatMessagesRequest request,
                                               String accessToken, String refreshToken, String lang) {
        // Validate user access
        validateUserAccess(accessToken, refreshToken, lang);
        
        Sort.Direction direction = Sort.Direction.fromString(request.getSortDirection().toUpperCase());
        PageRequest pageRequest = PageRequest.of(
            request.getPage(),
            request.getSize(),
            Sort.by(direction, "sentAt")
        );
        
        Page<EventMessage> messages = messageRepository.findByEventId(eventId, pageRequest);
        
        // Get all unique sender IDs
        List<Long> senderIds = messages.getContent().stream()
            .map(EventMessage::getSenderId)
            .filter(id -> id != null && id != 0)
            .distinct()
            .collect(Collectors.toList());
            
        // Get profile pictures for all senders
        Map<Long, String> userAvatars = userService.getUsersProfilePicturesForChat(senderIds);
        
        return messages.map(message -> {
            ChatMessageDTO dto = convertToDTO(message);
            if (message.getSenderId() != null && message.getSenderId() != 0) {
                dto.setSenderAvatarUrl(userAvatars.getOrDefault(message.getSenderId(), ""));
            }
            return dto;
        });
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
        
        return messages.stream()
            .map(message -> {
                ChatMessageDTO dto = convertToDTO(message);
                if (message.getSenderId() != null && message.getSenderId() != 0) {
                    dto.setSenderAvatarUrl(userAvatars.getOrDefault(message.getSenderId(), ""));
                }
                return dto;
            })
            .collect(Collectors.toList());
    }

    private void validateUserAccess(String accessToken, String refreshToken, String lang) {
        ResponseEntity<Response> userResponse = userService.validateTokenAndGetSubject(accessToken, refreshToken, lang);
        if (userResponse.getStatusCode().is4xxClientError()) {
            throw new RuntimeException("Unauthorized access");
        }
    }

    @Transactional
    public ChatMessageDTO sendMessage(Long eventId, Long userId, String message, String accessToken, String refreshToken, String language) {
        try {
            // Для системных сообщений (когда userId = 0)
            if (userId == 0) {
                EventMessage systemMessage = EventMessage.builder()
                    .eventId(eventId)
                    .userId(userId)
                    .message(message)
                    .content(message)  // дублируем сообщение в оба поля
                    .type("SYSTEM")
                    .timestamp(LocalDateTime.now())
                    .sentAt(LocalDateTime.now())
                    .senderName("System")
                    .build();

                EventMessage savedMessage = messageRepository.save(systemMessage);
                
                // Отправляем уведомление через WebSocket
                ChatMessageDTO messageDTO = convertToDTO(savedMessage);
                messagingTemplate.convertAndSend("/topic/chat/" + eventId, messageDTO);
                return messageDTO;
            }

            // Для обычных пользовательских сообщений
            validateUserAccess(accessToken, refreshToken, language);
            
            ResponseEntity<Response> userResponse = userService.validateTokenAndGetSubject(accessToken, refreshToken, language);
            EventMessage userMessage = EventMessage.builder()
                .eventId(eventId)
                .senderId(userId)
                .content(message)
                .sentAt(LocalDateTime.now())
                .senderName("User " + userId)
                .type("USER")
                .build();

            if (userResponse.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userInfo = (Map<String, Object>) userResponse.getBody().getResult();
                
                String firstName = userInfo.get("firstName") != null ? userInfo.get("firstName").toString() : "";
                String lastName = userInfo.get("lastName") != null ? userInfo.get("lastName").toString() : "";
                
                userMessage.setSenderName(firstName + " " + lastName);
            }

            EventMessage savedMessage = messageRepository.save(userMessage);
            ChatMessageDTO messageDTO = convertToDTO(savedMessage);
            
            // Get sender's profile picture
            Map<Long, String> userAvatars = userService.getUsersProfilePicturesForChat(List.of(userId));
            messageDTO.setSenderAvatarUrl(userAvatars.getOrDefault(userId, ""));
            
            messagingTemplate.convertAndSend("/topic/chat/" + eventId, messageDTO);
            return messageDTO;

        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage());
            throw new RuntimeException("Error sending message", e);
        }
    }

    private ChatMessageDTO convertToDTO(EventMessage message) {
        return ChatMessageDTO.builder()
                .messageId(message.getMessageId())
                .eventId(message.getEventId())
                .senderId(message.getSenderId())
                .senderName(message.getSenderName())
                .content(message.getContent() != null ? message.getContent() : message.getMessage())
                .sentAt(message.getSentAt() != null ? message.getSentAt() : message.getTimestamp())
                .type(message.getType())
                .build();
    }
} 