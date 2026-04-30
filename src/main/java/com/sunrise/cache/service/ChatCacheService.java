package com.sunrise.cache.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.sunrise.cache.entity.Cache.Chat;
import com.sunrise.helpclass.mapper.ChatMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class ChatCacheService {

    private final Cache<Long, Chat> chatInfoCache;
    private final Cache<String, Long> personalChatIndex;

    public ChatCacheService(Cache<Long, Chat> chatInfoCache, 
                            @Qualifier("personalChatIndex") Cache<String, Long> personalChatIndex) {

        this.chatInfoCache = chatInfoCache;
        this.personalChatIndex = personalChatIndex;
    }


    // ========== CHAT METHODS ==========

    public void saveBatch(Collection<Chat> newChats) {
        for (Chat newChat : newChats) {
            chatInfoCache.put(newChat.getId(), ChatMapper.copy(newChat));
            if (newChat.isPersonal()) {
                savePersonalChatIndex(newChat.getId(), newChat.getCreatedBy(), newChat.getOpponentId());
            }
        }
        log.debug("[⚡] 💬 Batch saved {} chats to cache and updated indexes", newChats.size());
    }

    public void save(Chat newChat) {
        chatInfoCache.put(newChat.getId(), ChatMapper.copy(newChat));
        if (newChat.isPersonal()) {
            savePersonalChatIndex(newChat.getId(), newChat.getCreatedBy(), newChat.getOpponentId());
        }
        log.debug("[⚡] 💬 Saved chat {} in cache and updated indexes", newChat.getId());
    }

    public void increaseChatMemberCounter(long chatId, int numToAdd) {
        getChatLink(chatId).ifPresent(chat -> chat.increaseMembersCount(numToAdd));
        log.debug("[⚡] 💬🧑‍🔧 Dirty increase of membersCount in chat", chatId);
    }
    public void decreaseChatMemberCounter(long chatId, int numToSubtract) {
        getChatLink(chatId).ifPresent(chat -> chat.decreaseMembersCount(numToSubtract));
        log.debug("[⚡] 💬🧑‍🔧 Dirty decrease of membersCount in chat {} in cache", chatId);
    }

    public void invalidate(long chatId) {
        chatInfoCache.invalidate(chatId);
        log.debug("[⚡] 💬🚫 Invalidated chat {} in cache", chatId);
    }

    public Optional<Chat> getPersonalChat(long userId1, long userId2) {
        String key = getPersonalChatKey(userId1, userId2);
        Long chatId = personalChatIndex.getIfPresent(key);
        if (chatId == null) {
            log.debug("[⚡] 💬🔍 Personal chat between {} and {} not found in index", userId1, userId2);
            return Optional.empty();
        }
        Optional<Chat> chat = getChatLink(chatId);
        if (chat.isEmpty()) {
            personalChatIndex.invalidate(key);
            log.debug("[⚡] 💬🔍 Personal chat index {} invalidated (chat not in cache)", key);
        }
        return chat;
    }

    public Optional<Chat> get(long chatId) {
        return Optional.ofNullable(ChatMapper.copy(chatInfoCache.getIfPresent(chatId)));
    }

    public Optional<Chat> getChatLink(long chatId) {
        return Optional.ofNullable(chatInfoCache.getIfPresent(chatId));
    }

    private String getPersonalChatKey(long userId1, long userId2) {
        return Math.min(userId1, userId2) + ":" + Math.max(userId1, userId2);
    }

    public void savePersonalChatIndex(long chatId, long creatorId, long opponentId) {
        personalChatIndex.put(getPersonalChatKey(creatorId, opponentId), chatId);
        log.debug("[⚡] 💬📌 Saved personal chat index for users {} and {} -> chat {}", creatorId, opponentId, chatId);
    }
}