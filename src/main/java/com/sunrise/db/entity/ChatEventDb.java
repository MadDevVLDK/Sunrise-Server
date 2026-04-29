package com.sunrise.db.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "chat_events")
@IdClass(ChatEventId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatEventDb {

    @Id
    @Column(name = "chat_id")
    private Long chatId;

    @Id
    @Column(name = "seq")
    private Long seq = 0L;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}