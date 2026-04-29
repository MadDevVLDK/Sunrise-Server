package com.sunrise.db.entity;

import jakarta.persistence.*;

import java.time.Instant;


@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@Entity
@Cacheable(false)
@Table(name = "message_read_status")
public class MessageReadStatus {

    @EmbeddedId
    protected MessageReadStatusId id;

    @Column(name = "read_at", nullable = false)
    private Instant readAt = Instant.now();
}
