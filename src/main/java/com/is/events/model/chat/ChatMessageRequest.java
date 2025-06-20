package com.is.events.model.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {
    private String content;
    private Long quotedMessageId;
    private String quotedMessageContent;
    private String quotedMessageSender;
} 