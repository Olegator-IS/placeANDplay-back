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

    public ChatService(EventMessageRepository messageRepository,
                      SimpMessagingTemplate messagingTemplate,
                      UserService userService) {
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
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
        
        return messageRepository.findByEventId(eventId, pageRequest)
                .map(this::convertToDTO);
    }

    public List<ChatMessageDTO> getLatestEventMessages(Long eventId, int limit,
                                                      String accessToken, String refreshToken, String lang) {
        // Validate user access
        validateUserAccess(accessToken, refreshToken, lang);
        
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "sentAt"));
        return messageRepository.findByEventId(eventId, pageRequest)
                .getContent()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private void validateUserAccess(String accessToken, String refreshToken, String lang) {
        ResponseEntity<Response> userResponse = userService.validateTokenAndGetSubject(accessToken, refreshToken, lang);
        if (userResponse.getStatusCode().is4xxClientError()) {
            throw new RuntimeException("Unauthorized access");
        }
    }

    @Transactional
    public ChatMessageDTO sendMessage(Long eventId, Long senderId, String content,
                                      String accessToken,
                                      String refreshToken,
                                      String lang) {
        log.info("Sending message to event: {} from user: {}", eventId, senderId);
        
        // Validate user access first
        validateUserAccess(accessToken, refreshToken, lang);
        
        EventMessage message = EventMessage.builder()
                .eventId(eventId)
                .senderId(senderId)
                .senderName("User " + senderId) // Default name if we can't get user info
                .content(content)
                .sentAt(LocalDateTime.now())
                .build();

        try {
            ResponseEntity<Response> userResponse = userService.validateTokenAndGetSubject(accessToken, refreshToken, lang);
            if (userResponse.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userInfo = (Map<String, Object>) userResponse.getBody().getResult();
                
                String firstName = userInfo.get("firstName") != null ? userInfo.get("firstName").toString() : "";
                String lastName = userInfo.get("lastName") != null ? userInfo.get("lastName").toString() : "";
                String avatarUrl = userInfo.get("profile_picture_url") != null ? userInfo.get("profile_picture_url").toString() : "";
                
                message.setSenderName(firstName + " " + lastName);
                message.setSenderAvatarUrl(avatarUrl);
            }
        } catch (Exception e) {
            log.warn("Could not get user information, using default values", e);
        }

        EventMessage savedMessage = messageRepository.save(message);
        ChatMessageDTO messageDTO = convertToDTO(savedMessage);

        // Отправляем сообщение всем подписчикам события
        messagingTemplate.convertAndSend("/topic/chat/" + eventId, messageDTO);
        
        return messageDTO;
    }

    private ChatMessageDTO convertToDTO(EventMessage message) {
        return ChatMessageDTO.builder()
                .messageId(message.getMessageId())
                .eventId(message.getEventId())
                .senderId(message.getSenderId())
                .senderName(message.getSenderName())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .senderAvatarUrl(message.getSenderAvatarUrl())
                .build();
    }
} 