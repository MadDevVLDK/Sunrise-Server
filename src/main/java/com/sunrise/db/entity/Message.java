package com.sunrise.db.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;

import java.time.Instant;

import com.sunrise.orchestrator.type.MessageType;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@Entity
@Cacheable(false)
@Table(name = "messages")
public class Message {
    @Id
    private long id;

    @Column(name = "chat_id", nullable = false)
    private long chatId;

    @Column(name = "sender_id")
    private long senderId;

    @Column(name = "message_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    @Column(name = "text")
    private String text;

    @Min(0)
    @Column(name = "read_count", nullable = false)
    private int readCount;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    public boolean isActive(){
        return !isDeleted;
    }
}
