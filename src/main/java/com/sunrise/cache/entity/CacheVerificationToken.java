package com.sunrise.cache.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

import com.sunrise.orchestrator.type.TokenType;

@Getter
@AllArgsConstructor
public class CacheVerificationToken {
    private long id;
    private long userId;
    private String token;
    private TokenType tokenType;
    private Instant expiryDate;
    private Instant createdAt;

    private final Instant cachedAt = Instant.now();
}
