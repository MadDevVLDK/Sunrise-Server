package com.sunrise.core.dataservice;

import com.sunrise.entity.cache.*;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("NullableProblems")
@Slf4j
@Service
public class CacheService {

    // кэш пользователей
    private final Cache<Long, CacheUserSecurity> userSecurityCache = Caffeine.newBuilder()  // userId -> CacheUserProfile (пользователи)
            .maximumSize(100_000)
            .expireAfterAccess(1, TimeUnit.HOURS) // 1 h
            .recordStats()
            .build();

    private final Cache<Long, CacheUserProfile> userProfileCache = Caffeine.newBuilder()  // userId -> CacheUserProfile (пользователи)
            .maximumSize(100_000)
            .expireAfterAccess(1, TimeUnit.HOURS) // 1 h
            .recordStats()
            .build();

    private final Cache<String, Long> usernameIndex = Caffeine.newBuilder() // username -> userId (для регистрации)
            .maximumSize(100_000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .softValues()  // при нехватке памяти delete-аем
            .recordStats()
            .build();
    private final Cache<String, Long> emailIndex = Caffeine.newBuilder() // email -> userId (для регистрации)
            .maximumSize(150_000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .softValues()  // при нехватке памяти delete-аем
            .recordStats()
            .build();


    // кэш чатов
    private final Cache<Long, CacheChat> chatInfoCache = Caffeine.newBuilder() // chatId -> CacheChat (чаты)
            .maximumSize(50_000)
            .expireAfterAccess(12, TimeUnit.HOURS) // 12 h
            .build();

    private final Cache<String, Long> personalChatIndex = Caffeine.newBuilder() // "creatorId:userId" -> chatId (личные чаты)
            .maximumSize(100_000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .softValues() // при нехватке памяти delete-аем
            .recordStats()
            .build();


    // контейнеры участников чата
    private final Cache<Long, CacheChatMembersContainer> chatMembersCache = Caffeine.newBuilder() // chatId -> CacheChatMembersContainer (контейнер с участниками чата)
            .maximumSize(100_000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .recordStats()
            .build();

    private final Cache<Long, CacheMessageSecurity> messageCache = Caffeine.newBuilder() // messageId -> CacheMessageSecurity (сообщения)
            .maximumSize(200_000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .recordStats()
            .build();


    // кеш токенов подтверждения
    private final Cache<String, CacheVerificationToken> verificationTokenCache = Caffeine.newBuilder() // token -> CacheVerificationToken (токены подтверждения)
            .maximumSize(50_000)
            .expireAfterWrite(2, TimeUnit.HOURS)
            .build();



    // ========== USER METHODS ==========


    // Основные методы
    public void saveUsersProfile(Collection<CacheUserProfile> users) {
        for (CacheUserProfile user : users){
            userProfileCache.put(user.getId(), CacheUserProfile.copy(user));
            usernameIndex.put(user.getUsername().toLowerCase(), user.getId());
        }
        log.debug("[⚡] Batch saved {} users to cache and updated indexes || saveUsers", users.size());
    }
    public void saveUserSecurity(CacheUserSecurity user) {
        userSecurityCache.put(user.getId(), CacheUserSecurity.copy(user));
        emailIndex.put(user.getEmail().toLowerCase(), user.getId());
        log.debug("[⚡] Saved user security {} in cache and updated indexes || saveUserSecurity", user.getId());
    }
    public void saveUserProfile(CacheUserProfile user) {
        userProfileCache.put(user.getId(), CacheUserProfile.copy(user));
        usernameIndex.put(user.getUsername().toLowerCase(), user.getId());
        log.debug("[⚡] Saved user profile {} in cache and updated indexes || saveUserProfile", user.getId());
    }
    public void invalidateUserSecurity(long userId) {
        userSecurityCache.invalidate(userId);
        log.debug("[⚡] Invalidated user security {} in cache || invalidateUserSecurity", userId);
    }
    public void invalidateUserProfile(long userId) {
        userProfileCache.invalidate(userId);
        log.debug("[⚡] Invalidated user profile {} in cache || invalidateUserProfile", userId);
    }


    // Вспомогательные методы
    public Map<Long, CacheUserProfile> getCacheUsersByIds(Collection<Long> userIds, Collection<Long> missingIds) {
        Map<Long, CacheUserProfile> result = new HashMap<>(userIds.size());
        for (Long userId : userIds) {
            Optional<CacheUserProfile> user = getUserProfile(userId);
            if (user.isPresent()) {
                result.put(userId, user.get());
            } else if (missingIds != null) {
                missingIds.add(userId);
            }
        }
        return result;
    }
    public Optional<CacheUserSecurity> getUserSecurityByUsername(String username) {
        String key = username.toLowerCase();
        Long userId = usernameIndex.getIfPresent(key);
        if (userId == null) return Optional.empty();

        Optional<CacheUserSecurity> user = getUserSecurity(userId);
        if (user.isEmpty()) {
            usernameIndex.invalidate(key);
        }
        return user;
    }
    public Optional<CacheUserSecurity> getUserSecurityByEmail(String email) {
        String key = email.toLowerCase();
        Long userId = emailIndex.getIfPresent(key);
        if (userId == null) return Optional.empty();

        Optional<CacheUserSecurity> user = getUserSecurity(userId);
        if (user.isEmpty()) {
            emailIndex.invalidate(key);
        }
        return user;
    }
    public Optional<CacheUserSecurity> getUserSecurity(long userId) {
        return Optional.ofNullable(CacheUserSecurity.copy(userSecurityCache.getIfPresent(userId)));
    }

    public Optional<CacheUserProfile> getUserProfile(long userId) {
        return Optional.ofNullable(CacheUserProfile.copy(userProfileCache.getIfPresent(userId)));
    }

    public boolean existsUserByUsername(String username) {
        return usernameIndex.getIfPresent(username.toLowerCase()) != null;
    }
    public boolean existsUserByEmail(String email) {
        return emailIndex.getIfPresent(email.toLowerCase()) != null;
    }


    // ========== CHAT METHODS ==========


    // Основные методы
    public void saveChats(Collection<CacheChat> newChats) {
        for (CacheChat newChat : newChats){
            chatInfoCache.put(newChat.getId(), CacheChat.copy(newChat));
            if (newChat.isPersonal()) {
                savePersonalChatIndex(newChat.getId(), newChat.getCreatedBy(), newChat.getOpponentId());
            }
        }
        log.debug("[⚡] Batch saved {} chats to cache and updated indexes || saveChats", newChats.size());
    }
    public void saveChat(CacheChat newChat) {
        chatInfoCache.put(newChat.getId(), CacheChat.copy(newChat));
        if (newChat.isPersonal()) {
            savePersonalChatIndex(newChat.getId(), newChat.getCreatedBy(), newChat.getOpponentId());
        }
        log.debug("[⚡] Saved chat {} in cache and updated indexes || saveChat", newChat.getId());
    }
    public void saveChatAndAddMembers(CacheChat newChat, Collection<CacheChatMember> members) {
        long chatId = newChat.getId();

        // добавляем чат
        chatInfoCache.put(newChat.getId(), CacheChat.copy(newChat));
        if (newChat.isPersonal()) {
            savePersonalChatIndex(newChat.getId(), newChat.getCreatedBy(), newChat.getOpponentId());
        }
        log.debug("[⚡] Saved chat {} in cache and updated indexes || saveChatAndAddMembers", newChat.getId());

        // добавляем участников
        getOrCreateChatMembersContainer(chatId).addBatch(members);
        log.debug("[⚡] Batch saved {} chat members in chat {} || saveChatAndAddMembers", members.size(), chatId);
    }
    public void invalidateChat(long chatId) {
        chatInfoCache.invalidate(chatId);
        log.debug("[⚡] Invalidated chat {} in cache || invalidateChat", chatId);
    }


    // Вспомогательные методы
    public Optional<CacheChat> getPersonalChat(long userId1, long userId2) {
        String key = getPersonalChatKey(userId1, userId2);
        Long chatId = personalChatIndex.getIfPresent(key);
        if (chatId == null) return Optional.empty();

        Optional<CacheChat> chat = getChat(chatId);
        if (chat.isEmpty()) personalChatIndex.invalidate(key);
        return chat;
    }
    public Optional<CacheChat> getChat(long chatId) {
        return Optional.ofNullable(CacheChat.copy(chatInfoCache.getIfPresent(chatId)));
    }
    private Optional<CacheChat> getChatLink(long chatId) {
        return Optional.ofNullable(chatInfoCache.getIfPresent(chatId));
    }


    // Методы для сохранения индекса личного чата TODO: НАДО ПОДУМАТЬ ЧТО СДЕЛАТЬ
    private String getPersonalChatKey(long userId1, long userId2) {
        return Math.min(userId1, userId2) + ":" + Math.max(userId1, userId2);
    }
    public void savePersonalChatIndex(long chatId, long creatorId, long opponentId) {
        personalChatIndex.put(getPersonalChatKey(creatorId, opponentId), chatId);
    }



    // ========== CHAT MEMBER METHODS ========== TODO: СЧЕТЧИК НЕ ОБНОВЛЯЕТСЯ В ЧАТЕ


    // Основные методы
    private CacheChatMembersContainer getOrCreateChatMembersContainer(long chatId) {
        return chatMembersCache.get(chatId, key -> new CacheChatMembersContainer(chatId));
    }
    private Optional<CacheChatMembersContainer> getChatMembersContainer(long chatId) {
        return Optional.ofNullable(chatMembersCache.getIfPresent(chatId));
    }

    public void saveChatMembers(long chatId, Collection<CacheChatMember> members) {
        // Обновляем контейнер
        getOrCreateChatMembersContainer(chatId).addBatch(members);
        getChatLink(chatId).ifPresent(chat -> chat.onAddMembers(members.size()));
        log.debug("[⚡] Batch saved {} chat members in chat {} || saveChatMember", members.size(), chatId);
    }
    public void saveChatMember(CacheChatMember chatMember) {
        long chatId = chatMember.getChatId();
        long userId = chatMember.getUserId();

        // Обновляем контейнер
        getOrCreateChatMembersContainer(chatId).add(chatMember);
        getChatLink(chatId).ifPresent(CacheChat::onAddMember);
        log.debug("[⚡] Saved chat member {} in chat {} || saveChatMember", userId, chatId);
    }
    public void invalidateChatMember(long chatId, long userId) {
        getChatMembersContainer(chatId).ifPresent(c -> {
            c.invalidateMember(userId);
            log.debug("[⚡] Invalidated member {} in chat {} in cache || invalidateChatMember", userId, chatId);
        });
    }
    public void invalidateChatMembersContainer(long chatId) {
        chatMembersCache.invalidate(chatId);
        log.debug("[⚡] Invalidated chat members container {} in cache || invalidateChatMembersContainer", chatId);
    }


    // Вспомогательные методы
    public Map<Long, CacheChatMember> getChatMembers(long chatId, Collection<Long> userIds, Collection<Long> missingIds) {
        Map<Long, CacheChatMember> result = new HashMap<>(userIds.size());

        Optional<CacheChatMembersContainer> containerOpt = getChatMembersContainer(chatId);
        if (containerOpt.isPresent()) {
            CacheChatMembersContainer container = containerOpt.get();
            for (Long userId : userIds) {
                Optional<CacheChatMember> member = container.getMember(userId);
                if (member.isPresent()) {
                    result.put(userId, member.get());
                } else if (missingIds != null) {
                    missingIds.add(userId);
                }
            }
        } else if (missingIds != null) {
            missingIds.addAll(userIds);
        }

        return result;
    }
    public Optional<CacheChatMember> getChatMember(long chatId, long userId) {
        return getChatMembersContainer(chatId).flatMap(c -> c.getMember(userId));
    }
    public Optional<Boolean> hasActiveChatMember(long chatId, long userId) {
        return getChatMembersContainer(chatId).flatMap(c -> c.hasMemberAndIsActive(userId));
    }



    // Вспомогательные методы
    public Optional<Boolean> isActiveAdminInActiveChat(long chatId, long userId) {
        return getChatMembersContainer(chatId).flatMap(c -> c.isAdmin(userId));
    }
    public Optional<List<CacheChatMember>> getChatAdmins(long chatId) {
        return getChatMembersContainer(chatId).map(CacheChatMembersContainer::getChatAdmins);
    }


    // ========== VERIFICATION TOKEN METHODS ==========


    // Основные методы
    public void saveVerificationToken(CacheVerificationToken cache) {
        verificationTokenCache.put(cache.getToken(), CacheVerificationToken.copy(cache));
        log.debug("[⚡] Saved verification token for user {} (token={}) || saveVerificationToken", cache.getUserId(), cache.getToken());
    }
    public void invalidateVerificationToken(String token) {
        verificationTokenCache.invalidate(token);
        log.debug("[⚡] Deleted verification token {} || deleteVerificationToken", token);
    }

    // Вспомогательные методы
    public Optional<CacheVerificationToken> getVerificationToken(String token) {
        return Optional.ofNullable(CacheVerificationToken.copy(verificationTokenCache.getIfPresent(token)));
    }



    // ========== MESSAGES METHODS ==============


    // Основные методы
    public void saveMessage(CacheMessageSecurity message) {
        CacheMessageSecurity copy = CacheMessageSecurity.copy(message);
        messageCache.put(copy.getId(), copy);
        log.debug("[⚡] Saved message {} in cache (chat={}, sender={}) || saveMessage", copy.getId(), copy.getChatId(), copy.getSenderId());
    }
    public void saveMessages(List<CacheMessageSecurity> messages) {
        for (CacheMessageSecurity message : messages) {
            messageCache.put(message.getId(), CacheMessageSecurity.copy(message));
        }
        log.debug("[⚡] Batch saved {} messages to cache || saveMessages", messages.size());
    }
    public void invalidateMessage(long messageId) {
        messageCache.invalidate(messageId);
        log.debug("[⚡] Invalidated message {} in cache || invalidateMessage", messageId);
    }


    // Вспомогательные методы
    public Optional<CacheMessageSecurity> getMessage(long messageId) {
        return Optional.ofNullable(CacheMessageSecurity.copy(messageCache.getIfPresent(messageId)));
    }


    // ========== CACHE STATISTICS AND MANAGEMENT ==========


    // Основные методы
    @Data
    @AllArgsConstructor
    public static final class CacheStats {
        final long activatedUserCount;
        final int allUserCount;
        final long notDeletedChatCount;
        final int chatCount;
        final int chatMembersCount;
        final int adminRightsCount;
        final int verificationTokenCount;
        final int deletedMembersCount;
    }

    public CacheStats getCacheStatus() {
        Map<Long, CacheUserSecurity> userCacheSnapshot = userSecurityCache.asMap();
        Map<Long, CacheChat> chatInfoCacheSnapshot = chatInfoCache.asMap();
        Map<Long, CacheChatMembersContainer> containersSnapshot = chatMembersCache.asMap();

        long activatedUserCount = userCacheSnapshot.values().stream()
                .filter(user -> !user.isDeleted() && user.isEnabled())
                .count();

        int totalChatMembers = containersSnapshot.values().stream()
                .mapToInt(container -> container.getMembers().size())
                .sum();

        int totalAdminRights = containersSnapshot.values().stream()
                .mapToInt(container -> container.getAdminIds().size())
                .sum();

        int totalDeletedMembers = containersSnapshot.values().stream()
                .mapToInt(container -> container.getDeletedMemberIds().size())
                .sum();

        return new CacheStats(
            activatedUserCount,
            userCacheSnapshot.size(),
            chatInfoCacheSnapshot.values().stream().filter(chat -> !chat.isDeleted()).count(),
            chatInfoCacheSnapshot.size(),
            totalChatMembers,
            totalAdminRights,
            (int)verificationTokenCache.estimatedSize(),
            totalDeletedMembers
        );
    }
    public void printCacheStats() {
        CacheService.CacheStats stats = getCacheStatus();
        log.info("📊 Cache Statistics:");
        log.info("   ├─ Active Users: {}", stats.getActivatedUserCount());
        log.info("   ├─ Users: {}", stats.getAllUserCount());
        log.info("   ├─ Active Chats: {}", stats.getChatCount());
        log.info("   ├─ Verification Tokens: {}", stats.getVerificationTokenCount());
        log.info("   ├─ Chat Members: {}", stats.getChatMembersCount());
        log.info("   └─ Admin Rights: {}", stats.getAdminRightsCount());
    }

    public Map<String, Object> getDetailedCacheStatus() {
        Map<String, Object> stats = new HashMap<>();

        // статистика кеша пользователей
        var userStats = userSecurityCache.stats();
        stats.put("userCache.estimatedSize", userSecurityCache.estimatedSize());
        stats.put("userCache.hitRate", userStats.hitRate());
        stats.put("userCache.missRate", userStats.missRate());
        stats.put("userCache.evictionCount", userStats.evictionCount());

        // статистика кеша чатов
        var chatStats = chatInfoCache.stats();
        stats.put("chatCache.estimatedSize", chatInfoCache.estimatedSize());
        stats.put("chatCache.hitRate", chatStats.hitRate());
        stats.put("chatCache.missRate", chatStats.missRate());
        stats.put("chatCache.evictionCount", chatStats.evictionCount());

        var containerStats = chatMembersCache.stats();
        stats.put("chatMembersContainerCache.estimatedSize", chatMembersCache.estimatedSize());
        stats.put("chatMembersContainerCache.hitRate", containerStats.hitRate());
        stats.put("chatMembersContainerCache.missRate", containerStats.missRate());
        stats.put("chatMembersContainerCache.evictionCount", containerStats.evictionCount());

        var tokenStats = verificationTokenCache.stats();
        stats.put("tokenCache.estimatedSize", verificationTokenCache.estimatedSize());
        stats.put("tokenCache.hitRate", tokenStats.hitRate());
        stats.put("tokenCache.missRate", tokenStats.missRate());
        stats.put("tokenCache.evictionCount", tokenStats.evictionCount());

        // статистика кеша индексов
        stats.put("usernameIndex.size", usernameIndex.estimatedSize());
        stats.put("emailIndex.size", emailIndex.estimatedSize());
        stats.put("personalChatIndex.size", personalChatIndex.estimatedSize());

        return stats;
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 10_000) // 1000 * 60 * 60
    public void logDetailedCacheStatus() {

        var cacheStats = getDetailedCacheStatus();

        log.info("---------------------------");

        printCacheStats(); // Выводим основную статистику

        log.info("📊 Cache Statistics Report");
        log.info("   ├─ User Cache: size={}, hitRate={}, missRate={}, evictions={}",
                cacheStats.get("userCache.estimatedSize"),
                (Double)cacheStats.get("userCache.hitRate") * 100,
                (Double)cacheStats.get("userCache.missRate") * 100,
                cacheStats.get("userCache.evictionCount"));

        log.info("   ├─ Chat Cache: size={}, hitRate={}, missRate={}, evictions={}",
                cacheStats.get("chatCache.estimatedSize"),
                (Double)cacheStats.get("chatCache.hitRate") * 100,
                (Double)cacheStats.get("chatCache.missRate") * 100,
                cacheStats.get("chatCache.evictionCount"));

        log.info("   ├─ Chat Member Cache: size={}, hitRate={}%, missRate={}%, evictions={}",
                cacheStats.get("chatMembersContainerCache.estimatedSize"),
                Math.round((Double)cacheStats.get("chatMembersContainerCache.hitRate") * 100),
                Math.round((Double)cacheStats.get("chatMembersContainerCache.missRate") * 100),
                cacheStats.get("chatMembersContainerCache.evictionCount"));

        log.info("   ├─ Token Cache: size={}, hitRate={}, missRate={}, evictions={}",
                cacheStats.get("tokenCache.estimatedSize"),
                (Double)cacheStats.get("tokenCache.hitRate") * 100,
                (Double)cacheStats.get("tokenCache.missRate") * 100,
                cacheStats.get("tokenCache.evictionCount"));

        log.info("   ├─ Indexes: username={}, email={}, personalChats={}",
                cacheStats.get("usernameIndex.size"),
                cacheStats.get("emailIndex.size"),
                cacheStats.get("personalChatIndex.size"));

        log.info("---------------------------");
    }
}