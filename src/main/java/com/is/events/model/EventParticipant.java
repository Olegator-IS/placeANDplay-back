package com.is.events.model;

import com.is.auth.model.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventParticipant {
    private Event event;
    private User user;
} 