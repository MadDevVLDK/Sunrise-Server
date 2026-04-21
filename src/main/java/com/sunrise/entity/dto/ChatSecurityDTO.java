package com.sunrise.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sunrise.core.dataservice.type.ChatType;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class ChatSecurityDTO {
    private long id;
    private ChatType chatType;
    private int membersCount;
    private LocalDateTime createdAt;
    private long createdBy;
    private LocalDateTime deletedAt;
    private boolean isDeleted;

    @JsonIgnore
    public boolean isActionsEnabled() {
        return chatType.isActionsEnabled(membersCount);
    }
}