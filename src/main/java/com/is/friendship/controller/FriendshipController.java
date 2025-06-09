package com.is.friendship.controller;

import com.is.friendship.dto.FriendshipListResponse;
import com.is.friendship.dto.FriendshipRequest;
import com.is.friendship.dto.FriendshipResponse;
import com.is.friendship.service.FriendshipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/friendships")
@Tag(name = "Friendship", description = "Friendship management APIs")
public class FriendshipController {

    private final FriendshipService friendshipService;

    @PostMapping("/requests")
    @Operation(summary = "Send a friend request", description = "Send a friend request to another user")
    public ResponseEntity<FriendshipResponse> sendFriendRequest(
            @RequestHeader Long userId,
            @Valid @RequestBody FriendshipRequest request) {
        log.info("User {} is sending a friend request to user {}", userId, request.getFriendId());
        return ResponseEntity.ok(friendshipService.sendFriendRequest(userId, request.getFriendId()));
    }

    @PostMapping("/requests/{friendshipId}/accept")
    @Operation(summary = "Accept a friend request", description = "Accept an incoming friend request")
    public ResponseEntity<FriendshipResponse> acceptFriendRequest(
            @RequestHeader Long userId,
            @PathVariable Long friendshipId) {
        log.info("User {} is accepting friend request {}", userId, friendshipId);
        return ResponseEntity.ok(friendshipService.acceptFriendRequest(userId, friendshipId));
    }

    @PostMapping("/requests/{friendshipId}/reject")
    @Operation(summary = "Reject a friend request", description = "Reject an incoming friend request")
    public ResponseEntity<FriendshipResponse> rejectFriendRequest(
            @RequestHeader Long userId,
            @PathVariable Long friendshipId) {
        log.info("User {} is rejecting friend request {}", userId, friendshipId);
        return ResponseEntity.ok(friendshipService.rejectFriendRequest(userId, friendshipId));
    }

    @PostMapping("/block")
    @Operation(summary = "Block a user", description = "Block another user")
    public ResponseEntity<FriendshipResponse> blockUser(
            @RequestHeader Long userId,
            @Valid @RequestBody FriendshipRequest request) {
        log.info("User {} is blocking user {}", userId, request.getFriendId());
        return ResponseEntity.ok(friendshipService.blockUser(userId, request.getFriendId()));
    }

    @DeleteMapping("/block/{blockedUserId}")
    @Operation(summary = "Unblock a user", description = "Unblock a previously blocked user")
    public ResponseEntity<FriendshipResponse> unblockUser(
            @RequestHeader Long userId,
            @PathVariable Long blockedUserId) {
        log.info("User {} is unblocking user {}", userId, blockedUserId);
        return ResponseEntity.ok(friendshipService.unblockUser(userId, blockedUserId));
    }

    @DeleteMapping("/{friendId}")
    @Operation(summary = "Remove a friend", description = "Remove a user from friends list")
    public ResponseEntity<Void> removeFriend(
            @RequestHeader Long userId,
            @PathVariable Long friendId) {
        log.info("User {} is removing user {} from friends", userId, friendId);
        friendshipService.removeFriend(userId, friendId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/friends")
    @Operation(summary = "Get friends list", description = "Get a paginated list of user's friends")
    public ResponseEntity<FriendshipListResponse> getFriends(
            @RequestHeader Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        log.info("User {} is retrieving friends list", userId);
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        return ResponseEntity.ok(friendshipService.getFriends(userId, PageRequest.of(page, size, sort)));
    }

    @GetMapping("/requests/incoming")
    @Operation(summary = "Get incoming requests", description = "Get a paginated list of incoming friend requests")
    public ResponseEntity<FriendshipListResponse> getIncomingRequests(
            @RequestHeader Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        log.info("User {} is retrieving incoming friend requests", userId);
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        return ResponseEntity.ok(friendshipService.getIncomingRequests(userId, PageRequest.of(page, size, sort)));
    }

    @GetMapping("/requests/outgoing")
    @Operation(summary = "Get outgoing requests", description = "Get a paginated list of outgoing friend requests")
    public ResponseEntity<FriendshipListResponse> getOutgoingRequests(
            @RequestHeader Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        log.info("User {} is retrieving outgoing friend requests", userId);
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        return ResponseEntity.ok(friendshipService.getOutgoingRequests(userId, PageRequest.of(page, size, sort)));
    }

    @GetMapping("/blocked")
    @Operation(summary = "Get blocked users", description = "Get a paginated list of blocked users")
    public ResponseEntity<FriendshipListResponse> getBlockedUsers(
            @RequestHeader Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        log.info("User {} is retrieving blocked users list", userId);
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        return ResponseEntity.ok(friendshipService.getBlockedUsers(userId, PageRequest.of(page, size, sort)));
    }
} 