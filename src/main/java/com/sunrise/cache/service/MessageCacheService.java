package com.sunrise.cache.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.sunrise.cache.entity.Cache.Message;
import com.sunrise.helpclass.mapper.MessageMapper;
import com.sunrise.orchestrator.type.Direction;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

@Slf4j
@Service
public class MessageCacheService {

    private final Cache<Long, Message> messageCache;
    private final Cache<Long, NavigableSet<Long>> recentMessagesIdsCache;
    private final int maxMessagesPerChat;

     public MessageCacheService(Cache<Long, Message> messageCache,
                                @Qualifier("recentMessagesIdsCache") Cache<Long, NavigableSet<Long>> recentMessagesIdsCache,
                                @Value("${app.cache.max-per-chat.messages:200}") int maxMessagesPerChat) {
        
        this.messageCache = messageCache;
        this.recentMessagesIdsCache = recentMessagesIdsCache;
        this.maxMessagesPerChat = maxMessagesPerChat;
    }


    // ========== MESSAGES METHODS ==========

    public void save(Message message) {
        Message copy = MessageMapper.copy(message);
        messageCache.put(copy.id(), copy);
        log.debug("[⚡] ✉️ Saved message {} in cache (chat={}, sender={})", copy.id(), copy.chatId(), copy.senderId());
    }

    public void saveBatch(List<Message> messages) {
        for (Message message : messages) {
            messageCache.put(message.id(), MessageMapper.copy(message));
        }
        log.debug("[⚡] ✉️ Batch saved {} messages to cache", messages.size());
    }

    public void invalidate(long messageId) {
        messageCache.invalidate(messageId);
        log.debug("[⚡] ✉️🚫 Invalidated message {} in cache", messageId);
    }

    public Optional<Message> get(long messageId) {
        return Optional.ofNullable(MessageMapper.copy(messageCache.getIfPresent(messageId)));
    }


    // ========== RECENT CHAT MESSAGES METHODS ==========


    public void saveRecentIds(long chatId, List<Long> messageIds) {
        NavigableSet<Long> set = new ConcurrentSkipListSet<>(Comparator.reverseOrder());
        set.addAll(messageIds);
        while (set.size() > maxMessagesPerChat) {
            set.pollLast();
        }
        recentMessagesIdsCache.put(chatId, set);
        log.debug("[⚡] ✉️ Initialized recent IDs for chat {}: {} messages", chatId, set.size());
    }
    
    public void addToRecentIds(long chatId, long messageId) {
        NavigableSet<Long> set = recentMessagesIdsCache.getIfPresent(chatId);
        if (set == null) return; // кеша для этого чата нет — ничего не делаем

        set.add(messageId);
        while (set.size() > maxMessagesPerChat) {
            set.pollLast();
        }
        log.debug("[⚡] ✉️ Added message {} to recent cache for chat {}, new size={}", messageId, chatId, set.size());
    }

    public List<Long> getRecentIdsRange(long chatId, Long cursor, int limit, Direction direction) {
        NavigableSet<Long> set = recentMessagesIdsCache.getIfPresent(chatId);
        if (set == null) return Collections.emptyList();

        if (direction == Direction.FORWARD) { // FORWARD
            if (cursor == null) {
                // Начало пагинации вперёд — самые новые сообщения
                return set.stream().limit(limit).toList();
            } else {
                // Получаем элементы строго больше курсора (более новые), сохраняя порядок убывания
                return set.tailSet(cursor, false).stream().limit(limit).toList();
            }
        } else { // BACKWARD
            if (cursor == null) {
                // Начало пагинации назад — тоже самые новые (по умолчанию)
                return set.stream().limit(limit).toList();
            } else {
                // Получаем элементы строго меньше курсора (более старые)
                return set.headSet(cursor, false).stream().limit(limit).toList();
            }
        }
    }

    public boolean hasRecentIds(long chatId) {
        return recentMessagesIdsCache.getIfPresent(chatId) != null;
    }

    public boolean isRecentIdsFull(long chatId) {
        NavigableSet<Long> set = recentMessagesIdsCache.getIfPresent(chatId);
        return set != null && set.size() == maxMessagesPerChat;
    }

    public int getMaxMessagesResentIdsPerChat() {
        return maxMessagesPerChat;
    }
}