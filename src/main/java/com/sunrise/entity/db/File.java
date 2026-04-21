package com.sunrise.entity.db;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class File {

    @Id
    private Long id;

    @Column(name = "hash", length = 64, nullable = false, unique = true)
    private String hash;

    @Column(name = "size", nullable = false)
    private long size;

    @Column(name = "mime_type", nullable = false, length = 127)
    private String mimeType;

    @Column(name = "original_name", nullable = false, columnDefinition = "TEXT")
    private String originalName;

    @Column(name = "storage_path", nullable = false, columnDefinition = "TEXT")
    private String storagePath;

    @Column(name = "ref_count", nullable = false)
    private int refCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}