package com.sunrise.orchestrator.result;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Getter
@lombok.AllArgsConstructor
public class ChatMemberFullDTO {
    private long chatId;
    private long userId;
    private String tag;
    private Instant settingsUpdatedAt;
    private Instant updatedAt;
    private Instant joinedAt;
    @JsonProperty("isPinned")
    private boolean isPinned;
    @JsonProperty("isAdmin")
    private boolean isAdmin;
}
