package com.is.auth.service;

import com.is.auth.repository.EmailVerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExpiredCodeCleanupTask {

    private final EmailVerificationCodeRepository verificationCodeRepository;

    @Scheduled(fixedRate = 600_000) // Каждые 10 минут
    public void cleanUpExpiredCodes() {
        verificationCodeRepository.deleteExpiredCodes();
    }
}