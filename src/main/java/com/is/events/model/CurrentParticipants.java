package com.is.events.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CurrentParticipants implements Serializable {
    @JsonProperty("size")
    private int size = 0;

    @JsonProperty("participants")
    private List<Participant> participants = new ArrayList<>();

    @Data
    public static class Participant implements Serializable {
        @JsonProperty("participantId")
        private Long participantId;

        @JsonProperty("participantName")
        private String participantName;

        @JsonProperty("status")
        private String status;

        @JsonProperty("joinedAt")
        private LocalDateTime joinedAt;

        @JsonProperty("checkedInAt")
        private LocalDateTime checkedInAt;

        public Participant() {}

        public Participant(Long id, String name, String status, LocalDateTime joinedAt) {
            this.participantId = id;
            this.participantName = name;
            this.status = status;
            this.joinedAt = joinedAt;
            this.checkedInAt = null;
        }

        public Participant(Long id, String name, String status, LocalDateTime joinedAt, LocalDateTime checkedInAt) {
            this.participantId = id;
            this.participantName = name;
            this.status = status;
            this.joinedAt = joinedAt;
            this.checkedInAt = checkedInAt;
        }
    }

    public void addParticipant(Long id, String name) {
        if (participants == null) {
            participants = new ArrayList<>();
        }
        participants.add(new Participant(id, name, "ACTIVE", LocalDateTime.now()));
        size = participants.size();
    }

    public void checkInParticipant(Long participantId) {
        if (participants != null) {
            participants.stream()
                    .filter(p -> p.getParticipantId().equals(participantId))
                    .findFirst()
                    .ifPresent(participant -> {
                        participant.setStatus("PRESENT");
                        participant.setCheckedInAt(LocalDateTime.now());
                    });
        }
    }

    public boolean hasParticipant(Long participantId) {
        if (participants == null) return false;
        return participants.stream()
                .anyMatch(p -> p.getParticipantId().equals(participantId));
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void removeParticipant(Long participantId) {
        if (participants != null) {
            participants.removeIf(p -> p.getParticipantId().equals(participantId));
            size = participants.size();
        }
    }

    public String getParticipantName(Long participantId) {
        return participants.stream()
                .filter(p -> p.getParticipantId().equals(participantId))
                .map(Participant::getParticipantName)
                .findFirst()
                .orElse("Unknown Participant");
    }
}