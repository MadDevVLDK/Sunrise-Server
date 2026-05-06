package com.sunrise.cache.entity;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import com.sunrise.orchestrator.type.ChatType;
import com.sunrise.orchestrator.type.MessageType;
import com.sunrise.orchestrator.type.TokenType;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class Cache {

    public record UserSecurity(
        long id,
        String email,
        String hashPassword,
        int jwtVersion,
        boolean isEnabled,
        Instant deletedAt,
        boolean isDeleted
    ) { }


    public record UserProfile(
        long id,
        String username,
        String name,
        Instant profileUpdatedAt,
        Instant createdAt,
        Instant deletedAt,
        boolean isDeleted
    ) { }

    @Getter @AllArgsConstructor
    public static class Chat {
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

        public void increaseMembersCount(int numToAdd) { membersCount += numToAdd; }
        public void decreaseMembersCount(int numToSubtract) { membersCount -= numToSubtract; }
        public boolean isActive() { return !isDeleted; }
        public boolean isPersonal() { return chatType.isPersonal(); }
        public boolean isNotPersonal() { return chatType.isNotPersonal(); }
    }

    public record ChatMember(
        long chatId,
        long userId,
        String tag,
        Instant settingsUpdatedAt,
        Instant updatedAt,
        Instant joinedAt,
        boolean isPinned,
        boolean isAdmin,
        Instant deletedAt,
        boolean isDeleted) {

        public boolean isActive() {
            return !isDeleted;
        }
    }

    public record Message( 
        long id,
        long chatId,
        MessageType messageType,
        long senderId,
        String text,
        AtomicInteger readCount,
        Instant sentAt,
        Instant updatedAt,
        Instant deletedAt,
        boolean isDeleted) { 
        public boolean isActive() {
            return !isDeleted;
        }
    }

    public record VerificationToken(
        long id,
        long userId,
        String token,
        TokenType tokenType,
        Instant expiryDate,
        Instant createdAt
    ) { }
}