package com.is.events.model;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.vladmihalcea.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;
import lombok.*;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@TypeDefs({
        @TypeDef(name = "json", typeClass = JsonType.class)
})
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

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Type(type = "json")
    @Column(name = "sport_event", columnDefinition = "jsonb")
    private SportEvent sportEvent;

    @Type(type = "json")
    @Column(name = "organizer_event", columnDefinition = "jsonb")
    private OrganizerEvent organizerEvent;

    @Column(name = "date_time")
    private LocalDateTime dateTime;

    @Type(type = "json")
    @Column(name = "current_participants", columnDefinition = "jsonb")
    @Builder.Default
    private CurrentParticipants currentParticipants = new CurrentParticipants();

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "skill_level", length = 20)
    private String skillLevel;

    private boolean isFirstTimeEventCreation;
}

