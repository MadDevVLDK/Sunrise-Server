package com.sunrise.orchestrator.result;

import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Getter
@lombok.AllArgsConstructor
public class ChatMetaDTO {
    private long id;
    @JsonProperty("isPinned")
    private boolean isPinned;
    private Long lastMsgId;
    private int unreadCount;
    private long seq;
}