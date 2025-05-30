package com.is.friendship.exception;

public class FriendshipException extends RuntimeException {

    public enum ErrorType {
        FRIENDSHIP_ALREADY_EXISTS("Friendship already exists", 409),
        USER_NOT_FOUND("User not found", 404),
        FRIEND_NOT_FOUND("Friend not found", 404),
        USER_BLOCKED("User is blocked", 403),
        NOT_AUTHORIZED("Not authorized to perform this action", 403),
        FRIENDSHIP_NOT_FOUND("Friendship not found", 404),
        FRIENDSHIP_NOT_PENDING("Friendship is not in pending status", 400),
        BLOCKED_FRIENDSHIP_NOT_FOUND("Blocked friendship not found", 404),
        INVALID_OPERATION("Invalid operation", 400);

        private final String message;
        private final int status;

        ErrorType(String message, int status) {
            this.message = message;
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public int getStatus() {
            return status;
        }
    }

    private final ErrorType errorType;

    public FriendshipException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
    }

    public FriendshipException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public FriendshipException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
} 