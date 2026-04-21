package com.sunrise.entity.cache;

import com.sunrise.core.dataservice.type.ChatType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CacheChat {
    private long id;
    private String name;
    private String description;
    private ChatType chatType;
    private Long opponentId;
    private int membersCount;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
    private long createdBy;
    private LocalDateTime deletedAt;
    private boolean isDeleted;

    private Long avatarId;
    private String avatarHash;
    private String avatarPreviewHash;
    private LocalDateTime avatarCreatedAt;

    private final LocalDateTime cachedAt = LocalDateTime.now();

    public void onAddMember(){
        membersCount++;
    }
    public void onAddMembers(int membersToAdd){
        membersCount += membersToAdd;
    }
    public void onDeleteMember(){
        membersCount--;
    }

    public boolean isActive() {
        return !isDeleted;
    }
    public boolean isPersonal(){
        return chatType.isPersonal();
    }
    public boolean isNotPersonal(){
        return chatType.isNotPersonal();
    }

    public static CacheChat copy(CacheChat chat) {
        if (chat == null) return null;

        return new CacheChat(
            chat.getId(),
            chat.getName(),
            chat.getDescription(),
            chat.getChatType(),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getUpdatedAt(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted(),

            chat.getAvatarId(),
            chat.getAvatarHash(),
            chat.getAvatarPreviewHash(),
            chat.getAvatarCreatedAt()
        );
    }
}