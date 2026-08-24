package com.sunrise.core.service;

import com.sunrise.core.creation.CreateDto;
import com.sunrise.helpclass.SnowflakeId;
import com.sunrise.helpclass.exception.MyErrorCode;
import com.sunrise.helpclass.exception.MyException;
import com.sunrise.orchestrator.DataValidator;
import com.sunrise.orchestrator.result.Dto.*;
import com.sunrise.orchestrator.service.ChatOrchestrator;
import com.sunrise.web.payload.ApiRequest.ChatSyncUnit;

import org.springframework.lang.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatOrchestrator chatOrchestrator;

    private final DataValidator validator;

    @Transactional
    public Long createPersonalChat(String tempId, long creatorId, long opponentId) {
        validator.validateActiveUsers(creatorId, opponentId);

        Instant createdAt = Instant.now();
        Optional<ChatSecurity> optActiveChat = chatOrchestrator.getActivePersonalChat(creatorId, opponentId);
        if (optActiveChat.isPresent()) {
            log.info("[🔧] ✅ Get personal chat {} between users {} and {}", optActiveChat.get().id(), creatorId, opponentId);
            return optActiveChat.get().id();
        }

        long chatId = SnowflakeId.next();

        CreateDto.PersonalChat chat = new CreateDto.PersonalChat(
            chatId, opponentId, createdAt, creatorId
        );
        CreateDto.ChatMember creator = new CreateDto.ChatMember(chatId, creatorId, createdAt, false);
        CreateDto.ChatMember opponent = new CreateDto.ChatMember(chatId, opponentId, createdAt, false);

        chatOrchestrator.savePersonalChatAndAddMembers(tempId, chat, creator, opponent);

        log.info("[🔧] ✅ Created personal chat {} between users {} and {}", chatId, creatorId, opponentId);
        return chatId;
    }

    @Transactional
    public Long createGroupChat(String tempId, long creatorId, @NonNull String chatName, @NonNull String chatDescription, @NonNull Set<Long> usersToAddIds) {
        validator.validateCanCreateGroupChat(creatorId, usersToAddIds);

        int membersCount = usersToAddIds.size() + 1;
        long chatId = SnowflakeId.next();
        Instant createdAt = Instant.now();

        CreateDto.GroupChat chat = new CreateDto.GroupChat(chatId, chatName, chatDescription, membersCount, createdAt, creatorId);
        CreateDto.ChatMember creator = new CreateDto.ChatMember(chatId, creatorId, createdAt, true);

        List<CreateDto.ChatMember> chatMembers = new ArrayList<>(membersCount);
        for (long userId : usersToAddIds) {
            chatMembers.add(new CreateDto.ChatMember(chatId, userId, createdAt, false));
        }

        chatOrchestrator.saveGroupChatAndAddMembers(tempId, chat, creator, chatMembers);

        log.info("[🔧] ✅ Created group chat {} '{}' with {} members by creator {}", chatId, chatName, usersToAddIds.size(), creatorId);
        return chatId;
    }

    @Transactional
    public void updateChatInfo(long chatId, long userId, String newName, String newDescription) {
        validator.validateCanUpdateChatInfo(chatId, userId);

        Instant updatedAt = Instant.now();
        boolean isUpdated = chatOrchestrator.updateChatProfile(chatId, newName, newDescription, updatedAt);
        if (isUpdated) {
            log.info("[🔧] ✅ Chat info changed for chat {} by user {}", chatId, userId);
        }
    }

    @Transactional
    public void deleteChat(long chatId, long userId) {
        validator.validateCanDeleteChat(chatId, userId);

        Instant deletedAt = Instant.now();
        boolean isUpdated = chatOrchestrator.delete(chatId, deletedAt);
        if (isUpdated) {
            log.info("[🔧] ✅ Admin {} deleted chat {}", userId, chatId);
        }
    }

    @Transactional(readOnly = true)
    public Map<Long, ChatEventSync> syncChats(long userId, List<ChatSyncUnit> cursors) {
        validator.validateActiveUser(userId);
        for (ChatSyncUnit cursor : cursors) {
            validator.validateActiveChatMemberInActiveChat(cursor.chatId(), userId);
        }

        Map<Long, ChatEventSync> result = chatOrchestrator.getSyncChats(cursors, userId);
        log.debug("[🔧] ✅ User {} got sync for {} chats", userId, result.size());
        return result;
    }

    @Transactional(readOnly = true)
    public List<ChatMeta> getUserChatsMeta(long userId) {
        validator.validateActiveUser(userId);

        List<ChatMeta> result = chatOrchestrator.getChatsMeta(userId);
        log.debug("[🔧] ✅ User {} got meta for {} chats", userId, result.size());
        return result;
    }

    @Transactional(readOnly = true)
    public List<ChatProfile> getUserChatsByIds(long userId, Set<Long> chatIds) {
        validator.validateActiveUser(userId);
        if (chatIds == null || chatIds.isEmpty()) {
            return List.of();
        }

        List<ChatProfile> result = chatOrchestrator.getUserChatsByIds(userId, chatIds.toArray(new Long[0]));
        log.debug("[🔧] ✅ User {} got {} chats by ids", userId, result.size());
        return result;
    }

    @Transactional(readOnly = true)
    public ChatProfile getUserChat(long chatId, long userId) {
        validator.validateActiveUser(userId);

        ChatProfile chat = chatOrchestrator.getUserChat(chatId, userId)
            .orElseThrow(() -> new MyException(
                MyErrorCode.CHAT_NOT_FOUND_OR_DELETED,
                "Chat not found or is deleted -> " + chatId
            ));

        log.debug("[🔧] ✅ User {} got chat {}", userId, chatId);
        return chat;
    }

    @Transactional(readOnly = true)
    public ChatStatsResult getChatStats(long chatId, long userId) {
        validator.validateActiveChatMemberInActiveChat(chatId, userId);

        ChatStatsResult result = chatOrchestrator.getChatClearStats(chatId, userId);
        log.debug("[🔧] ✅ User {} viewed stats for chat {}", userId, chatId);
        return result;
    }

    @Transactional(readOnly = true)
    public boolean isActionsEnabledForChat(long chatId, long userId) {
        ChatSecurity chat = validator.validateActiveUserInActiveChatAndGetChat(chatId, userId);
        return chat.chatType().isActionsEnabled(chat.membersCount());
    }
}