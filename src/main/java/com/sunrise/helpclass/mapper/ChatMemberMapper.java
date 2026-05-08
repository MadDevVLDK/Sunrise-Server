package com.sunrise.helpclass.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sunrise.cache.entity.*;
import com.sunrise.core.creation.*;
import com.sunrise.db.entity.*;
import com.sunrise.db.result.*;
import com.sunrise.orchestrator.result.*;

import io.jsonwebtoken.lang.Collections;

public class ChatMemberMapper {
    

    // ========== CHAT MEMBER ==========

    public static Cache.ChatMember copy(Cache.ChatMember member){
        if (member == null) return null;

        return new Cache.ChatMember(
            member.chatId(),
            member.userId(),
            member.tag(),
            member.settingsUpdatedAt(),
            member.updatedAt(),
            member.joinedAt(),
            member.isPinned(),
            member.isAdmin(),
            member.deletedAt(),
            member.isDeleted()
        );
    }

    public static Cache.ChatMember toCache(CreateDto.ChatMember member) {
        if (member == null) return null;

        return new Cache.ChatMember(
            member.getChatId(),
            member.getUserId(),
            member.getTag(),
            member.getSettingsUpdatedAt(),
            member.getUpdatedAt(),
            member.getJoinedAt(),
            member.isPinned(),
            member.isAdmin(),
            member.getDeletedAt(),
            member.isDeleted()
        );
    }

    public static Cache.ChatMember toCache(ChatMember member) {
        if (member == null) return null;

        return new Cache.ChatMember(
            member.getChatId(),
            member.getUserId(),
            member.getTag(),
            member.getSettingsUpdatedAt(),
            member.getUpdatedAt(),
            member.getJoinedAt(),
            member.isPinned(),
            member.isAdmin(),
            member.getDeletedAt(),
            member.isDeleted()
        );
    }

    public static List<Cache.ChatMember> toCaches(Collection<CreateDto.ChatMember> items) {
        if (items == null) return Collections.emptyList();

        List<Cache.ChatMember> cached = new ArrayList<>();
        for (var item : items) {
            cached.add(toCache(item));
        }
        return cached;
    }

    public static ChatMember toEntity(CreateDto.ChatMember member) {
        if (member == null) return null;

        return new ChatMember(
            new ChatMemberId(member.getChatId(), member.getUserId()),
            member.getTag(),
            member.getSettingsUpdatedAt(),
            member.getUpdatedAt(),
            member.getJoinedAt(),
            member.isPinned(),
            member.isAdmin(),
            member.getDeletedAt(),
            member.isDeleted()
        );
    }

    public static Dto.ChatMemberProfile toProfileDTO(ChatMember member) {
        if (member == null) return null;

        return new Dto.ChatMemberProfile(
            member.getChatId(),
            member.getUserId(),
            member.getTag(),
            member.getUpdatedAt(),
            member.getJoinedAt(),
            member.isAdmin()
        );
    }

    public static Dto.ChatMemberProfile toProfileDTO(Cache.ChatMember member) {
        if (member == null) return null;

        return new Dto.ChatMemberProfile(
            member.chatId(),
            member.userId(),
            member.tag(),
            member.updatedAt(),
            member.joinedAt(),
            member.isAdmin()
        );
    }

    public static Dto.ChatMemberFull toSelfProfileDTO(UserChatResult chat, long userId) {
        if (chat == null) return null;

        return new Dto.ChatMemberFull(
            chat.getId(),
            userId,
            chat.getSelfMemberTag(),
            chat.getSelfMemberSettingsUpdatedAt(),
            chat.getSelfMemberUpdatedAt(),
            chat.getSelfMemberJoinedAt(),
            chat.getSelfMemberIsPinned(),
            chat.getSelfMemberIsAdmin()
        );
    }

    public static Dto.ChatMemberProfileFull toOpponentProfileFullDTO(UserChatResult userChat) {
        if (userChat == null || userChat.getOpponentId() == null) return null;

        return new Dto.ChatMemberProfileFull(
            new Dto.UserProfileLight(
                userChat.getOpponentId(),
                userChat.getOpponentUsername(),
                userChat.getOpponentName(),
                userChat.getOpponentProfileUpdatedAt(),
                userChat.getOpponentCreatedAt()
            ),
            new Dto.ChatMemberProfile(
                userChat.getId(),
                userChat.getOpponentId(),
                userChat.getOpponentMemberTag(),
                userChat.getOpponentMemberUpdatedAt(),
                userChat.getOpponentMemberJoinedAt(),
                userChat.getOpponentMemberIsAdmin()
            )
        );
    }

    public static Dto.ChatMemberProfileFull toProfileFullDTO(Dto.UserProfileLight user, Dto.ChatMemberProfile member) {
        if (user == null || member == null) return null;

        return new Dto.ChatMemberProfileFull(
            user,
            member
        );
    }
}
