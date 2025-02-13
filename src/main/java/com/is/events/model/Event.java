package com.is.events.model;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "events", schema = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "place_id")
    private Long placeId;

    @Column(name = "sport_type_id")
    private Long sportTypeId;

    @Column(name = "organizer_id")
    private Long organizerId;

    @Column(name = "current_participants")
    private String currentParticipants;

    private String status;
    private String description;

    @Column(name = "skill_level")
    private String skillLevel;

    @Column(name = "date_time")
    private LocalDateTime dateTime;

}
