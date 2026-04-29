package com.sunrise.cache.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.sunrise.cache.entity.CacheChatMember;
import com.sunrise.helpclass.mapper.ChatMemberMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

@Slf4j
@Service
public class ChatMemberCacheService {

    private final Cache<String, CacheChatMember> chatMembersCache;
    private final Cache<Long, NavigableSet<Long>> recentChatMembersIdsCache;
    private final int maxMembersPerChat;

    public ChatMemberCacheService(Cache<String, CacheChatMember> chatMembersCache,
                                  @Qualifier("recentChatMembersIdsCache") Cache<Long, NavigableSet<Long>> recentChatMembersIdsCache,
                                  @Value("${app.cache.max-per-chat.chat-members:500}") int maxMembersPerChat) {
        
                                    this.chatMembersCache = chatMembersCache;
        this.recentChatMembersIdsCache = recentChatMembersIdsCache;
        this.maxMembersPerChat = maxMembersPerChat;
    }


    // ========== CHAT MEMBER METHODS ==========

    public void saveBatch(long chatId, Collection<CacheChatMember> members)  {
        for (CacheChatMember member : members) {
            chatMembersCache.put(getKey(member.getChatId(), member.getUserId()), ChatMemberMapper.copy(member));
        }
        log.debug("[⚡] 📦👥 Batch saved {} chat members in chat {}", members.size(), chatId);
    }
    
    public void save(CacheChatMember member) {
        chatMembersCache.put(getKey(member.getChatId(), member.getUserId()), ChatMemberMapper.copy(member));
        log.debug("[⚡] 📦👤 Saved chat member {} in chat {}", member.getUserId(), member.getChatId());
    }

    public void invalidate(long chatId, long userId) {
        chatMembersCache.invalidate(getKey(chatId, userId));
    }

    public Optional<CacheChatMember> get(long chatId, long userId) {
        return Optional.ofNullable(ChatMemberMapper.copy(chatMembersCache.getIfPresent(getKey(chatId, userId))));
    }
    public Optional<CacheChatMember> getLink(long chatId, long userId) {
        return Optional.ofNullable(chatMembersCache.getIfPresent(getKey(chatId, userId)));
    }

    public Map<Long, CacheChatMember> getBatch(long chatId, Collection<Long> userIds, Collection<Long> missingIds) {
        Map<Long, CacheChatMember> result = new HashMap<>(userIds.size());
        for (long userId : userIds) {
            Optional<CacheChatMember> member = getLink(chatId, userId);
            if (member.isPresent()) {
                result.put(userId, member.get());
            } else {
                missingIds.add(userId);
            }
        }
        log.debug("[⚡] 📦🔍 Retrieved {} chat members from cache for chat {}, {} missing", result.size(), chatId, missingIds != null ? missingIds.size() : 0);
        return result;
    }

    public String getKey(long chatId, long userId){
        return chatId + ":" + userId;
    }

    public Optional<Boolean> hasActive(long chatId, long userId) {
        return getLink(chatId, userId).map(member -> member.isActive());
    }

    public Optional<Boolean> isActiveAdmin(long chatId, long userId) {
        return getLink(chatId, userId).map(member -> member.isActive() && member.isAdmin());
    }

    
    // ========== CHAT MEMBERS IDS CACHE ==========

    public void saveResentIds(long chatId, Collection<Long> ids) {
        NavigableSet<Long> set = new ConcurrentSkipListSet<>(Comparator.reverseOrder());
        set.addAll(ids);
        recentChatMembersIdsCache.put(chatId, set);
        log.debug("[⚡] 📦 Saved {} member IDs for chat {}", ids.size(), chatId);
    }
    
    public void addToRecentIds(long chatId, long messageId) {
        NavigableSet<Long> set = recentChatMembersIdsCache.getIfPresent(chatId);
        if (set == null) return; // кеша для этого чата нет — ничего не делаем

        set.add(messageId);
        while (set.size() > maxMembersPerChat) {
            set.pollLast();
        }
        log.debug("[⚡] ✉️ Added member id {} to recent cache for chat {}, new size={}", messageId, chatId, set.size());
    }

    public List<Long> getRecentIdsRange(long chatId, Long cursor, int limit) {
        NavigableSet<Long> idsSet = recentChatMembersIdsCache.getIfPresent(chatId);
        if (cursor == null) {
            return idsSet.stream().limit(limit).toList();
        } else {
            return idsSet.tailSet(cursor, false).stream().limit(limit).toList();
        }
    }

    public boolean hasResentIds(long chatId) {
        return recentChatMembersIdsCache.getIfPresent(chatId) != null;
    }

    public void invalidateResentIds(long chatId) {
        recentChatMembersIdsCache.invalidate(chatId);
        log.debug("[⚡] 📦🚫 Invalidated member IDs for chat {}", chatId);
    }

    public boolean isRecentIdsFull(long chatId) {
        NavigableSet<Long> set = recentChatMembersIdsCache.getIfPresent(chatId);
        return set != null && set.size() == maxMembersPerChat;
    }

    public int getMaxMembersResentIdsPerChat() {
        return maxMembersPerChat;
    }
}