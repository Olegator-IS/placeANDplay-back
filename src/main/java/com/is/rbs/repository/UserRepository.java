package com.is.rbs.repository;

import com.is.rbs.model.user.User;
import com.is.rbs.model.user.UserAdditionalInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    User getUserInfoByEmail(String email);
}


