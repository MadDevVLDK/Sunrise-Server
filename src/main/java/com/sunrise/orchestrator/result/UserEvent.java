package com.sunrise.orchestrator.result;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class UserEvent {
    public interface IUserEvent {
        @JsonIgnore Instant getCreatedAtDb();
    }

    public record ChatCreatedWithMembers(
        long chatId,
        String name,
        String description,
        String chatType,
        Long opponentId,
        int membersCount,
        long createdBy,
        List<MemberInfo> members,
        Instant createdAt
    ) implements IUserEvent {
        @Override public Instant getCreatedAtDb() { return createdAt; }
        public record MemberInfo(long userId, boolean isAdmin, Instant joinedAt) {}
    }

    public record ChatMemberSettingsUpdated(
        long chatId,
        long userId, 
        boolean isPinned, 
        Instant updatedAt
    ) implements IUserEvent {
        @Override public Instant getCreatedAtDb() { return updatedAt; }
    }
}
