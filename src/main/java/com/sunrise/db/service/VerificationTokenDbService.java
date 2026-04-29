package com.sunrise.db.service;

import com.sunrise.db.entity.VerificationToken;
import com.sunrise.db.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationTokenDbService {

    private final VerificationTokenRepository tokenRepository;

    @Transactional
    public void save(VerificationToken token) {
        log.debug("[🗄️] 🎫 Saving verification token: userId={}, token={}, type={}", 
            token.getUserId(), token.getToken(), token.getTokenType());
        tokenRepository.save(token);
    }

    @Transactional
    public void deleteByToken(String token) {
        log.debug("[🗄️] 🚫 Deleting verification token: token={}", token);
        tokenRepository.deleteByToken(token);
    }

    @Transactional(readOnly = true)
    public Optional<VerificationToken> getByToken(String token) {
        log.debug("[🗄️] 🔍 Getting verification token: token={}", token);
        return tokenRepository.getByToken(token);
    }

    @Transactional
    public int cleanupExpired() {
        log.debug("[🗄️] 🧹 Cleaning up expired verification tokens");
        int result = tokenRepository.deleteByExpiryDateBefore(Instant.now());
        log.debug("[🗄️] 🧹 Cleaned up {} expired tokens", result);
        return result;
    }
}