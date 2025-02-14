package com.is.events.model;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.vladmihalcea.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;
import lombok.*;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    @Column(name = "place_id")
    private Long placeId;

    @Type(type = "json")
    @Column(name = "sport_event", columnDefinition = "jsonb")
    private SportEvent sportEvent;

    @Type(type = "json")
    @Column(name = "organizer_event", columnDefinition = "jsonb")
    private OrganizerEvent organizerEvent;

    @Type(type = "json")
    @Column(name = "current_participants", columnDefinition = "jsonb")
    private List<CurrentParticipants> currentParticipants;


    private String status;
    private String description;

    @Column(name = "skill_level")
    private String skillLevel;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "date_time")
    private LocalDateTime dateTime;
}

