package com.is.events.service;

import com.is.events.model.chat.EventMessage;
import com.is.events.repository.EventMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventMessageService {
    private final EventMessageRepository messageRepository;
    private final WebSocketService webSocketService;
    private final UserProfileService userProfileService;

} 