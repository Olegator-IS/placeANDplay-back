package com.is.events.model;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_messages", schema = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "sender_avatar_url")
    private String senderAvatarUrl;
} 