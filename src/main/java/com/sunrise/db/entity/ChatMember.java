package com.sunrise.db.entity;

import jakarta.persistence.*;

import java.time.Instant;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@Entity
@Cacheable(false)
@Table(name = "chat_members")
public class ChatMember {

    @EmbeddedId
    private ChatMemberId id;

    @Column(name = "tag")
    private String tag = null;

    @Column(name = "settings_updated_at", nullable = false)
    private Instant settingsUpdatedAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    @Column(name = "is_pinned", nullable = false)
    private boolean isPinned = false;

    @Column(name = "is_admin", nullable = false)
    private boolean isAdmin = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "is_deleted",nullable = false)
    private boolean isDeleted = false;


    public Long getChatId() {
        return id != null ? id.getChatId() : null;
    }

    public Long getUserId() {
        return id != null ? id.getUserId() : null;
    }
    public boolean isActive() {
        return !isDeleted;
    }
}
