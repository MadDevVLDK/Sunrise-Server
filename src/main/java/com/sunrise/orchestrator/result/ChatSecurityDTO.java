package com.sunrise.orchestrator.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sunrise.orchestrator.type.ChatType;

import java.time.Instant;

@lombok.Getter
@lombok.AllArgsConstructor
public class ChatSecurityDTO {
    private long id;
    private ChatType chatType;
    private int membersCount;
    private Instant createdAt;
    private long createdBy;
    private Instant deletedAt;
    @JsonProperty("isDeleted")
    private boolean isDeleted;
}