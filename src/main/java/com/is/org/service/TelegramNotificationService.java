package com.is.org.service;

import com.is.org.model.OrganizationTelegramChat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramNotificationService {

    private final OrganizationTelegramChatService telegramChatService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${telegram.bot.default-token:}")
    private String defaultBotToken;

    @Value("${telegram.api.url}")
    private String telegramApiUrl;

    /**
     * Отправляет уведомление в Telegram для конкретной организации
     */
    public void sendNotificationToOrganization(Long orgId, String message) {
        List<OrganizationTelegramChat> chats = telegramChatService.findByOrgId(orgId);
        
        if (chats.isEmpty()) {
            log.warn("No active telegram chats found for organization: {}", orgId);
            return;
        }

        for (OrganizationTelegramChat chat : chats) {
            try {
                sendMessage(chat.getChatId(), message, chat.getBotToken());
                log.info("Notification sent to chat {} for organization {}", chat.getChatId(), orgId);
            } catch (Exception e) {
                log.error("Failed to send notification to chat {} for organization {}: {}", 
                    chat.getChatId(), orgId, e.getMessage());
            }
        }
    }

    /**
     * Отправляет уведомление в конкретный chat ID
     */
    public void sendNotificationToChat(String chatId, String message) {
        sendNotificationToChat(chatId, message, defaultBotToken);
    }

    /**
     * Отправляет уведомление в конкретный chat ID с указанным токеном бота
     */
    public void sendNotificationToChat(String chatId, String message, String botToken) {
        try {
            sendMessage(chatId, message, botToken);
            log.info("Notification sent to chat: {}", chatId);
        } catch (Exception e) {
            log.error("Failed to send notification to chat {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Отправляет уведомление всем активным организациям
     */
    public void sendNotificationToAllOrganizations(String message) {
        List<OrganizationTelegramChat> allChats = telegramChatService.findAllActive();
        
        for (OrganizationTelegramChat chat : allChats) {
            try {
                sendMessage(chat.getChatId(), message, chat.getBotToken());
                log.info("Broadcast notification sent to chat {} for organization {}", 
                    chat.getChatId(), chat.getOrgId());
            } catch (Exception e) {
                log.error("Failed to send broadcast notification to chat {} for organization {}: {}", 
                    chat.getChatId(), chat.getOrgId(), e.getMessage());
            }
        }
    }

    /**
     * Отправляет сообщение через Telegram Bot API
     */
    private void sendMessage(String chatId, String message, String botToken) {

        String url = telegramApiUrl  + "/sendNotification";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("chatId", chatId);
        requestBody.put("message", message);
//        requestBody.put("parse_mode", "HTML"); // Поддержка HTML разметки

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            restTemplate.postForObject(url, request, String.class);
        } catch (Exception e) {
            log.error("Error sending telegram message: {}", e.getMessage());
            throw new RuntimeException("Failed to send telegram message", e);
        }
    }

    /**
     * Проверяет доступность бота
     */
    public boolean testBotConnection(String botToken) {
        if (botToken == null || botToken.isEmpty()) {
            botToken = defaultBotToken;
        }

        if (botToken == null || botToken.isEmpty()) {
            return false;
        }

        String url = telegramApiUrl + botToken + "/getMe";

        try {
            String response = restTemplate.getForObject(url, String.class);
            return response != null && response.contains("\"ok\":true");
        } catch (Exception e) {
            log.error("Error testing bot connection: {}", e.getMessage());
            return false;
        }
    }
}
