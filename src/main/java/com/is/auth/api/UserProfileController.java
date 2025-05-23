package com.is.auth.api;

import com.is.auth.model.ResponseAnswers.Response;
import com.is.auth.model.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<Response> getUserProfile(
            @PathVariable Long userId,
            @RequestHeader(value = "Accept-Language", defaultValue = "ru") String language) {
        return userService.getUserProfile(userId, language);
    }
} 