package com.sunrise.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sunrise.cache.entity.Cache.VerificationToken;
import com.sunrise.cache.service.VerificationTokenCacheService;
import com.sunrise.orchestrator.type.TokenType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationTokenCacheServiceTest {

    private Cache<String, VerificationToken> verificationTokenCache;
    private VerificationTokenCacheService tokenCacheService;

    @BeforeEach
    void setUp() {
        verificationTokenCache = Caffeine.newBuilder().build();
        tokenCacheService = new VerificationTokenCacheService(verificationTokenCache);
    }

    @Test
    void save_shouldStoreToken() {
        VerificationToken token = new VerificationToken(1L, 100L, "abc123", TokenType.REGISTRATION,
                Instant.now().plusSeconds(3600), Instant.now());
        tokenCacheService.save(token);

        assertThat(verificationTokenCache.getIfPresent("abc123")).isNotNull();
    }

    @Test
    void get_whenExists_shouldReturnCopy() {
        VerificationToken original = new VerificationToken(1L, 100L, "xyz", TokenType.PASSWORD_UPDATE,
                Instant.now().plusSeconds(3600), Instant.now());
        verificationTokenCache.put("xyz", original);

        Optional<VerificationToken> result = tokenCacheService.get("xyz");

        assertThat(result).isPresent();
        assertThat(result.get()).isNotSameAs(original);
        assertThat(result.get().token()).isEqualTo("xyz");
    }

    @Test
    void get_whenMissing_shouldReturnEmpty() {
        Optional<VerificationToken> result = tokenCacheService.get("missing");
        assertThat(result).isEmpty();
    }

    @Test
    void invalidate_shouldRemoveToken() {
        verificationTokenCache.put("token123", mockToken());
        tokenCacheService.invalidate("token123");
        assertThat(verificationTokenCache.getIfPresent("token123")).isNull();
    }

    private VerificationToken mockToken() {
        return new VerificationToken(1L, 1L, "t", TokenType.REGISTRATION, Instant.now().plusSeconds(3600), Instant.now());
    }
}