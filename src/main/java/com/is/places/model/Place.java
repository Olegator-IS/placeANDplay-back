package com.is.places.model;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "places", schema = "locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Place {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id")
    private Long placeId;

    private String name; // Название площадки

    private String address; // Адрес

    private Double latitude;  // Широта

    private Double longitude; // Долгота

    @Column(name = "type_id")
    private int typeId; // Вид спорта (например, баскетбол это 3,теннис это 1)

    private Boolean verified = false; // Подтверждена ли площадка

    private String description;

    private String phone;
    @Column(name = "current_location_city_id")
    private int currentLocationCityId;
    @Column(name = "current_location_country_id")
    private int currentLocationCountryId;
    private Long orgId;
}
