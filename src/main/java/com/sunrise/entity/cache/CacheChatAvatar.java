package com.sunrise.entity.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CacheChatAvatar {
    private long id;
    private long chatId;
    private long fileId;
    private Long filePreviewId;
    private boolean isPrimary;
    private String metadata;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private boolean isDeleted;

    private final LocalDateTime cachedAt = LocalDateTime.now();

    public static CacheChatAvatar copy(CacheChatAvatar avatar) {
        if (avatar == null) return null;

        return new CacheChatAvatar(
            avatar.getId(),
            avatar.getChatId(),
            avatar.getFileId(),
            avatar.getFilePreviewId(),
            avatar.isPrimary(),
            avatar.getMetadata(),
            avatar.getUpdatedAt(),
            avatar.getCreatedAt(),
            avatar.getDeletedAt(),
            avatar.isDeleted()
        );
    }
}