package com.sunrise.core.service;

import com.sunrise.core.creation.CreateDto;
import com.sunrise.core.result.*;
import com.sunrise.db.result.ChatStatsResult; // TODO: Колхоз, потом сделаю отдельный класс, щас он просто не нужен
import com.sunrise.notifier.WebSocketNotifier;
import com.sunrise.orchestrator.DataValidator;
import com.sunrise.orchestrator.result.Dto.*;
import com.sunrise.orchestrator.service.ChatOrchestrator;
import com.sunrise.helpclass.SimpleSnowflakeId;
import com.sunrise.helpclass.ValidationException;

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

    private final WebSocketNotifier wsNotify;


    @Transactional
    public ResultOneArg<Long> createPersonalChat(long tempId, long creatorId, long opponentId) {
        try {
            validator.validateActiveUsers(creatorId, opponentId);

            Instant createdAt = Instant.now();
            Optional<ChatSecurity> optChat = chatOrchestrator.getPersonalChat(creatorId, opponentId);
            if (optChat.isPresent()){
                ChatSecurity chat = optChat.get();
                if (chat.isDeleted()) {
                    boolean restored = chatOrchestrator.restore(chat.id(), createdAt);
                    if (restored) {
                        log.info("[🔧] ✅ Restored personal chat {} between users {} and {}", chat.id(), creatorId, opponentId);
                    }
                }
                return ResultOneArg.success(chat.id());
            }

            long chatId = SimpleSnowflakeId.nextId();

            CreateDto.PersonalChat chat = new CreateDto.PersonalChat(
                chatId, opponentId, createdAt, creatorId
            );

            CreateDto.ChatMember creator = new CreateDto.ChatMember(
                chatId, creatorId, createdAt, false
            );
            CreateDto.ChatMember opponent = new CreateDto.ChatMember(
                chatId, opponentId, createdAt, false
            );

            chatOrchestrator.savePersonalChatAndAddMembers(chat, creator, opponent);

            // уведомить надо
            wsNotify.notifyPersonalChatNew(tempId, chat, Set.of(creatorId, opponentId));

            log.info("[🔧] ✅ Created personal chat {} between users {} and {}", chatId, creatorId, opponentId);
            return ResultOneArg.success(chatId);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to create personal chat: {}", e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error creating personal chat: {}", e.getMessage());
            return ResultOneArg.error("CreatePersonalChat failed due to server error");
        }
    }

    @Transactional
    public ResultOneArg<Long> createGroupChat(long tempId, long creatorId, @NonNull String chatName, @NonNull String chatDescription, @NonNull Set<Long> usersToAddIds) {
        try {
            validator.validateCanCreateGroupChat(creatorId, usersToAddIds);

            int membersCount = usersToAddIds.size() + 1;
            long chatId = SimpleSnowflakeId.nextId();
            Instant createdAt = Instant.now();

            CreateDto.GroupChat chat = new CreateDto.GroupChat(chatId, chatName, chatDescription, membersCount, createdAt, creatorId);

            CreateDto.ChatMember creator = new CreateDto.ChatMember(chatId, creatorId, createdAt, true);  // creator с правами админа

            List<CreateDto.ChatMember> chatMembers = new ArrayList<>(membersCount);
            for (long userId : usersToAddIds){
                chatMembers.add(new CreateDto.ChatMember(chatId, userId, createdAt, false)); // остальные без прав
            }

            chatOrchestrator.saveGroupChatAndAddMembers(chat, creator, chatMembers);

            // уведомить надо
            var usersToNotify = new HashSet<>(usersToAddIds);
            usersToNotify.add(creatorId);
            wsNotify.notifyGroupChatNew(tempId, chat, usersToNotify);

            log.info("[🔧] ✅ Created group chat {} '{}' with {} members by creator {}", chatId, chatName, usersToAddIds.size(), creatorId);
            return ResultOneArg.success(chatId);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to create group chat: {}", e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error creating group chat: {}", e.getMessage());
            return ResultOneArg.error("CreateGroupChat failed due to server error");
        }
    }

    @Transactional
    public ResultNoArgs updateChatInfo(long chatId, long userId, String newName, String newDescription) {
        try {
            validator.validateCanUpdateChatInfo(chatId, userId);

            Instant updatedAt = Instant.now();
            boolean changed = chatOrchestrator.updateChatProfile(chatId, newName, newDescription, updatedAt);
            if (changed) {
                // уведомить всех надо об этом
                wsNotify.notifyChatInfoUpdated(chatId, newName, newDescription, updatedAt);
                log.info("[🔧] ✅ Chat info changed for chat {} by user {}", chatId, userId);
            }
            return ResultNoArgs.success();
        } catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to change chat info {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        } catch (Exception e) {
            log.error("[🔧] ⚠️ Error changing chat info {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error("ChangeChatInfo failed due to server error");
        }
    }

    @Transactional
    public ResultNoArgs deleteChat(long chatId, long userId) {
        try {
            validator.validateCanDeleteChat(chatId, userId);

            Instant deletedAt = Instant.now();
            boolean deleted = chatOrchestrator.delete(chatId, deletedAt);
            if (deleted) {
                // уведомить всех надо об этом
                wsNotify.notifyChatDeleted(chatId, deletedAt);
                log.info("[🔧] ✅ Admin {} deleted chat {}", userId, chatId);
            }
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to delete chat {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error deleting chat {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error("DeleteChat failed due to server error");
        }
    }

    public ResultOneArg<Map<Long, GlobalChatSync>> syncChats(long userId, Map<Long, Long> chatSeqIds) {
        try {
            validator.validateActiveUser(userId);
            for (long chatId : chatSeqIds.keySet()) {
                validator.validateActiveChatMemberInActiveChat(chatId, userId);
            }

            Map<Long, GlobalChatSync> result = chatOrchestrator.getSyncChats(chatSeqIds);

            log.debug("[🔧] ✅ User {} got sync for {} chats", userId, result.size());
            return ResultOneArg.success(result);
        } catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get sync chats for user {}: {}", userId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        } catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting sync chats for user {}: {}", userId, e.getMessage());
            return ResultOneArg.error("syncChats failed due to server error");
        }
    }

    public ResultOneArg<List<ChatMeta>> getUserChatsMeta(long userId) {
        try {
            validator.validateActiveUser(userId);

            List<ChatMeta> result = chatOrchestrator.getChatsMeta(userId);

            log.debug("[🔧] ✅ User {} got meta for {} chats", userId, result.size());
            return ResultOneArg.success(result);
        } catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get chat meta for user {}: {}", userId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        } catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting chat meta for user {}: {}", userId, e.getMessage());
            return ResultOneArg.error("getChatsMeta failed due to server error");
        }
    }

    public ResultOneArg<List<ChatProfile>> getUserChatsByIds(long userId, Set<Long> chatIds) {
        try {
            validator.validateActiveUser(userId);
            if (chatIds == null || chatIds.isEmpty()) {
                return ResultOneArg.success(List.of());
            }

            List<ChatProfile> result = 
                    chatOrchestrator.getUserChatsByIds(userId, chatIds.toArray(new Long[0]));

            log.debug("[🔧] ✅ User {} got {} chats by ids", userId, result.size());
            return ResultOneArg.success(result);
        } catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get chats by ids for user {}: {}", userId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        } catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting chats by ids for user {}: {}", userId, e.getMessage());
            return ResultOneArg.error("getChatsByIds failed due to server error");
        }
    }

    public ResultOneArg<ChatProfile> getUserChat(long chatId, long userId) {
        try {
            validator.validateActiveUser(userId);

            ChatProfile chat = chatOrchestrator.getUserChat(chatId, userId)
                    .orElseThrow(() -> new ValidationException("Chat is deleted or not found"));

            log.debug("[🔧] ✅ User {} got chat {}", userId, chatId);
            return ResultOneArg.success(chat);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get user {} chat {}: {}", userId, chatId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting user {} chat {}: {}", userId, chatId, e.getMessage());
            return ResultOneArg.error("getUserChat failed due to server error");
        }
    }
    
    public ResultOneArg<ChatStatsResult> getChatStats(long chatId, long userId) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            ChatStatsResult result = chatOrchestrator.getChatClearStats(chatId, userId);

            log.debug("[🔧] ✅ User {} viewed stats for chat {}", userId, chatId);
            return ResultOneArg.success(result);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get chat {} stats: {}", chatId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting chat {} stats: {}", chatId, e.getMessage());
            return ResultOneArg.error("GetChatStats failed due to server error");
        }
    }
    
    public ResultOneArg<Boolean> isActionsEnabledForChat(long chatId, long userId) {
        try {
            ChatSecurity chat = validator.validateActiveUserInActiveChatAndGetChat(chatId, userId);
            return ResultOneArg.success(chat.chatType().isActionsEnabled(chat.membersCount()));
        } catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get enabled actions for chat {}: {}", chatId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        } catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting enabled actions for chat {}: {}", chatId, e.getMessage());
            return ResultOneArg.error("isActionsEnabledForChat failed due to server error");
        }
    }
}