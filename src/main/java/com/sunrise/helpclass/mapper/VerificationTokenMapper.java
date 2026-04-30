package com.sunrise.helpclass.mapper;

import com.sunrise.cache.entity.*;
import com.sunrise.core.creation.*;
import com.sunrise.db.entity.*;
import com.sunrise.orchestrator.result.*;

public class VerificationTokenMapper {

    
    // ========== VERIFICATION_TOKEN ==========

    public static Cache.VerificationToken copy(Cache.VerificationToken token) {
        if (token == null) return null;

        return new Cache.VerificationToken(
            token.id(),
            token.userId(),
            token.token(),
            token.tokenType(),
            token.expiryDate(),
            token.createdAt()
        );
    }

    public static Cache.VerificationToken toCache(CreateDto.VerificationToken verificationToken) {
        if (verificationToken == null) return null;

        return new Cache.VerificationToken(
            verificationToken.getId(),
            verificationToken.getUserId(),
            verificationToken.getToken(),
            verificationToken.getTokenType(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt()
        );
    }
    public static Cache.VerificationToken toCache(VerificationToken verificationToken) {
        if (verificationToken == null) return null;

        return new Cache.VerificationToken(
            verificationToken.getId(),
            verificationToken.getUserId(),
            verificationToken.getToken(),
            verificationToken.getTokenType(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt()
        );
    }

    public static VerificationToken toEntity(CreateDto.VerificationToken verificationToken) {
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

    public static Dto.VerificationToken toDTO(VerificationToken verificationToken) {
        if (verificationToken == null) return null;

        return new Dto.VerificationToken(
            verificationToken.getId(),
            verificationToken.getUserId(),
            verificationToken.getToken(),
            verificationToken.getTokenType(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt()
        );
    }
    public static Dto.VerificationToken toDTO(Cache.VerificationToken verificationToken) {
        if (verificationToken == null) return null;

        return new Dto.VerificationToken(
            verificationToken.id(),
            verificationToken.userId(),
            verificationToken.token(),
            verificationToken.tokenType(),
            verificationToken.expiryDate(),
            verificationToken.createdAt()
        );
    }
}