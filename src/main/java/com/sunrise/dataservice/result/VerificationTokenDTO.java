package com.sunrise.dataservice.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sunrise.dataservice.type.TokenType;

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
