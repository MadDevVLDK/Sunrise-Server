package com.sunrise.entity;

import com.sunrise.core.dataservice.dbresult.*;
import com.sunrise.core.dataservice.type.*;
import com.sunrise.entity.cache.*;
import com.sunrise.entity.creation.*;
import com.sunrise.entity.db.*;
import com.sunrise.entity.dto.*;

import java.util.*;

public class EntityMapper {


    // ========== USER ==========

    public static CacheUserProfile toUserProfileCache(CreateUserDTO user) {
        if (user == null) return null;

        return new CacheUserProfile(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getProfileUpdatedAt(),
            user.getCreatedAt(),
            user.getDeletedAt(),
            user.isDeleted(),
            null, null, null, null
        );
    }
    public static CacheUserProfile toUserProfileCache(UserProfileResult user) {
        if (user == null) return null;

        return new CacheUserProfile(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getCreatedAt(),
            user.getCreatedAt(),
            user.getCreatedAt(),
            user.getIsDeleted(),

            user.getAvatarId(),
            user.getAvatarHash(),
            user.getAvatarPrHash(),
            user.getAvatarCreatedAt()
        );
    }

    public static CacheUserSecurity toUserSecurityCache(CreateUserDTO user) {
        if (user == null) return null;

        return new CacheUserSecurity(
            user.getId(),
            user.getEmail(),
            user.getHashPassword(),
            user.getJwtVersion(),
            user.isEnabled(),
            user.getDeletedAt(),
            user.isDeleted()
        );
    }
    public static CacheUserSecurity toUserSecurityCache(UserSecurityResult user) {
        if (user == null) return null;

        return new CacheUserSecurity(
            user.getId(),
            user.getEmail(),
            user.getHashPassword(),
            user.getJwtVersion(),
            user.getIsEnabled(),
            user.getDeletedAt(),
            user.getIsDeleted()
        );
    }

    public static User toUserEntity(CreateUserDTO user) {
        if (user == null) return null;

        return new User(
            user.getId(),
            null,
            null,
            user.getUsername(),
            user.getName(),
            user.getEmail(),
            user.getHashPassword(),
            user.getLastLogin(),
            user.getCreatedAt(),
            user.getCreatedAt(),
            user.getCreatedAt(),
            user.getJwtVersion(),
            user.isEnabled(),
            user.getDeletedAt(),
            user.isDeleted()
        );
    }

    public static UserSecurityDTO toUserSecurityDTO(UserSecurityResult user) {
        if (user == null) return null;

        return new UserSecurityDTO(
            user.getId(),
            user.getEmail(),
            user.getHashPassword(),
            user.getJwtVersion(),
            user.getIsEnabled(),
            user.getDeletedAt(),
            user.getIsDeleted()
        );
    }
    public static UserSecurityDTO toUserSecurityDTO(CacheUserSecurity user) {
        if (user == null) return null;

        return new UserSecurityDTO(
            user.getId(),
            user.getEmail(),
            user.getHashPassword(),
            user.getJwtVersion(),
            user.isEnabled(),
            user.getDeletedAt(),
            user.isDeleted()
        );
    }

    public static UserProfileLightDTO toUserProfileDTO(CacheUserProfile user) {
        if (user == null) return null;

        return new UserProfileLightDTO(
            user.getId(),
            toDtoUserAvatar(user),
            user.getUsername(),
            user.getName(),
            user.getProfileUpdatedAt(),
            user.getCreatedAt()
        );
    }
    public static UserProfileLightDTO toUserProfileDTO(UserProfileResult user) {
        if (user == null) return null;

        return new UserProfileLightDTO(
            user.getId(),
            toDtoUserAvatar(user),
            user.getUsername(),
            user.getName(),
            user.getProfileUpdatedAt(),
            user.getCreatedAt()
        );
    }

    public static Map<Long, UserProfileLightDTO> toDtoUserProfiles(Collection<UserProfileResult> users, Map<Long, UserProfileLightDTO> resultMap) {
        if (users == null) return resultMap;

        for (UserProfileResult user : users) {
            resultMap.put(user.getId(), toUserProfileDTO(user));
        }
        return resultMap;
    }


    // ========== USER AVATAR ==========

