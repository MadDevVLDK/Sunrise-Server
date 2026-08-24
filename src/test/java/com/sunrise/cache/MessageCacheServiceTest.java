package com.sunrise.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sunrise.cache.entity.Cache.Message;
import com.sunrise.cache.service.MessageCacheService;
import com.sunrise.orchestrator.type.Direction;
import com.sunrise.orchestrator.type.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class MessageCacheServiceTest {

    private Cache<Long, Message> messageCache;
    private Cache<Long, NavigableSet<Long>> recentMessagesIdsCache;
    private MessageCacheService messageCacheService;

    @BeforeEach
    void setUp() {
        messageCache = Caffeine.newBuilder().build();
        recentMessagesIdsCache = Caffeine.newBuilder().build();
        messageCacheService = new MessageCacheService(messageCache, recentMessagesIdsCache, 3);
    }

    @Test
    void save_shouldStoreMessageCopy() {
        Message msg = new Message(1L, 100L, MessageType.COMMON, 10L, "Hello", new AtomicBoolean(false),
                Instant.now(), Instant.now(), null, false);
        messageCacheService.save(msg);

        Message stored = messageCache.getIfPresent(1L);
        assertThat(stored).isNotNull();
        assertThat(stored).isNotSameAs(msg);
        assertThat(stored.text()).isEqualTo("Hello");
    }

    @Test
    void saveBatch_shouldStoreAll() {
        Message m1 = new Message(1L, 100L, MessageType.COMMON, 10L, "Hi", new AtomicBoolean(false),
                Instant.now(), Instant.now(), null, false);
        Message m2 = new Message(2L, 100L, MessageType.COMMON, 11L, "Hey", new AtomicBoolean(false),
                Instant.now(), Instant.now(), null, false);
        messageCacheService.saveBatch(List.of(m1, m2));

        assertThat(messageCache.getIfPresent(1L)).isNotNull();
        assertThat(messageCache.getIfPresent(2L)).isNotNull();
    }

    @Test
    void getLink_shouldReturnSameInstance() {
        Message msg = new Message(1L, 100L, MessageType.COMMON, 10L, "text", new AtomicBoolean(false),
                Instant.now(), Instant.now(), null, false);
        messageCache.put(1L, msg);

        Optional<Message> result = messageCacheService.getLink(1L);

        assertThat(result).contains(msg);
    }

    @Test
    void get_shouldReturnCopy() {
        Message msg = new Message(1L, 100L, MessageType.COMMON, 10L, "text", new AtomicBoolean(false),
                Instant.now(), Instant.now(), null, false);
        messageCache.put(1L, msg);

        Optional<Message> result = messageCacheService.get(1L);

        assertThat(result).isPresent();
        assertThat(result.get()).isNotSameAs(msg);
    }

    @Test
    void markAsReadBatch_shouldSetAtomicBooleanToTrue() {
        AtomicBoolean readFlag = new AtomicBoolean(false);
        Message msg = new Message(1L, 100L, MessageType.COMMON, 10L, "text", readFlag,
                Instant.now(), Instant.now(), null, false);
        messageCache.put(1L, msg);

        messageCacheService.markAsReadBatch(List.of(1L));

        assertThat(readFlag.get()).isTrue();
    }

    @Test
    void invalidate_shouldRemove() {
        messageCache.put(1L, mockMessage());
        messageCacheService.invalidate(1L);
        assertThat(messageCache.getIfPresent(1L)).isNull();
    }

    // ---------- recent messages IDs ----------

    @Test
    void saveRecentIds_shouldStoreSetWithLimit() {
        List<Long> ids = List.of(100L, 90L, 80L, 70L);
        messageCacheService.saveRecentIds(1L, ids);

        NavigableSet<Long> set = recentMessagesIdsCache.getIfPresent(1L);
        assertThat(set).containsExactly(100L, 90L, 80L); // limited to 3, descending
    }

    @Test
    void addToRecentIds_whenCacheExists_shouldAddAndLimit() {
        NavigableSet<Long> set = new ConcurrentSkipListSet<>(Comparator.reverseOrder());
        set.addAll(List.of(50L, 40L));
        recentMessagesIdsCache.put(1L, set);

        messageCacheService.addToRecentIds(1L, 60L);
        messageCacheService.addToRecentIds(1L, 55L);
        messageCacheService.addToRecentIds(1L, 30L); // > limit

        assertThat(set).containsExactly(60L, 55L, 50L);
        assertThat(set).hasSize(3);
    }

    @Test
    void getRecentIdsRange_withoutCursor_shouldReturnFirstLimit() {
        NavigableSet<Long> set = new ConcurrentSkipListSet<>(Comparator.reverseOrder());
        set.addAll(List.of(100L, 90L, 80L, 70L));
        recentMessagesIdsCache.put(1L, set);

        List<Long> result = messageCacheService.getRecentIdsRange(1L, null, 2, Direction.BACKWARD);

        assertThat(result).containsExactly(100L, 90L);
    }

    @Test
    void getRecentIdsRange_withCursorForward_shouldReturnHeadSet() {
        NavigableSet<Long> set = new ConcurrentSkipListSet<>(Comparator.reverseOrder());
        set.addAll(List.of(100L, 90L, 80L, 70L));
        recentMessagesIdsCache.put(1L, set);

        List<Long> result = messageCacheService.getRecentIdsRange(1L, 90L, 2, Direction.FORWARD);

        // FORWARD: cursor=90, return IDs > 90 (more recent) – headSet(90, false)
        assertThat(result).containsExactly(100L);
    }

    @Test
    void getRecentIdsRange_withCursorBackward_shouldReturnTailSet() {
        NavigableSet<Long> set = new ConcurrentSkipListSet<>(Comparator.reverseOrder());
        set.addAll(List.of(100L, 90L, 80L, 70L));
        recentMessagesIdsCache.put(1L, set);

        List<Long> result = messageCacheService.getRecentIdsRange(1L, 90L, 2, Direction.BACKWARD);

        // BACKWARD: cursor=90, return IDs < 90 (older) – tailSet(90, false)
        assertThat(result).containsExactly(80L, 70L);
    }

    @Test
    void hasRecentIds_shouldReturnTrueIfPresent() {
        recentMessagesIdsCache.put(1L, new ConcurrentSkipListSet<>());
        assertThat(messageCacheService.hasRecentIds(1L)).isTrue();
        assertThat(messageCacheService.hasRecentIds(2L)).isFalse();
    }

    @Test
    void isRecentIdsFull_shouldReturnTrueWhenSizeEqualsMax() {
        NavigableSet<Long> set = new ConcurrentSkipListSet<>(Comparator.reverseOrder());
        set.addAll(List.of(1L, 2L, 3L));
        recentMessagesIdsCache.put(1L, set);
        assertThat(messageCacheService.isRecentIdsFull(1L)).isTrue();
    }

    private Message mockMessage() {
        return new Message(1L, 100L, MessageType.COMMON, 10L, "text", new AtomicBoolean(false),
                Instant.now(), Instant.now(), null, false);
    }
}