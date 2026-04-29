package com.sunrise.helpclass.mapper;

import com.sunrise.cache.entity.*;
import com.sunrise.core.creation.*;
import com.sunrise.db.entity.*;
import com.sunrise.orchestrator.result.*;

public class VerificationTokenMapper {

    
    // ========== VERIFICATION_TOKEN ==========

    public static CacheVerificationToken copy(CacheVerificationToken token) {
        if (token == null) return null;

        return new CacheVerificationToken(
            token.getId(),
            token.getUserId(),
            token.getToken(),
            token.getTokenType(),
            token.getExpiryDate(),
            token.getCreatedAt()
        );
    }

    public static CacheVerificationToken toCache(CreateVerificationTokenDTO verificationToken) {
        if (verificationToken == null) return null;

        return new CacheVerificationToken(
            verificationToken.getId(),
            verificationToken.getUserId(),
            verificationToken.getToken(),
            verificationToken.getTokenType(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt()
        );
    }
    public static CacheVerificationToken toCache(VerificationToken verificationToken) {
        if (verificationToken == null) return null;

        return new CacheVerificationToken(
            verificationToken.getId(),
            verificationToken.getUserId(),
            verificationToken.getToken(),
            verificationToken.getTokenType(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt()
        );
    }

    public static VerificationToken toEntity(CreateVerificationTokenDTO verificationToken) {
        if (verificationToken == null) return null;

        return new VerificationToken(
            verificationToken.getId(),
            verificationToken.getUserId(),
            verificationToken.getToken(),
            verificationToken.getTokenType(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt()
        );
    }

    public static VerificationTokenDTO toDTO(VerificationToken verificationToken) {
        if (verificationToken == null) return null;

        return new VerificationTokenDTO(
            verificationToken.getId(),
            verificationToken.getUserId(),
            verificationToken.getToken(),
            verificationToken.getTokenType(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt()
        );
    }
    public static VerificationTokenDTO toDTO(CacheVerificationToken verificationToken) {
        if (verificationToken == null) return null;

        return new VerificationTokenDTO(
            verificationToken.getId(),
            verificationToken.getUserId(),
            verificationToken.getToken(),
            verificationToken.getTokenType(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt()
        );
    }
}