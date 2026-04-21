package com.sunrise.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sunrise.core.dataservice.type.TokenType;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.AllArgsConstructor
public class VerificationTokenDTO {
    private long id;
    private long userId;
    private String token;
    private TokenType tokenType;
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;

    @JsonIgnore
    public boolean isExpired() {
        return expiryDate.isBefore(LocalDateTime.now());
    }
}
