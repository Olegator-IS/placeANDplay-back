package com.is.events.model;

import org.springframework.data.jpa.domain.Specification;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class EventSpecification {

    public static Specification<Event> withFilters(EventFilterDTO filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Фильтр по площадке
            if (filter.getPlaceId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("placeId"), filter.getPlaceId()));
            }

            // Фильтр по датам
            if (filter.getDateFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dateTime"), filter.getDateTimeFrom()));
            }
            if (filter.getDateTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateTime"), filter.getDateTimeTo()));
            }

            // Фильтр по статусу
            if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }

            // Фильтр по типу события
            if (filter.getEventType() != null && !filter.getEventType().isEmpty()) {
                Expression<String> sportType = criteriaBuilder.function(
                    "jsonb_extract_path_text",
                    String.class,
                    root.get("sportEvent"),
                    criteriaBuilder.literal("sportType")
                );
                predicates.add(criteriaBuilder.equal(sportType, filter.getEventType()));
            }

            // Фильтр по цене
            if (filter.getPriceMin() != null) {
                Expression<Double> price = criteriaBuilder.function(
                    "jsonb_extract_path_text",
                    Double.class,
                    root.get("sportEvent"),
                    criteriaBuilder.literal("price")
                );
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    criteriaBuilder.coalesce(price, 0.0),
                    filter.getPriceMin()
                ));
            }
            if (filter.getPriceMax() != null) {
                Expression<Double> price = criteriaBuilder.function(
                    "jsonb_extract_path_text",
                    Double.class,
                    root.get("sportEvent"),
                    criteriaBuilder.literal("price")
                );
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    criteriaBuilder.coalesce(price, Double.MAX_VALUE),
                    filter.getPriceMax()
                ));
            }

            // Фильтр по количеству свободных мест
            if (filter.getAvailableSpots() != null) {
                Expression<Integer> maxParticipants = criteriaBuilder.function(
                    "jsonb_extract_path_text",
                    Integer.class,
                    root.get("sportEvent"),
                    criteriaBuilder.literal("maxParticipants")
                );
                Expression<Integer> currentSize = criteriaBuilder.function(
                    "jsonb_extract_path_text",
                    Integer.class,
                    root.get("currentParticipants"),
                    criteriaBuilder.literal("size")
                );
                predicates.add(
                    criteriaBuilder.greaterThanOrEqualTo(
                        criteriaBuilder.diff(
                            criteriaBuilder.coalesce(maxParticipants, 0),
                            criteriaBuilder.coalesce(currentSize, 0)
                        ),
                        filter.getAvailableSpots()
                    )
                );
            }

            // Поиск по названию и описанию
            if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                String searchPattern = "%" + filter.getSearch().toLowerCase() + "%";
                Expression<String> title = criteriaBuilder.function(
                    "jsonb_extract_path_text",
                    String.class,
                    root.get("sportEvent"),
                    criteriaBuilder.literal("title")
                );
                predicates.add(
                    criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(title), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchPattern)
                    )
                );
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}