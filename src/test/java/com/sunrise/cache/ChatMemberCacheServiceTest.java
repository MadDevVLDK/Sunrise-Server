package com.sunrise.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sunrise.cache.entity.Cache.ChatMember;
import com.sunrise.cache.service.ChatMemberCacheService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMemberCacheServiceTest {

    private Cache<String, ChatMember> chatMembersCache;
    private Cache<Long, NavigableSet<Long>> recentChatMembersIdsCache;
    private ChatMemberCacheService chatMemberCacheService;

    @BeforeEach
    void setUp() {
        chatMembersCache = Caffeine.newBuilder().build();
        recentChatMembersIdsCache = Caffeine.newBuilder().build();
        chatMemberCacheService = new ChatMemberCacheService(chatMembersCache, recentChatMembersIdsCache, 3);
    }

    @Test
    void save_shouldStoreMember() {
        ChatMember member = new ChatMember(1L, 10L, "tag", Instant.now(), Instant.now(), Instant.now(),
                false, true, null, false);
        chatMemberCacheService.save(member);

        String key = "1:10";
        assertThat(chatMembersCache.getIfPresent(key)).isNotNull();
    }

    @Test
    void saveBatch_shouldStoreAll() {
        ChatMember m1 = new ChatMember(1L, 10L, null, Instant.now(), Instant.now(), Instant.now(), false, false, null, false);
        ChatMember m2 = new ChatMember(1L, 11L, null, Instant.now(), Instant.now(), Instant.now(), false, false, null, false);
        chatMemberCacheService.saveBatch(1L, List.of(m1, m2));

        assertThat(chatMembersCache.getIfPresent("1:10")).isNotNull();
        assertThat(chatMembersCache.getIfPresent("1:11")).isNotNull();
    }

    @Test
    void get_whenExists_shouldReturnCopy() {
        ChatMember original = new ChatMember(1L, 10L, "tag", Instant.now(), Instant.now(), Instant.now(),
                false, true, null, false);
        chatMembersCache.put("1:10", original);

        Optional<ChatMember> result = chatMemberCacheService.get(1L, 10L);

        assertThat(result).isPresent();
        assertThat(result.get()).isNotSameAs(original);
        assertThat(result.get().tag()).isEqualTo("tag");
    }

    @Test
    void getLink_shouldReturnSameInstance() {
        ChatMember original = new ChatMember(1L, 10L, "tag", Instant.now(), Instant.now(), Instant.now(),
                false, true, null, false);
        chatMembersCache.put("1:10", original);

        Optional<ChatMember> result = chatMemberCacheService.getLink(1L, 10L);

        assertThat(result).contains(original);
    }

    @Test
    void getBatch_shouldReturnPresentAndCollectMissing() {
        ChatMember m1 = new ChatMember(1L, 10L, null, Instant.now(), Instant.now(), Instant.now(), false, false, null, false);
        chatMembersCache.put("1:10", m1);
        // 11 missing

        List<Long> missingIds = new ArrayList<>();
        Map<Long, ChatMember> result = chatMemberCacheService.getBatch(1L, List.of(10L, 11L), missingIds);

        assertThat(result).containsKey(10L).doesNotContainKey(11L);
        assertThat(missingIds).containsExactly(11L);
    }

    @Test
    void invalidate_shouldRemoveMember() {
        chatMembersCache.put("1:10", mockMember());
        chatMemberCacheService.invalidate(1L, 10L);
        assertThat(chatMembersCache.getIfPresent("1:10")).isNull();
    }

    // ---------- recent IDs methods ----------

    @Test
    void saveResentIds_shouldStoreSet() {
        List<Long> ids = List.of(30L, 20L, 10L);
        chatMemberCacheService.saveResentIds(1L, ids);

        NavigableSet<Long> set = recentChatMembersIdsCache.getIfPresent(1L);
        assertThat(set).containsExactly(30L, 20L, 10L); // descending order
    }

    @Test
    void addToRecentIds_whenCacheExists_shouldAddAndLimit() {
        NavigableSet<Long> set = new ConcurrentSkipListSet<>(Comparator.reverseOrder());
        set.addAll(List.of(100L, 90L));
        recentChatMembersIdsCache.put(1L, set);

        chatMemberCacheService.addToRecentIds(1L, 200L);
        chatMemberCacheService.addToRecentIds(1L, 110L);
        chatMemberCacheService.addToRecentIds(1L, 80L); // 4 items, but max=3

        assertThat(set).containsExactly(200L, 110L, 100L);
        assertThat(set).hasSize(3);
    }

    @Test
    void addToRecentIds_whenCacheMissing_shouldDoNothing() {
        chatMemberCacheService.addToRecentIds(1L, 123L);
        assertThat(recentChatMembersIdsCache.getIfPresent(1L)).isNull();
    }

    @Test
    void removeFromRecentIds_whenCacheExists_shouldRemove() {
        NavigableSet<Long> set = new ConcurrentSkipListSet<>(Comparator.reverseOrder());
        set.addAll(List.of(50L, 40L, 30L));
        recentChatMembersIdsCache.put(1L, set);

        chatMemberCacheService.removeFromRecentIds(1L, 40L);

        assertThat(set).containsExactly(50L, 30L);
    }

    @Test
    void getRecentIdsRange_withoutCursor_shouldReturnFirstLimit() {
        NavigableSet<Long> set = new ConcurrentSkipListSet<>(Comparator.reverseOrder());
        set.addAll(List.of(100L, 90L, 80L, 70L));
        recentChatMembersIdsCache.put(1L, set);

        List<Long> result = chatMemberCacheService.getRecentIdsRange(1L, null, 2);

        assertThat(result).containsExactly(100L, 90L);
    }

    @Test
    void getRecentIdsRange_withCursor_shouldReturnTailSet() {
        NavigableSet<Long> set = new ConcurrentSkipListSet<>(Comparator.reverseOrder());
        set.addAll(List.of(100L, 90L, 80L, 70L));
        recentChatMembersIdsCache.put(1L, set);

        List<Long> result = chatMemberCacheService.getRecentIdsRange(1L, 90L, 2);

        assertThat(result).containsExactly(80L, 70L);
    }

    @Test
    void hasResentIds_shouldReturnTrueIfPresent() {
        recentChatMembersIdsCache.put(1L, new ConcurrentSkipListSet<>());
        assertThat(chatMemberCacheService.hasResentIds(1L)).isTrue();
        assertThat(chatMemberCacheService.hasResentIds(2L)).isFalse();
    }

    @Test
    void invalidateResentIds_shouldRemove() {
        recentChatMembersIdsCache.put(1L, new ConcurrentSkipListSet<>());
        chatMemberCacheService.invalidateResentIds(1L);
        assertThat(recentChatMembersIdsCache.getIfPresent(1L)).isNull();
    }

    @Test
    void isRecentIdsFull_shouldReturnTrueWhenSizeEqualsMax() {
        NavigableSet<Long> set = new ConcurrentSkipListSet<>(Comparator.reverseOrder());
        set.addAll(List.of(1L, 2L, 3L));
        recentChatMembersIdsCache.put(1L, set);
        assertThat(chatMemberCacheService.isRecentIdsFull(1L)).isTrue();
    }

    private ChatMember mockMember() {
        return new ChatMember(1L, 10L, null, Instant.now(), Instant.now(), Instant.now(),
                false, false, null, false);
    }
}