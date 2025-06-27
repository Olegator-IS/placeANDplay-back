package com.is.events.model;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vladmihalcea.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;
import lombok.*;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import com.is.events.model.enums.EventStatus;

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
@JsonIgnoreProperties(ignoreUnknown = true)
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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateTime;

    @Type(type = "json")
    @Column(name = "current_participants", columnDefinition = "jsonb")
    private CurrentParticipants currentParticipants = new CurrentParticipants();

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private EventStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejected_by")
    private Long rejectedBy;

    @Column(name = "requested_changes")
    private String requestedChanges;

    @Column(name = "last_status_change")
    private LocalDateTime lastStatusChange;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "skill_level", length = 20)
    private String skillLevel;

    @Column(name = "additional_info")
    private String additionalInfo;

    private boolean isFirstTimeEventCreation;

    @PrePersist
    @PreUpdate
    protected void onCreate() {
        if (lastStatusChange == null) {
            lastStatusChange = LocalDateTime.now();
        }
    }

    public boolean canTransitionTo(EventStatus newStatus) {
        if (this.status == null) {
            return true;
        }
        return this.status.canTransitionTo(newStatus);
    }

    public void setStatus(EventStatus newStatus) {
        if (this.status == null || canTransitionTo(newStatus)) {
            this.status = newStatus;
            this.lastStatusChange = LocalDateTime.now();
        } else {
            throw new IllegalStateException("Cannot transition from " + status + " to " + newStatus);
        }
    }

    public void reject(String reason, Long rejectedBy) {
        this.setStatus(EventStatus.REJECTED);
        this.rejectionReason = reason;
        this.rejectedBy = rejectedBy;
        this.rejectedAt = LocalDateTime.now();
    }

    public void requestChanges(String changes, Long rejectedBy) {
        this.setStatus(EventStatus.CHANGES_REQUESTED);
        this.requestedChanges = changes;
        this.rejectedBy = rejectedBy;
        this.rejectedAt = LocalDateTime.now();
    }

    public void confirm() {
        this.setStatus(EventStatus.CONFIRMED);
        this.rejectionReason = null;
        this.rejectedAt = null;
        this.rejectedBy = null;
        this.requestedChanges = null;
    }

    public void startEvent() {
        LocalDateTime now = LocalDateTime.now();
        if (this.dateTime.isAfter(now)) {
            throw new IllegalStateException(String.format(
                "Cannot start event before its scheduled time. Event time: %s, Current time: %s", 
                this.dateTime, now));
        }

        if (!this.status.equals(EventStatus.CONFIRMED)) {
            throw new IllegalStateException("Event must be confirmed before it can start");
        }
        
        this.setStatus(EventStatus.IN_PROGRESS);
    }

    public void complete() {
        if (!this.status.equals(EventStatus.IN_PROGRESS)) {
            throw new IllegalStateException("Event must be in progress before it can be completed");
        }
        this.setStatus(EventStatus.COMPLETED);
    }

    public void cancel() {
        if (!this.status.equals(EventStatus.CONFIRMED)) {
            throw new IllegalStateException("Only confirmed events can be cancelled");
        }
        this.setStatus(EventStatus.CANCELLED);
    }

    public void forceExpire() {
        this.status = EventStatus.EXPIRED;
        this.lastStatusChange = LocalDateTime.now();
    }
}