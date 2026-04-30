package com.sunrise.cache.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.sunrise.cache.entity.Cache.VerificationToken;
import com.sunrise.helpclass.mapper.VerificationTokenMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationTokenCacheService {

    private final Cache<String, VerificationToken> verificationTokenCache;


    // ========== VERIFICATION TOKEN METHODS ==========

    public void save(VerificationToken cache) {
        verificationTokenCache.put(cache.token(), VerificationTokenMapper.copy(cache));
        log.debug("[⚡] 🎫 Saved verification token for user {} (token={}) || saveVerificationToken", cache.userId(), cache.token());
    }

    public void invalidate(String token) {
        verificationTokenCache.invalidate(token);
        log.debug("[⚡] 🎫🚫 Invalidated verification token {} || deleteVerificationToken", token);
    }

    public Optional<VerificationToken> get(String token) {
        return Optional.ofNullable(VerificationTokenMapper.copy(verificationTokenCache.getIfPresent(token)));
    }
}