package com.is.events.model.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private String type;
} 