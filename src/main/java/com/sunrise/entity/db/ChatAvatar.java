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
public class ChatAvatar {

    @Id
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private long chatId;

    @Column(name = "file_id", nullable = false)
    private long fileId;

    @Column(name = "file_preview_id")
    private Long filePreviewId;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;
}