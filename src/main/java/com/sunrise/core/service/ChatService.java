package com.sunrise.core.service;

import com.sunrise.entity.creation.CreateChatMemberDTO;
import com.sunrise.entity.creation.CreateGroupChatDTO;
import com.sunrise.entity.creation.CreatePersonalChatDTO;
import com.sunrise.entity.dto.ChatProfileDTO;
import com.sunrise.entity.pagination.UserChatsPageDTO;
import com.sunrise.entity.dto.ChatSecurityDTO;

import com.sunrise.core.dataservice.DataOrchestrator;
import com.sunrise.core.dataservice.DataValidator;
import com.sunrise.core.dataservice.LockManager;
import com.sunrise.core.dataservice.type.ChatType;
import com.sunrise.core.dataservice.dbresult.ChatStatsResult;
import com.sunrise.core.notifier.WebSocketNotifier;
import com.sunrise.core.service.result.*;

import com.sunrise.helpclass.SimpleSnowflakeId;
import com.sunrise.helpclass.ValidationException;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatService {

    private final DataValidator validator;
    private final DataOrchestrator dataOrchestrator;
    private final LockManager lockManager;
    private final WebSocketNotifier wsNotify;

    public ResultOneArg<Long> createPersonalChat(long tempId, long creatorId, long opponentId) {
        try {
            // на будущий чат
            if (!lockManager.tryLockPersonalChatCreation(creatorId, opponentId)) {
                throw new ValidationException("Try again later");
            }

            validator.validateActiveUsers(creatorId, opponentId);

            LocalDateTime createdAt = LocalDateTime.now();
            Optional<ChatSecurityDTO> optChat = dataOrchestrator.getPersonalChat(creatorId, opponentId);
            if (optChat.isPresent()){
                ChatSecurityDTO chat = optChat.get();
                long chatId = chat.getId();
                if (chat.isDeleted()) {
                    dataOrchestrator.restoreChat(chatId, createdAt);
                    log.info("[🔧] ✅ Restored personal chat {} between users {} and {}", chatId, creatorId, opponentId);
                }
                return ResultOneArg.success(chatId);
            }

            long chatId = SimpleSnowflakeId.nextId();

            CreatePersonalChatDTO chat = new CreatePersonalChatDTO(chatId, opponentId, createdAt, creatorId);

            CreateChatMemberDTO creator = new CreateChatMemberDTO(chatId, creatorId, createdAt, false);
            CreateChatMemberDTO opponent = new CreateChatMemberDTO(chatId, opponentId, createdAt, false);

            dataOrchestrator.savePersonalChatAndAddMembers(chat, creator, opponent);

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
        finally {
            lockManager.unLockPersonalChatCreation(creatorId, opponentId);
        }
    }
    public ResultOneArg<Long> createGroupChat(long tempId, long creatorId, @NotNull String chatName, @NotNull String chatDescription, @NotNull Set<Long> usersToAddIds) {
        try {
            validator.validateCanCreateGroupChat(creatorId, usersToAddIds);

            int membersCount = usersToAddIds.size() + 1;
            long chatId = SimpleSnowflakeId.nextId();
            LocalDateTime createdAt = LocalDateTime.now();

            CreateGroupChatDTO chat = new CreateGroupChatDTO(chatId, chatName, chatDescription, membersCount, createdAt, creatorId);

            CreateChatMemberDTO creator = new CreateChatMemberDTO(chatId, creatorId, createdAt, true);  // creator с правами админа

            List<CreateChatMemberDTO> chatMembers = new ArrayList<>(membersCount);
            for (long userId : usersToAddIds){
                chatMembers.add(new CreateChatMemberDTO(chatId, userId, createdAt, false)); // остальные без прав
            }

            dataOrchestrator.saveGroupChatAndAddMembers(chat, creator, chatMembers);

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

    public ResultNoArgs updateChatInfo(long chatId, long userId, String newName, String newDescription) {
        try {
            validator.validateCanUpdateChatInfo(chatId, userId);

            LocalDateTime updatedAt = LocalDateTime.now();
            dataOrchestrator.updateChatInfo(chatId, newName, newDescription, updatedAt);

            // уведомить надо
            wsNotify.notifyChatInfoUpdated(chatId, newName, newDescription, updatedAt);

            log.info("[🔧] ✅ Chat info changed for chat {} by user {}", chatId, userId);
            return ResultNoArgs.success();
        } catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to change chat info {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        } catch (Exception e) {
            log.error("[🔧] ⚠️ Error changing chat info {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error("ChangeChatInfo failed due to server error");
        }
    }

    public ResultNoArgs deleteChat(long chatId, long userId) {
        try {
            validator.validateCanDeleteChat(chatId, userId);

            LocalDateTime deletedAt = LocalDateTime.now();
            dataOrchestrator.deleteChat(chatId, deletedAt);

            // уведомить надо
            wsNotify.notifyChatDeleted(chatId, deletedAt);

            log.info("[🔧] ✅ Admin {} deleted chat {}", userId, chatId);
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

    public ResultOneArg<List<Long>> getUserChatIds(long userId) {
        try {
            validator.validateActiveUser(userId);

            List<Long> chatIds = dataOrchestrator.getUserChatIds(userId);

            log.debug("[🔧] ✅ User {} got {} chatIds", userId, chatIds.size());
            return ResultOneArg.success(chatIds);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get user {} chatIds: {}", userId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting user {} chatIds: {}", userId, e.getMessage());
            return ResultOneArg.error("getUserChat failed due to server error");
        }
    }
    public ResultOneArg<ChatProfileDTO> getUserChat(long chatId, long userId) {
        try {
            validator.validateActiveUser(userId);

            ChatProfileDTO chat = dataOrchestrator.getUserChat(chatId, userId)
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
    public ResultOneArg<UserChatsPageDTO> getUserChatsPage(long userId, Boolean isPinnedCursor, Long lastMsgIdCursor, Long chatIdCursor, int limit) {
        try {
            validator.validateActiveUser(userId);

            UserChatsPageDTO chats = dataOrchestrator.getUserChatsPage(userId, isPinnedCursor, lastMsgIdCursor, chatIdCursor, limit);

            log.debug("[🔧] ✅ User {} got {} chats", userId, chats.chats().size());
            return ResultOneArg.success(chats);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get user {} chats: {}", userId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting user {} chats: {}", userId, e.getMessage());
            return ResultOneArg.error("getUserChatsPage failed due to server error");
        }
    }
    public ResultOneArg<ChatStatsResult> getChatStats(long chatId, long userId) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            ChatStatsResult result = dataOrchestrator.getChatClearStats(chatId, userId);

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
            ChatSecurityDTO chat = validator.validateActiveUserInActiveChatAndGetChat(chatId, userId);
            return ResultOneArg.success(chat.isActionsEnabled());
        } catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get enabled actions for chat {}: {}", chatId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        } catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting enabled actions for chat {}: {}", chatId, e.getMessage());
            return ResultOneArg.error("isActionsEnabledForChat failed due to server error");
        }
    }
}