package com.sunrise.notifier;

import org.springframework.lang.NonNull;

import com.sunrise.core.creation.CreateLoginHistoryDTO;
import com.sunrise.core.creation.CreateVerificationTokenDTO;

public final class AsyncEvent {
    private AsyncEvent() {}

    public record SaveUserLoginHistory(@NonNull String username, @NonNull CreateLoginHistoryDTO loginHistory) {}
    public record SaveVerificationToken(@NonNull CreateVerificationTokenDTO verificationToken) {}
    public record DeleteVerificationToken(@NonNull String token) {}
}