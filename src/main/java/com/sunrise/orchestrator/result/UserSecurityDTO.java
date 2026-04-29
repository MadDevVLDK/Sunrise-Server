package com.sunrise.orchestrator.result;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Getter
@lombok.AllArgsConstructor
public class UserSecurityDTO {
    private long id;
    private String email;
    private String hashPassword;
    private int jwtVersion;
    @JsonProperty("isEnabled")
    private boolean isEnabled;
    private Instant deletedAt;
    @JsonProperty("isDeleted")
    private boolean isDeleted;
}