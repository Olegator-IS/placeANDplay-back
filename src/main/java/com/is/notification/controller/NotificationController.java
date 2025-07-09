package com.is.notification.controller;

import com.is.notification.dto.NotificationDTO;
import com.is.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationDTO> getUserNotifications(@RequestHeader Long userId) {
        return notificationService.getUserNotifications(userId);
    }

    @PostMapping("/{notificationId}/read")
    public void markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
    }

    @GetMapping("/readAll")
    public void markAsReadAll(@RequestParam Long userId) {
        notificationService.markAsReadAll(userId);
    }
} 