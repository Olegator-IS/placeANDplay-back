package com.is.friendship.repository;

import com.is.auth.model.user.User;
import com.is.friendship.model.Friendship;
import com.is.friendship.model.enums.FriendshipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    
    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.user1 = :user1 AND f.user2 = :user2) OR " +
           "(f.user1 = :user2 AND f.user2 = :user1)")
    Optional<Friendship> findByUsers(@Param("user1") User user1, @Param("user2") User user2);
    
    @Query("SELECT f FROM Friendship f WHERE " +
           "((f.user1 = :user1 AND f.user2 = :user2) OR " +
           "(f.user1 = :user2 AND f.user2 = :user1)) AND " +
           "f.status = :status")
    Optional<Friendship> findByUsersAndStatus(
        @Param("user1") User user1, 
        @Param("user2") User user2, 
        @Param("status") FriendshipStatus status
    );
    
    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.user1 = :user OR f.user2 = :user) AND " +
           "f.status = :status")
    List<Friendship> findByUserAndStatus(@Param("user") User user, @Param("status") FriendshipStatus status);
    
    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.user1 = :user OR f.user2 = :user) AND " +
           "f.status = :status")
    Page<Friendship> findByUserAndStatus(
        @Param("user") User user, 
        @Param("status") FriendshipStatus status, 
        Pageable pageable
    );
    
    @Query("SELECT COUNT(f) > 0 FROM Friendship f WHERE " +
           "((f.user1 = :user1 AND f.user2 = :user2) OR " +
           "(f.user1 = :user2 AND f.user2 = :user1)) AND " +
           "f.status = :status")
    boolean existsByUsersAndStatus(
        @Param("user1") User user1, 
        @Param("user2") User user2, 
        @Param("status") FriendshipStatus status
    );
    
    @Query("SELECT f FROM Friendship f WHERE " +
           "f.initiator = :user AND " +
           "f.status = :status")
    List<Friendship> findByInitiatorAndStatus(
        @Param("user") User user, 
        @Param("status") FriendshipStatus status
    );
    
    @Query("SELECT f FROM Friendship f WHERE " +
           "f.initiator = :user AND " +
           "f.status = :status")
    Page<Friendship> findByInitiatorAndStatus(
        @Param("user") User user, 
        @Param("status") FriendshipStatus status, 
        Pageable pageable
    );
    
    @Query("DELETE FROM Friendship f WHERE " +
           "(f.user1 = :user1 AND f.user2 = :user2) OR " +
           "(f.user1 = :user2 AND f.user2 = :user1)")
    void deleteByUsers(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT f FROM Friendship f WHERE " +
           "f.user2 = :user AND " +
           "f.status = :status")
    Page<Friendship> findIncomingRequests(
        @Param("user") User user, 
        @Param("status") FriendshipStatus status, 
        Pageable pageable
    );
} 