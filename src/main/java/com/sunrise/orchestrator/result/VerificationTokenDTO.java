package com.sunrise.orchestrator.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sunrise.orchestrator.type.TokenType;

import java.time.Instant;

@lombok.Getter
@lombok.AllArgsConstructor
public class VerificationTokenDTO {
    private long id;
    private long userId;
    private String token;
    private TokenType tokenType;
    private Instant expiryDate;
    private Instant createdAt;
}
