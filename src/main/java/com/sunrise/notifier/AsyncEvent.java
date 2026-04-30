package com.sunrise.notifier;

import org.springframework.lang.NonNull;

import com.sunrise.core.creation.CreateDto.*;

public final class AsyncEvent {

    public record SaveUserLoginHistory(
        @NonNull String username, 
        @NonNull LoginHistory loginHistory
    ) {}

    public record SaveVerificationToken(
        @NonNull VerificationToken verificationToken
    ) {}

    public record DeleteVerificationToken(
        @NonNull String token
    ) {}
}