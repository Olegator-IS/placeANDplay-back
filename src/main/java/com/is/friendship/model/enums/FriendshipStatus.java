package com.is.friendship.model.enums;

/**
 * Represents the possible statuses of a friendship relationship
 */
public enum FriendshipStatus {
    /**
     * Initial state when a friend request is sent
     */
    PENDING,

    /**
     * Friend request has been accepted
     */
    ACCEPTED,

    /**
     * Friend request has been rejected
     */
    REJECTED,

    /**
     * User has been blocked
     */
    BLOCKED
} 