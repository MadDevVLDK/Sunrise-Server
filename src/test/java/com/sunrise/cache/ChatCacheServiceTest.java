package com.sunrise.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sunrise.cache.entity.Cache.Chat;
import com.sunrise.cache.service.ChatCacheService;
import com.sunrise.orchestrator.type.ChatType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ChatCacheServiceTest {

    private Cache<Long, Chat> chatInfoCache;
    private Cache<String, Long> personalChatIndex;
    private ChatCacheService chatCacheService;

    @BeforeEach
    void setUp() {
        chatInfoCache = Caffeine.newBuilder().build();
        personalChatIndex = Caffeine.newBuilder().build();
        chatCacheService = new ChatCacheService(chatInfoCache, personalChatIndex);
    }

    @Test
    void save_shouldStoreChatAndIndexForPersonal() {
        Chat chat = new Chat(1L, null, null, ChatType.PERSONAL, 2L, 2,
                Instant.now(), Instant.now(), 1L, null, false);
        chatCacheService.save(chat);

        assertThat(chatInfoCache.getIfPresent(1L)).isNotNull();
        String key = "1:2";
        assertThat(personalChatIndex.getIfPresent(key)).isEqualTo(1L);
    }

    @Test
    void save_shouldNotIndexForGroupChat() {
        Chat chat = new Chat(2L, "Group", null, ChatType.GROUP, null, 5,
                Instant.now(), Instant.now(), 1L, null, false);
        chatCacheService.save(chat);

        assertThat(chatInfoCache.getIfPresent(2L)).isNotNull();
        assertThat(personalChatIndex.asMap()).isEmpty();
    }

    @Test
    void saveBatch_shouldStoreAllAndIndexPersonal() {
        Chat group = new Chat(1L, "Group", null, ChatType.GROUP, null, 5,
                Instant.now(), Instant.now(), 1L, null, false);
        Chat personal = new Chat(2L, null, null, ChatType.PERSONAL, 3L, 2,
                Instant.now(), Instant.now(), 2L, null, false);
        chatCacheService.saveBatch(List.of(group, personal));

        assertThat(chatInfoCache.getIfPresent(1L)).isNotNull();
        assertThat(chatInfoCache.getIfPresent(2L)).isNotNull();
        String key = "2:3";
        assertThat(personalChatIndex.getIfPresent(key)).isEqualTo(2L);
    }

    @Test
    void get_whenExists_shouldReturnCopy() {
        Chat original = new Chat(1L, "Name", "Desc", ChatType.GROUP, null, 10,
                Instant.now(), Instant.now(), 1L, null, false);
        chatInfoCache.put(1L, original);

        Optional<Chat> result = chatCacheService.get(1L);

        assertThat(result).isPresent();
        // проверяем, что вернулась копия (разные объекты)
        assertThat(result.get()).isNotSameAs(original);
        assertThat(result.get().getName()).isEqualTo("Name");
    }

    @Test
    void getChatLink_shouldReturnSameInstance() {
        Chat chat = new Chat(1L, "Name", "Desc", ChatType.GROUP, null, 10,
                Instant.now(), Instant.now(), 1L, null, false);
        chatInfoCache.put(1L, chat);

        Optional<Chat> result = chatCacheService.getChatLink(1L);

        assertThat(result).contains(chat);
    }

    @Test
    void getPersonalChat_whenExists_shouldReturn() {
        long u1 = 10L, u2 = 20L;
        String key = "10:20";
        personalChatIndex.put(key, 100L);
        Chat chat = new Chat(100L, null, null, ChatType.PERSONAL, 20L, 2,
                Instant.now(), Instant.now(), 10L, null, false);
        chatInfoCache.put(100L, chat);

        Optional<Chat> result = chatCacheService.getPersonalChat(u1, u2);

        assertThat(result).contains(chat);
    }

    @Test
    void getPersonalChat_whenIndexExistsButChatMissing_shouldInvalidateIndex() {
        personalChatIndex.put("10:20", 100L);
        // chatInfoCache empty

        Optional<Chat> result = chatCacheService.getPersonalChat(10L, 20L);

        assertThat(result).isEmpty();
        assertThat(personalChatIndex.getIfPresent("10:20")).isNull();
    }

    @Test
    void increaseChatMemberCounter_shouldModifyExistingChat() {
        Chat chat = new Chat(1L, "Group", null, ChatType.GROUP, null, 5,
                Instant.now(), Instant.now(), 1L, null, false);
        chatInfoCache.put(1L, chat);

        chatCacheService.increaseChatMemberCounter(1L, 3);

        assertThat(chat.getMembersCount()).isEqualTo(8);
    }

    @Test
    void decreaseChatMemberCounter_shouldModifyExistingChat() {
        Chat chat = new Chat(1L, "Group", null, ChatType.GROUP, null, 5,
                Instant.now(), Instant.now(), 1L, null, false);
        chatInfoCache.put(1L, chat);

        chatCacheService.decreaseChatMemberCounter(1L, 2);

        assertThat(chat.getMembersCount()).isEqualTo(3);
    }

    @Test
    void invalidate_shouldRemoveChatAndPersonalIndex() {
        Chat chat = new Chat(1L, null, null, ChatType.PERSONAL, 2L, 2,
                Instant.now(), Instant.now(), 1L, null, false);
        chatInfoCache.put(1L, chat);
        personalChatIndex.put("1:2", 1L);

        chatCacheService.invalidate(1L);

        assertThat(chatInfoCache.getIfPresent(1L)).isNull();
        assertThat(personalChatIndex.getIfPresent("1:2")).isNull();
    }
}