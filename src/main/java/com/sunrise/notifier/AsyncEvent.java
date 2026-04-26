package com.sunrise.notifier;

import org.springframework.lang.NonNull;

import com.sunrise.service.creation.CreateLoginHistoryDTO;
import com.sunrise.service.creation.CreateVerificationTokenDTO;

public final class AsyncEvent {
    private AsyncEvent() {}

    public record SaveUserLoginHistory(@NonNull String username, @NonNull CreateLoginHistoryDTO loginHistory) {}
    public record SaveVerificationToken(@NonNull CreateVerificationTokenDTO verificationToken) {}
    public record DeleteVerificationToken(@NonNull String token) {}
}