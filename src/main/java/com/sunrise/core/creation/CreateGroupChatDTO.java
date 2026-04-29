package com.sunrise.core.creation;

import java.time.Instant;

import com.sunrise.orchestrator.type.ChatType;

@lombok.Getter
public class CreateGroupChatDTO {
    private final long id;
    private final Long avatarFileId = null;
    private final Long avatarPreviewFileId = null;
    private final String name;
    private final String description;
    private final ChatType chatType = ChatType.GROUP;
    private final Long opponentId = null;
    private final int membersCount;
    private final Instant updatedAt;
    private final Instant createdAt;
    private final long createdBy;
    private final Instant deletedAt = null;
    private final boolean isDeleted = false;

    public CreateGroupChatDTO(long id, String name, String description, int membersCount, Instant createdAt, long createdBy) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.membersCount = membersCount;
        this.updatedAt = createdAt;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }
}
