package com.sunrise.entity.db;

import com.sunrise.core.dataservice.type.MessageType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

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
    private long readCount;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    public boolean isActive(){
        return !isDeleted;
    }
}
