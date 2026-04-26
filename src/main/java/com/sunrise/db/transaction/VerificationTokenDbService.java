package com.sunrise.db.transaction;

import com.sunrise.db.DBService;
import com.sunrise.db.entity.VerificationToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VerificationTokenDbService {

    private final DBService dbService;

    @Transactional
    public void saveVerificationToken(VerificationToken token) {
        dbService.saveVerificationToken(token);
    }

    @Transactional
    public void deleteVerificationToken(String token) {
        dbService.deleteVerificationToken(token);
    }

    @Transactional(readOnly = true)
    public Optional<VerificationToken> getVerificationToken(String token) {
        return dbService.getVerificationToken(token);
    }

    @Transactional
    public int cleanupExpiredVerificationTokens() {
        return dbService.cleanupExpiredVerificationTokens();
    }
}