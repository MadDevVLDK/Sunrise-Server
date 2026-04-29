package com.sunrise.cache.entity;

import java.time.Instant;

@lombok.Getter
@lombok.AllArgsConstructor
public class CacheChatMember {
    private long chatId;
    private long userId;
    private String tag;
    private Instant settingsUpdatedAt;
    private Instant updatedAt;
    private Instant joinedAt;
    private boolean isPinned;
    private boolean isAdmin;
    private Instant deletedAt;
    private boolean isDeleted;

    public boolean isActive() {
        return !isDeleted;
    }
}
