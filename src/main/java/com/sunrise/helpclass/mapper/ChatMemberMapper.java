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

    public static CacheChatMember copy(CacheChatMember member){
        if (member == null) return null;

        return new CacheChatMember(
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

    public static CacheChatMember toCache(CreateChatMemberDTO member) {
        if (member == null) return null;

        return new CacheChatMember(
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

    public static CacheChatMember toCache(ChatMember member) {
        if (member == null) return null;

        return new CacheChatMember(
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

    public static List<CacheChatMember> toCaches(Collection<CreateChatMemberDTO> items) {
        if (items == null) return Collections.emptyList();

        List<CacheChatMember> cached = new ArrayList<>();
        for (CreateChatMemberDTO item : items) {
            cached.add(toCache(item));
        }
        return cached;
    }

    public static ChatMember toEntity(CreateChatMemberDTO member) {
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

    public static ChatMemberProfileDTO toProfileDTO(ChatMember member) {
        if (member == null) return null;

        return new ChatMemberProfileDTO(
            member.getChatId(),
            member.getUserId(),
            member.getTag(),
            member.getUpdatedAt(),
            member.getJoinedAt(),
            member.isAdmin()
        );
    }

    public static ChatMemberProfileDTO toProfileDTO(CacheChatMember member) {
        if (member == null) return null;

        return new ChatMemberProfileDTO(
            member.getChatId(),
            member.getUserId(),
            member.getTag(),
            member.getUpdatedAt(),
            member.getJoinedAt(),
            member.isAdmin()
        );
    }

    public static ChatMemberFullDTO toSelfProfileDTO(UserChatResult chat, long userId) {
        if (chat == null) return null;

        return new ChatMemberFullDTO(
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

    public static ChatMemberProfileFullDTO toOpponentProfileFullDTO(UserChatResult userChat) {
        if (userChat == null || userChat.getOpponentId() == null) return null;

        return new ChatMemberProfileFullDTO(
            new UserProfileLightDTO(
                userChat.getOpponentId(),
                userChat.getOpponentUsername(),
                userChat.getOpponentName(),
                userChat.getOpponentProfileUpdatedAt(),
                userChat.getOpponentCreatedAt()
            ),
            new ChatMemberProfileDTO(
                userChat.getId(),
                userChat.getOpponentId(),
                userChat.getOpponentMemberTag(),
                userChat.getOpponentMemberUpdatedAt(),
                userChat.getOpponentMemberJoinedAt(),
                userChat.getOpponentMemberIsAdmin()
            )
        );
    }

    public static ChatMemberProfileFullDTO toProfileFullDTO(UserProfileLightDTO user, ChatMemberProfileDTO member) {
        if (user == null || member == null) return null;

        return new ChatMemberProfileFullDTO(
            user,
            member
        );
    }
}
