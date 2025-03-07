package com.is.events.service;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

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
        return messageSource.getMessage("event." + key, null, locale);
    }
} 