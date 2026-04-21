package com.sunrise.entity.creation;

import com.sunrise.core.dataservice.type.ChatType;

import java.time.LocalDateTime;

@lombok.Getter
public class CreatePersonalChatDTO {
    private final long id;
    private final Long avatarFileId = null;
    private final Long avatarPreviewFileId = null;
    private final String name = null;
    private final String description = null;
    private final ChatType chatType = ChatType.PERSONAL;
    private final long opponentId;
    private final int membersCount = 2;
    private final LocalDateTime updatedAt;
    private final LocalDateTime createdAt;
    private final long createdBy;
    private final LocalDateTime deletedAt = null;
    private final boolean isDeleted = false;

    public CreatePersonalChatDTO(long id, Long opponentId, LocalDateTime createdAt, long createdBy) {
        this.id = id;
        this.opponentId = opponentId;
        this.updatedAt = createdAt;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }
}
