package com.is.events.model;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class EventFilterDTO {
    private String placeId;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFrom;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateTo;

    private String status;
    private String eventType;
    private Double priceMin;
    private Double priceMax;
    private Integer availableSpots;
    private String search;

    // Параметры пагинации
    private int page = 0;
    private int size = 10;
    private String sortBy = "dateTime";
    private String sortDirection = "DESC";

    public LocalDateTime getDateTimeFrom() {
        return dateFrom != null ? dateFrom.atStartOfDay() : null;
    }

    public LocalDateTime getDateTimeTo() {
        return dateTo != null ? dateTo.atTime(LocalTime.MAX) : null;
    }
}
