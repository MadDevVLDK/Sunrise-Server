package com.sunrise.service.creation;

import com.sunrise.dataservice.type.TokenType;

import java.time.LocalDateTime;

@lombok.Getter
public class CreateVerificationTokenDTO {
    private final long id;
    private final long userId;
    private final String token;
    private final TokenType tokenType;
    private final LocalDateTime expiryDate;
    private final LocalDateTime createdAt;

    public CreateVerificationTokenDTO(long id, long userId, String token, TokenType tokenType, LocalDateTime createdAt, int expireInHours) {
        this.id = id;
        this.token = token;
        this.userId = userId;
        this.expiryDate = createdAt.plusHours(expireInHours);
        this.createdAt = createdAt;
        this.tokenType = tokenType;
    }
}