    public static UserAvatarDTO toDtoUserAvatar(UserProfileResult user) {
        if (user == null || user.getAvatarId() == null) return null;

        return new UserAvatarDTO(
            user.getAvatarId(),
            user.getAvatarHash(),
            user.getAvatarPrHash(),
            true,
            user.getAvatarCreatedAt()
        );
    }
    public static UserAvatarDTO toDtoUserAvatar(CacheUserProfile user) {
        if (user == null || user.getAvatarId() == null) return null;

        return new UserAvatarDTO(
            user.getAvatarId(),
            user.getAvatarHash(),
            user.getAvatarPreviewHash(),
            true,
            user.getAvatarCreatedAt()
        );
    }


    // ========== CHAT ==========

    public static CacheChat toChatCache(CreateGroupChatDTO chat) {
        if (chat == null) return null;

        return new CacheChat(
            chat.getId(),
            chat.getName(),
            chat.getDescription(),
            chat.getChatType(),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getUpdatedAt(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted(),
            null, null, null, null
        );
    }
    public static CacheChat toChatCache(CreatePersonalChatDTO chat) {
        if (chat == null) return null;

        return new CacheChat(
            chat.getId(),
            chat.getName(),
            chat.getDescription(),
            chat.getChatType(),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getUpdatedAt(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted(),
            null, null, null, null
        );
    }
    public static CacheChat toChatCache(ChatProfileResult chat) {
        if (chat == null) return null;

        return new CacheChat(
            chat.getId(),
            chat.getName(),
            chat.getDescription(),
            ChatType.valueOf(chat.getChatType()),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getUpdatedAt(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.getIsDeleted(),

            chat.getAvatarId(),
            chat.getAvatarHash(),
            chat.getAvatarPrHash(),
            chat.getAvatarCreatedAt()
        );
    }
    public static CacheChat toChatCache(UserChatResult chat) {
        if (chat == null) return null;

        return new CacheChat(
                chat.getId(),
                chat.getName(),
                chat.getDescription(),
                ChatType.valueOf(chat.getChatType()),
                chat.getOpponentId(),
                chat.getMembersCount(),
                chat.getUpdatedAt(),
                chat.getCreatedAt(),
                chat.getCreatedBy(),
                chat.getDeletedAt(),
                chat.getIsDeleted(),

                chat.getAvatarId(),
                chat.getAvatarHash(),
                chat.getAvatarPrHash(),
                chat.getAvatarCreatedAt()
        );
    }
    public static List<CacheChat> toChatsCache(Collection<UserChatResult> items) {
        if (items == null) return Collections.emptyList();

        List<CacheChat> cached = new ArrayList<>(items.size());
        for (UserChatResult item : items) {
            cached.add(EntityMapper.toChatCache(item));
        }
        return cached;
    }

    public static Chat toChatEntity(CreateGroupChatDTO chat) {
        if (chat == null) return null;

        return new Chat(
            chat.getId(),
            chat.getAvatarFileId(),
            chat.getAvatarPreviewFileId(),
            chat.getName(),
            chat.getDescription(),
            chat.getChatType(),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getUpdatedAt(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }
    public static Chat toChatEntity(CreatePersonalChatDTO chat) {
        if (chat == null) return null;

        return new Chat(
            chat.getId(),
            chat.getAvatarFileId(),
            chat.getAvatarPreviewFileId(),
            chat.getName(),
            chat.getDescription(),
            chat.getChatType(),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getUpdatedAt(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }

    public static ChatSecurityDTO toChatSecurityDTO(ChatProfileResult chat) {
        if (chat == null) return null;

        return new ChatSecurityDTO(
            chat.getId(),
            ChatType.valueOf(chat.getChatType()),
            chat.getMembersCount(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.getIsDeleted()
        );
    }
    public static ChatSecurityDTO toChatSecurityDTO(CacheChat chat) {
        if (chat == null) return null;

        return new ChatSecurityDTO(
            chat.getId(),
            chat.getChatType(),
            chat.getMembersCount(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }

    public static ChatProfileDTO toChatProfileDTO(UserChatResult chat) {
        if (chat == null) return null;

        return new ChatProfileDTO(
            chat.getId(),
            toChatAvatarDTO(chat),
            chat.getName(),
            chat.getDescription(),
            ChatType.valueOf(chat.getChatType()),
            chat.getOpponentId(),
            chat.getMembersCount(),
            toUserMessageDTO(chat),
            chat.getUnreadCount(),
            chat.getUpdatedAt(),
            chat.getCreatedAt(),
            chat.getCreatedBy()
        );
    }
    public static Map<Long, ChatProfileDTO> toChatProfileDTOs(Collection<UserChatResult> chats) {
        if (chats == null) return null;
        Map<Long, ChatProfileDTO> resultMap = new HashMap<>(chats.size());

        for (UserChatResult chat : chats) {
            resultMap.put(chat.getId(), toChatProfileDTO(chat));
        }
        return resultMap;
    }


    // ========== CHAT AVATAR ==========

    public static ChatAvatarDTO toChatAvatarDTO(UserChatResult chat) {
        if (chat == null || chat.getAvatarId() == null) return null;

        return new ChatAvatarDTO(
            chat.getAvatarId(),
            chat.getAvatarHash(),
            chat.getAvatarPrHash(),
            true,
            chat.getAvatarCreatedAt()
        );
    }


    // ========== CHAT MEMBER ==========

    public static CacheChatMember toChatMemberCache(CreateChatMemberDTO member) {
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
    public static CacheChatMember toChatMemberCache(ChatMember member) {
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
    public static List<CacheChatMember> toChatMemberCaches(Collection<CreateChatMemberDTO> items) {
        if (items == null) return Collections.emptyList();

        List<CacheChatMember> cached = new ArrayList<>();
        for (CreateChatMemberDTO item : items) {
            cached.add(EntityMapper.toChatMemberCache(item));
        }
        return cached;
    }

    public static ChatMember toChatMemberEntity(CreateChatMemberDTO member) {
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

    public static ChatMemberFullDTO toChatMemberFullDTO(ChatMember member) {
        if (member == null) return null;

        return new ChatMemberFullDTO(
            member.getChatId(),
            member.getUserId(),
            member.getTag(),
            member.getSettingsUpdatedAt(),
            member.getUpdatedAt(),
            member.getJoinedAt(),
            member.isPinned(),
            member.isAdmin()
        );
    }
    public static ChatMemberFullDTO toChatMemberFullDTO(CacheChatMember member) {
        if (member == null) return null;

        return new ChatMemberFullDTO(
            member.getChatId(),
            member.getUserId(),
            member.getTag(),
            member.getSettingsUpdatedAt(),
            member.getUpdatedAt(),
            member.getJoinedAt(),
            member.isPinned(),
            member.isAdmin()
        );
    }
    public static ChatMemberProfileDTO toChatMemberProfileDTO(CacheChatMember member) {
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
    public static ChatMemberProfileFullDTO toChatMemberProfileFullDTO(UserProfileLightDTO user, ChatMemberProfileDTO member) {
        if (user == null || member == null) return null;

        return new ChatMemberProfileFullDTO(
            user,
            member
        );
    }


    // ========== MESSAGE ==========

    public static CacheMessageSecurity toMessageSecurityCache(CreateMessageDTO message) {
        if (message == null) return null;

        return new CacheMessageSecurity(
            message.getId(),
            message.getChatId(),
            message.getSenderId(),
            message.getMessageType(),
            message.getSentAt(),
            message.getDeletedAt(),
            message.isDeleted()
        );
    }
    public static CacheMessageSecurity toMessageSecurityCache(UserMessageResult message) {
        if (message == null) return null;

        return new CacheMessageSecurity(
            message.getId(),
            message.getChatId(),
            message.getSenderId(),
            MessageType.valueOf(message.getMessageType()),
            message.getSentAt(),
            message.getDeletedAt(),
            message.getIsDeleted()
        );
    }
    public static CacheMessageSecurity toMessageSecurityCache(Message message) {
        if (message == null) return null;

        return new CacheMessageSecurity(
            message.getId(),
            message.getChatId(),
            message.getSenderId(),
            message.getMessageType(),
            message.getSentAt(),
            message.getDeletedAt(),
            message.isDeleted()
        );
    }

    public static Message toMessageEntity(CreateMessageDTO message) {
        if (message == null) return null;

        return new Message(
            message.getId(),
            message.getChatId(),
            message.getSenderId(),
            message.getMessageType(),
            message.getText(),
            message.getReadCount(),
            message.getSentAt(),
            message.getUpdatedAt(),
            message.getDeletedAt(),
            message.isDeleted()
        );
    }

    public static UserMessageDTO toUserMessageDTO(UserMessageResult message, boolean isCensored) {
        if (message == null) return null;

        return new UserMessageDTO(
            message.getId(),
            message.getChatId(),
            message.getSenderId(),
            MessageType.valueOf(message.getMessageType()),
            message.getProfileUpdatedAt(),
            isCensored ? null : message.getText(),
            message.getReadCount(),
            message.getIsReadByUser() != null && message.getIsReadByUser(),
            message.getSentAt(),
            message.getUpdatedAt(),
            message.getDeletedAt(),
            message.getIsDeleted()
        );
    }
    public static UserMessageDTO toUserMessageDTO(UserChatResult chat) {
        if (chat == null || chat.getMsgId() == null) return null;

        return new UserMessageDTO(
            chat.getMsgId(),
            chat.getMsgChatId(),
            chat.getMsgSenderId(),
            MessageType.valueOf(chat.getMsgMessageType()),
            chat.getMsgProfileUpdatedAt(),
            chat.getMsgText(),
            chat.getMsgReadCount(),
            chat.getMsgIsReadByUser(),
            chat.getMsgSentAt(),
            chat.getMsgUpdatedAt(),
            chat.getMsgDeletedAt(),
            chat.getMsgIsDeleted()
        );
    }


    // ========== MESSAGE READ STATUS ==========

    public static Map<Long, MessageReadStatusDTO> toMessageReadDTOs(Collection<MessageReadStatusResult> items) {
        if (items == null) return null;

        Map<Long, MessageReadStatusDTO> resultMap = new HashMap<>(items.size());
        for (MessageReadStatusResult item : items) {
            resultMap.put(item.getUserId(), new MessageReadStatusDTO(
                item.getUserId(),
                item.getReadAt()
            ));
        }
        return resultMap;
    }


    // ========== VERIFICATION_TOKEN ==========

    public static CacheVerificationToken toVerificationTokenCache(CreateVerificationTokenDTO verificationToken) {
        if (verificationToken == null) return null;

        return new CacheVerificationToken(
            verificationToken.getId(),
            verificationToken.getUserId(),
            verificationToken.getToken(),
            verificationToken.getTokenType(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt()
        );
    }
    public static CacheVerificationToken toVerificationTokenCache(VerificationToken verificationToken) {
        if (verificationToken == null) return null;

        return new CacheVerificationToken(
            verificationToken.getId(),
            verificationToken.getUserId(),
            verificationToken.getToken(),
            verificationToken.getTokenType(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt()
        );
    }

    public static VerificationToken toVerificationTokenEntity(CreateVerificationTokenDTO verificationToken) {
        if (verificationToken == null) return null;

        return new VerificationToken(
            verificationToken.getId(),
            verificationToken.getUserId(),
            verificationToken.getToken(),
            verificationToken.getTokenType(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt()
        );
    }

    public static VerificationTokenDTO toVerificationTokenDTO(VerificationToken verificationToken) {
        if (verificationToken == null) return null;

        return new VerificationTokenDTO(
            verificationToken.getId(),
            verificationToken.getUserId(),
            verificationToken.getToken(),
            verificationToken.getTokenType(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt()
        );
    }
    public static VerificationTokenDTO toVerificationTokenDTO(CacheVerificationToken verificationToken) {
        if (verificationToken == null) return null;

        return new VerificationTokenDTO(
            verificationToken.getId(),
            verificationToken.getUserId(),
            verificationToken.getToken(),
            verificationToken.getTokenType(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt()
        );
    }


    // ========== LOGIN HISTORY ==========

    public static LoginHistory toLoginHistoryEntity(CreateLoginHistoryDTO loginHistory) {
        if (loginHistory == null) return null;

        return new LoginHistory(
            loginHistory.getId(),
            loginHistory.getUserId(),
            loginHistory.getIpAddress(),
            loginHistory.getDeviceInfo(),
            loginHistory.getLoginAt()
        );
    }
}