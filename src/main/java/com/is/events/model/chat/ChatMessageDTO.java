package com.is.events.model.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private Long messageId;
    private Long eventId;
    private Long senderId;
    private String senderName;
    private String content;
    private LocalDateTime sentAt;
    private String senderAvatarUrl;
    private MessageType type;
    private Boolean isEdited;
    private LocalDateTime editedAt;
    private Boolean isDeleted;
    private List<Long> readBy;
    private Long parentMessageId;
    private String parentMessageContent;
    private String parentMessageSender;
    private LocalDateTime parentMessageSentAt;
    private Map<String, List<Long>> reactions;
} 