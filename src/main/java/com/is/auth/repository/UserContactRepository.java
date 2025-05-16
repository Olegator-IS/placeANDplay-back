package com.is.auth.repository;

import com.is.auth.model.user.UserContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserContactRepository extends JpaRepository<UserContact, Long> {
    UserContact findByUserId(Long userId);
} 