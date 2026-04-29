package com.sunrise.cache.entity;

import java.time.Instant;

import com.sunrise.orchestrator.type.ChatType;

@lombok.Getter
@lombok.AllArgsConstructor
public class CacheChat {
    private long id;
    private String name;
    private String description;
    private ChatType chatType;
    private Long opponentId;
    private int membersCount;
    private Instant updatedAt;
    private Instant createdAt;
    private long createdBy;
    private Instant deletedAt;
    private boolean isDeleted;

    private final Instant cachedAt = Instant.now();

    public void increaseMembersCount(int numToAdd){
        membersCount += numToAdd;
    }
    public void decreaseMembersCount(int numToSubtract){
        membersCount -= numToSubtract;
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
}