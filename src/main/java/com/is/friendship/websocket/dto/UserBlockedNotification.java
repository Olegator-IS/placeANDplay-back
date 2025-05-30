package com.is.friendship.websocket.dto;

import com.is.friendship.model.enums.FriendshipStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBlockedNotification {
    private Long friendshipId;
    private Long senderId;
    private Long receiverId;
    private FriendshipStatus status;
} 