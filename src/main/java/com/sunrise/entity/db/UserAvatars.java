package com.sunrise.entity.db;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@Entity
@Cacheable(false)
@Table(name = "chat_avatars")
public class UserAvatars {

    @Id
    private Long id;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(name = "file_id", nullable = false)
    private long fileId;

    @Column(name = "file_preview_id")
    private Long filePreviewId;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;

    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;
}