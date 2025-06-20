package com.is.friendship.service;

import com.is.auth.model.user.User;
import com.is.auth.model.user.UserAdditionalInfo;
import com.is.auth.repository.UserRepository;
import com.is.auth.repository.UserAdditionalInfoRepository;
import com.is.auth.service.PushNotificationService;
import com.is.friendship.dto.FriendshipListResponse;
import com.is.friendship.dto.FriendshipRequest;
import com.is.friendship.dto.FriendshipResponse;
import com.is.friendship.exception.FriendshipException;
import com.is.friendship.model.Friendship;
import com.is.friendship.model.enums.FriendshipStatus;
import com.is.friendship.repository.FriendshipRepository;
import com.is.friendship.service.websocket.FriendshipWebSocketService;
import com.is.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final UserAdditionalInfoRepository userAdditionalInfoRepository;
    private final FriendshipWebSocketService webSocketService;
    private final PushNotificationService pushNotificationService;
    private final NotificationService notificationService;

    @Transactional
    public FriendshipResponse sendFriendRequest(Long currentId, Long friendId) {
        User currentUser = userRepository.findById(currentId)
            .orElseThrow(() -> new FriendshipException(FriendshipException.ErrorType.USER_NOT_FOUND));
        
        User friend = userRepository.findById(friendId)
            .orElseThrow(() -> new FriendshipException(FriendshipException.ErrorType.FRIEND_NOT_FOUND));

        if (currentId.equals(friendId)) {
            throw new FriendshipException(FriendshipException.ErrorType.INVALID_OPERATION, "Cannot send friend request to yourself");
        }

        // Проверяем существующую дружбу
        Optional<Friendship> existingFriendship = friendshipRepository.findByUsers(currentUser, friend);
        
        if (existingFriendship.isPresent()) {
            Friendship friendship = existingFriendship.get();
            // Если дружба уже существует и не отклонена, выбрасываем исключение
            if (friendship.getStatus() != FriendshipStatus.REJECTED) {
                throw new FriendshipException(FriendshipException.ErrorType.FRIENDSHIP_ALREADY_EXISTS);
            }
            // Если дружба была отклонена, удаляем её
            friendshipRepository.delete(friendship);
        }

        // Check if friend has blocked the current user
        if (friendshipRepository.existsByUsersAndStatus(friend, currentUser, FriendshipStatus.BLOCKED)) {
            throw new FriendshipException(FriendshipException.ErrorType.USER_BLOCKED);
        }

        // Создаем новую дружбу
        Friendship friendship = Friendship.builder()
            .user1(currentUser)
            .user2(friend)
            .initiator(currentUser)
            .status(FriendshipStatus.PENDING)
            .build();

        friendship = friendshipRepository.save(friendship);
        webSocketService.sendFriendRequestNotification(friendship);
        // PUSH: уведомление получателю
        String title = "Заявка в друзья";
        String body = String.format("🚀 %s %s отправил вам заявку в друзья. Не упустите шанс завести новое знакомство!", currentUser.getFirstName(), currentUser.getLastName());
        pushNotificationService.sendSimpleNotification(friend.getUserId(), title, body, "FRIEND_REQUEST");
        // История уведомлений
        notificationService.createNotification(friend.getUserId(), "FRIEND_REQUEST", title, body, null);
        return convertToResponse(friendship, currentId, FriendshipResponseType.FRIEND);
    }

    @Transactional
    public FriendshipResponse acceptFriendRequest(Long userId, Long friendshipId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new FriendshipException(FriendshipException.ErrorType.USER_NOT_FOUND));

        Friendship friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new FriendshipException(FriendshipException.ErrorType.FRIENDSHIP_NOT_FOUND));

        if (!friendship.getUser2().equals(user)) {
            throw new FriendshipException(FriendshipException.ErrorType.NOT_AUTHORIZED);
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new FriendshipException(FriendshipException.ErrorType.FRIENDSHIP_NOT_PENDING);
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendship = friendshipRepository.save(friendship);
        webSocketService.sendFriendRequestAcceptedNotification(friendship);
        // PUSH: уведомление инициатору
        User initiator = friendship.getInitiator();
        String title = "Заявка принята";
        String body = String.format("🥳 Ура! %s %s теперь в вашем списке друзей. Настало время для новых совместных активностей", user.getFirstName(), user.getLastName());
        pushNotificationService.sendSimpleNotification(initiator.getUserId(), title, body, "FRIEND_REQUEST_ACCEPTED");
        // История уведомлений
        notificationService.createNotification(initiator.getUserId(), "FRIEND_REQUEST_ACCEPTED", title, body, null);
        return convertToResponse(friendship, userId, FriendshipResponseType.FRIEND);
    }

    @Transactional
    public FriendshipResponse rejectFriendRequest(Long userId, Long friendshipId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new FriendshipException(FriendshipException.ErrorType.USER_NOT_FOUND));

        Friendship friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new FriendshipException(FriendshipException.ErrorType.FRIENDSHIP_NOT_FOUND));

        if (!friendship.getUser2().equals(user)) {
            throw new FriendshipException(FriendshipException.ErrorType.NOT_AUTHORIZED);
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new FriendshipException(FriendshipException.ErrorType.FRIENDSHIP_NOT_PENDING);
        }

        friendship.setStatus(FriendshipStatus.REJECTED);
        friendship = friendshipRepository.save(friendship);
        webSocketService.sendFriendRequestRejectedNotification(friendship);

        return convertToResponse(friendship, userId, FriendshipResponseType.FRIEND);
    }

    @Transactional
    public FriendshipResponse blockUser(Long userId, Long userToBlockId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new FriendshipException(FriendshipException.ErrorType.USER_NOT_FOUND));
        
        User userToBlock = userRepository.findById(userToBlockId)
            .orElseThrow(() -> new FriendshipException(FriendshipException.ErrorType.USER_NOT_FOUND));

        if (userId.equals(userToBlockId)) {
            throw new FriendshipException(FriendshipException.ErrorType.INVALID_OPERATION, "Cannot block yourself");
        }

        Friendship friendship = friendshipRepository.findByUsers(user, userToBlock)
            .orElseGet(() -> Friendship.builder()
                .user1(user)
                .user2(userToBlock)
                .initiator(user)
                .build());

        friendship.setStatus(FriendshipStatus.BLOCKED);
        friendship = friendshipRepository.save(friendship);
        webSocketService.sendUserBlockedNotification(friendship);

        return convertToResponse(friendship, userId, FriendshipResponseType.FRIEND);
    }

    @Transactional
    public FriendshipResponse unblockUser(Long userId, Long blockedUserId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new FriendshipException(FriendshipException.ErrorType.USER_NOT_FOUND));

        Friendship friendship = friendshipRepository.findByUsersAndStatus(
                user,
                userRepository.findById(blockedUserId)
                    .orElseThrow(() -> new FriendshipException(FriendshipException.ErrorType.USER_NOT_FOUND)),
                FriendshipStatus.BLOCKED)
            .orElseThrow(() -> new FriendshipException(FriendshipException.ErrorType.BLOCKED_FRIENDSHIP_NOT_FOUND));

        friendshipRepository.delete(friendship);
        webSocketService.sendUserUnblockedNotification(friendship);

        return convertToResponse(friendship, userId, FriendshipResponseType.FRIEND);
    }

    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new FriendshipException(FriendshipException.ErrorType.USER_NOT_FOUND));
        
        User friend = userRepository.findById(friendId)
            .orElseThrow(() -> new FriendshipException(FriendshipException.ErrorType.FRIEND_NOT_FOUND));

        Friendship friendship = friendshipRepository.findByUsersAndStatus(user, friend, FriendshipStatus.ACCEPTED)
            .orElseThrow(() -> new FriendshipException(FriendshipException.ErrorType.FRIENDSHIP_NOT_FOUND));

        friendshipRepository.delete(friendship);
        webSocketService.sendFriendRemovedNotification(friendship);
    }

    public enum FriendshipResponseType {
        INCOMING, OUTGOING, FRIEND
    }

    public FriendshipListResponse getFriends(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new FriendshipException(FriendshipException.ErrorType.USER_NOT_FOUND));

        Page<Friendship> friendships = friendshipRepository.findByUserAndStatus(user, FriendshipStatus.ACCEPTED, pageable);
        List<FriendshipResponse> responses = friendships.getContent().stream()
            .map(f -> convertToResponse(f, userId, FriendshipResponseType.FRIEND))
            .collect(Collectors.toList());

        return FriendshipListResponse.builder()
            .friendships(responses)
            .totalCount(friendships.getTotalElements())
            .build();
    }

    public FriendshipListResponse getIncomingRequests(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new FriendshipException(FriendshipException.ErrorType.USER_NOT_FOUND));

        Page<Friendship> friendships = friendshipRepository.findIncomingRequests(user, FriendshipStatus.PENDING, pageable);
        List<FriendshipResponse> responses = friendships.getContent().stream()
            .map(f -> convertToResponse(f, userId, FriendshipResponseType.INCOMING))
            .collect(Collectors.toList());

        return FriendshipListResponse.builder()
            .friendships(responses)
            .totalCount(friendships.getTotalElements())
            .build();
    }

    public FriendshipListResponse getOutgoingRequests(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new FriendshipException(FriendshipException.ErrorType.USER_NOT_FOUND));

        Page<Friendship> friendships = friendshipRepository.findByInitiatorAndStatus(user, FriendshipStatus.PENDING, pageable);
        List<FriendshipResponse> responses = friendships.getContent().stream()
            .map(f -> convertToResponse(f, userId, FriendshipResponseType.OUTGOING))
            .collect(Collectors.toList());

        return FriendshipListResponse.builder()
            .friendships(responses)
            .totalCount(friendships.getTotalElements())
            .build();
    }

    public FriendshipListResponse getBlockedUsers(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new FriendshipException(FriendshipException.ErrorType.USER_NOT_FOUND));

        Page<Friendship> friendships = friendshipRepository.findByUserAndStatus(user, FriendshipStatus.BLOCKED, pageable);
        List<FriendshipResponse> responses = friendships.getContent().stream()
            .map(f -> convertToResponse(f, userId, FriendshipResponseType.FRIEND))
            .collect(Collectors.toList());

        return FriendshipListResponse.builder()
            .friendships(responses)
            .totalCount(friendships.getTotalElements())
            .build();
    }

    private FriendshipResponse convertToResponse(Friendship friendship, Long currentUserId, FriendshipResponseType type) {
        User targetUser;
        Long friendId;
        String firstName;
        String lastName;
        String profilePictureUrl;

        switch (type) {
            case INCOMING:
                // Показываем отправителя (initiator или user1)
                targetUser = friendship.getUser1();
                break;
            case OUTGOING:
                // Показываем получателя (user2)
                targetUser = friendship.getUser2();
                break;
            case FRIEND:
            default:
                // Для друзей — тот, кто не currentUser
                if (friendship.getUser1().getUserId().equals(currentUserId)) {
                    targetUser = friendship.getUser2();
                } else {
                    targetUser = friendship.getUser1();
                }
                break;
        }
        friendId = targetUser.getUserId();
        firstName = targetUser.getFirstName();
        lastName = targetUser.getLastName();
        UserAdditionalInfo info = userAdditionalInfoRepository.findById(friendId)
            .orElse(new UserAdditionalInfo());
        profilePictureUrl = info.getProfilePictureUrl();

        return FriendshipResponse.builder()
            .id(friendship.getId())
            .userId(currentUserId)
            .friendId(friendId)
            .firstName(firstName)
            .lastName(lastName)
            .profilePictureUrl(profilePictureUrl)
            .status(friendship.getStatus())
            .createdAt(friendship.getCreatedAt())
            .updatedAt(friendship.getUpdatedAt())
            .build();
    }

    public boolean isFriend(Long userId, Long otherUserId) {
        User user = userRepository.findById(userId)
            .orElse(null);
        User other = userRepository.findById(otherUserId)
            .orElse(null);
        if (user == null || other == null) return false;
        return friendshipRepository.findByUsersAndStatus(user, other, FriendshipStatus.ACCEPTED).isPresent();
    }

    public String getFriendshipStatus(Long userId, Long otherUserId) {
        User user = userRepository.findById(userId).orElse(null);
        User other = userRepository.findById(otherUserId).orElse(null);
        if (user == null || other == null) return "not_friend";

        // Проверка на блокировку
        if (friendshipRepository.findByUsersAndStatus(user, other, com.is.friendship.model.enums.FriendshipStatus.BLOCKED).isPresent() ||
            friendshipRepository.findByUsersAndStatus(other, user, com.is.friendship.model.enums.FriendshipStatus.BLOCKED).isPresent()) {
            return "blocked";
        }
        // Проверка на дружбу
        if (friendshipRepository.findByUsersAndStatus(user, other, com.is.friendship.model.enums.FriendshipStatus.ACCEPTED).isPresent()) {
            return "already_friend";
        }
        // Проверка на pending (ожидание)
        if (friendshipRepository.findByUsersAndStatus(user, other, com.is.friendship.model.enums.FriendshipStatus.PENDING).isPresent() ||
            friendshipRepository.findByUsersAndStatus(other, user, com.is.friendship.model.enums.FriendshipStatus.PENDING).isPresent()) {
            return "pending_request";
        }
        return "not_friend";
    }
} 