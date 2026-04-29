package com.sunrise.cache.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

import com.sunrise.orchestrator.type.MessageType;

@Getter
@AllArgsConstructor
public class CacheMessage {
    private long id;
    private long chatId;
    private MessageType messageType;
    private long senderId;
    private String text;
    private long readCount;
    private Instant sentAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private boolean isDeleted;

    private final Instant cachedAt = Instant.now();

    public boolean isActive() {
        return !isDeleted;
    }
}