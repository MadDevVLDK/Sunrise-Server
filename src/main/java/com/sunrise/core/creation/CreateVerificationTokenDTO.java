package com.sunrise.core.creation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.sunrise.orchestrator.type.TokenType;

@lombok.Getter
public class CreateVerificationTokenDTO {
    private final long id;
    private final long userId;
    private final String token;
    private final TokenType tokenType;
    private final Instant expiryDate;
    private final Instant createdAt;

    public CreateVerificationTokenDTO(long id, long userId, String token, TokenType tokenType, Instant createdAt, int expireInHours) {
        this.id = id;
        this.token = token;
        this.userId = userId;
        this.expiryDate = createdAt.plus(expireInHours, ChronoUnit.HOURS);
        this.createdAt = createdAt;
        this.tokenType = tokenType;
    }
}
