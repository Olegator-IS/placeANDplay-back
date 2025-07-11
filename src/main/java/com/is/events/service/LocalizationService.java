package com.is.events.service;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

@Slf4j
@Service
public class LocalizationService {
    private final MessageSource messageSource;

    public LocalizationService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String getMessage(String key, String lang) {
        Locale locale = switch (lang.toLowerCase()) {
            case "ru" -> new Locale("ru");
            case "uz" -> new Locale("uz");
            default -> Locale.ENGLISH;
        };
        
        // Если ключ уже содержит префикс event., используем его как есть
        String fullKey = key.startsWith("event.") ? key : "event." + key;
        
        try {
            String message = messageSource.getMessage(fullKey, null, locale);
            log.debug("Found message for key: {} in locale: {} -> {}", fullKey, locale, message);
            return message;
        } catch (Exception e) {
            // Если сообщение не найдено, возвращаем fallback значения
            log.warn("Message not found for key: {} in locale: {}", fullKey, locale);
            
            // Fallback значения для основных ключей
            return switch (key) {
                case "hour_before.title" -> switch (lang.toLowerCase()) {
                    case "ru" -> "Напоминание о событии";
                    case "en" -> "Event Reminder";
                    case "uz" -> "Tadbir eslatmasi";
                    default -> "Event Reminder";
                };
                case "hour_before.message" -> switch (lang.toLowerCase()) {
                    case "ru" -> "Событие \"{0}\" начнется через час!";
                    case "en" -> "Event \"{0}\" starts in 1 hour!";
                    case "uz" -> "\"{0}\" tadbiri 1 soatdan keyin boshlanadi!";
                    default -> "Event \"{0}\" starts in 1 hour!";
                };
                case "hour_before.notification_type" -> "EVENT_HOUR_BEFORE";
                default -> key;
            };
        }
    }
} 