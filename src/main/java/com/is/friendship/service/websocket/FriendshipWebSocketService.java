package com.is.friendship.service.websocket;

import com.is.friendship.model.Friendship;
import com.is.friendship.model.enums.FriendshipStatus;
import com.is.friendship.websocket.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendshipWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendFriendRequestNotification(Friendship friendship) {
        FriendRequestNotification notification = FriendRequestNotification.builder()
            .friendshipId(friendship.getId())
            .senderId(friendship.getInitiator().getUserId())
            .receiverId(friendship.getUser2().getUserId())
            .status(friendship.getStatus())
            .build();

        messagingTemplate.convertAndSendToUser(
            friendship.getUser2().getUserId().toString(),
            "/queue/friend-requests",
            notification
        );
    }

    public void sendFriendRequestAcceptedNotification(Friendship friendship) {
        FriendRequestAcceptedNotification notification = FriendRequestAcceptedNotification.builder()
            .friendshipId(friendship.getId())
            .senderId(friendship.getUser2().getUserId())
            .receiverId(friendship.getInitiator().getUserId())
            .status(friendship.getStatus())
            .build();

        messagingTemplate.convertAndSendToUser(
            friendship.getInitiator().getUserId().toString(),
            "/queue/friend-requests/accepted",
            notification
        );
    }

    public void sendFriendRequestRejectedNotification(Friendship friendship) {
        FriendRequestRejectedNotification notification = FriendRequestRejectedNotification.builder()
            .friendshipId(friendship.getId())
            .senderId(friendship.getUser2().getUserId())
            .receiverId(friendship.getInitiator().getUserId())
            .status(friendship.getStatus())
            .build();

        messagingTemplate.convertAndSendToUser(
            friendship.getInitiator().getUserId().toString(),
            "/queue/friend-requests/rejected",
            notification
        );
    }

    public void sendFriendRemovedNotification(Friendship friendship) {
        FriendRemovedNotification notification = FriendRemovedNotification.builder()
            .friendshipId(friendship.getId())
            .senderId(friendship.getUser1().getUserId())
            .receiverId(friendship.getUser2().getUserId())
            .status(friendship.getStatus())
            .build();

        messagingTemplate.convertAndSendToUser(
            friendship.getUser2().getUserId().toString(),
            "/queue/friends/removed",
            notification
        );
    }

    public void sendUserBlockedNotification(Friendship friendship) {
        UserBlockedNotification notification = UserBlockedNotification.builder()
            .friendshipId(friendship.getId())
            .senderId(friendship.getInitiator().getUserId())
            .receiverId(friendship.getUser2().getUserId())
            .status(friendship.getStatus())
            .build();

        messagingTemplate.convertAndSendToUser(
            friendship.getUser2().getUserId().toString(),
            "/queue/users/blocked",
            notification
        );
    }

    public void sendUserUnblockedNotification(Friendship friendship) {
        UserUnblockedNotification notification = UserUnblockedNotification.builder()
            .friendshipId(friendship.getId())
            .senderId(friendship.getInitiator().getUserId())
            .receiverId(friendship.getUser2().getUserId())
            .status(friendship.getStatus())
            .build();

        messagingTemplate.convertAndSendToUser(
            friendship.getUser2().getUserId().toString(),
            "/queue/users/unblocked",
            notification
        );
    }
} 