package com.sunrise.orchestrator.result;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Getter
@lombok.AllArgsConstructor
public class ChatMemberProfileDTO {
    private long chatId;
    private long userId;
    private String tag;
    private Instant updatedAt;
    private Instant joinedAt;
    @JsonProperty("isAdmin")
    private boolean isAdmin;
}