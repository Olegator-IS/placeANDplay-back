package com.is.auth.model.enums;

import lombok.Getter;

@Getter
public enum Language {
    RU("ru", "name_ru"),
    EN("en", "name_en"),
    UZ("uz", "name_uz");

    private final String code;
    private final String columnName;

    Language(String code, String columnName) {
        this.code = code;
        this.columnName = columnName;
    }

    public static String getColumnNameByCode(String code) {
        for (Language language : values()) {
            if (language.getCode().equals(code.toLowerCase())) {
                return language.getColumnName();
            }
        }
        throw new IllegalArgumentException("Unsupported language: " + code);
    }

    public static boolean isSupported(String code) {
        for (Language language : values()) {
            if (language.getCode().equals(code.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
} 