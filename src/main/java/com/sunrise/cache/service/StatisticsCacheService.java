package com.sunrise.cache.service;

import com.sunrise.cache.entity.*;
import com.github.benmanes.caffeine.cache.Cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class StatisticsCacheService {

    private final Cache<Long, CacheUserSecurity> userSecurityCache;
    private final Cache<String, Long> usernameIndex;
    private final Cache<String, Long> emailIndex;
    private final Cache<Long, CacheChat> chatInfoCache;
    private final Cache<String, Long> personalChatIndex;
    private final Cache<String, CacheChatMember> chatMembersCache;
    private final Cache<String, CacheVerificationToken> verificationTokenCache;

    public StatisticsCacheService(Cache<Long, CacheUserSecurity> userSecurityCache,
                                  @Qualifier("usernameIndex") Cache<String, Long> usernameIndex,
                                  @Qualifier("emailIndex") Cache<String, Long> emailIndex,
                                  Cache<Long, CacheChat> chatInfoCache,
                                  @Qualifier("personalChatIndex") Cache<String, Long> personalChatIndex,
                                  Cache<String, CacheChatMember> chatMembersCache,
                                  Cache<String, CacheVerificationToken> verificationTokenCache) {
        this.userSecurityCache = userSecurityCache;
        this.usernameIndex = usernameIndex;
        this.emailIndex = emailIndex;
        this.chatInfoCache = chatInfoCache;
        this.personalChatIndex = personalChatIndex;
        this.chatMembersCache = chatMembersCache;
        this.verificationTokenCache = verificationTokenCache;
    }

    @Data
    @AllArgsConstructor
    public static final class CacheStats {
        private final long activatedUserCount;
        private final int allUserCount;
        private final long notDeletedChatCount;
        private final int chatCount;
        private final int chatMembersCount;
        private final int adminRightsCount;
        private final int verificationTokenCount;
        private final int deletedMembersCount;
    }

    public CacheStats getCacheStatus() {
        Map<Long, CacheUserSecurity> userCacheSnapshot = userSecurityCache.asMap();
        Map<Long, CacheChat> chatInfoCacheSnapshot = chatInfoCache.asMap();
        Map<String, CacheChatMember> chatMembersSnapshot = chatMembersCache.asMap();

        long activatedUserCount = userCacheSnapshot.values().stream()
                .filter(user -> user.isEnabled() && !user.isDeleted())
                .count();

        int totalChatMembers = chatMembersSnapshot.size();

        int totalAdminRights = (int) chatMembersSnapshot.values().stream()
                .filter(CacheChatMember::isAdmin)
                .count();

        int totalDeletedMembers = (int) chatMembersSnapshot.values().stream()
                .filter(CacheChatMember::isDeleted)
                .count();

        return new CacheStats(
            activatedUserCount,
            userCacheSnapshot.size(),
            chatInfoCacheSnapshot.values().stream().filter(chat -> !chat.isDeleted()).count(),
            chatInfoCacheSnapshot.size(),
            totalChatMembers,
            totalAdminRights,
            (int) verificationTokenCache.estimatedSize(),
            totalDeletedMembers
        );
    }

    public Map<String, Object> getDetailedCacheStatus() {
        Map<String, Object> stats = new HashMap<>();

        var userStats = userSecurityCache.stats();
        stats.put("userCache.estimatedSize", userSecurityCache.estimatedSize());
        stats.put("userCache.hitRate", userStats.hitRate());
        stats.put("userCache.missRate", userStats.missRate());
        stats.put("userCache.evictionCount", userStats.evictionCount());

        var chatStats = chatInfoCache.stats();
        stats.put("chatCache.estimatedSize", chatInfoCache.estimatedSize());
        stats.put("chatCache.hitRate", chatStats.hitRate());
        stats.put("chatCache.missRate", chatStats.missRate());
        stats.put("chatCache.evictionCount", chatStats.evictionCount());

        var memberStats = chatMembersCache.stats();
        stats.put("chatMembersCache.estimatedSize", chatMembersCache.estimatedSize());
        stats.put("chatMembersCache.hitRate", memberStats.hitRate());
        stats.put("chatMembersCache.missRate", memberStats.missRate());
        stats.put("chatMembersCache.evictionCount", memberStats.evictionCount());

        var tokenStats = verificationTokenCache.stats();
        stats.put("tokenCache.estimatedSize", verificationTokenCache.estimatedSize());
        stats.put("tokenCache.hitRate", tokenStats.hitRate());
        stats.put("tokenCache.missRate", tokenStats.missRate());
        stats.put("tokenCache.evictionCount", tokenStats.evictionCount());

        stats.put("usernameIndex.size", usernameIndex.estimatedSize());
        stats.put("emailIndex.size", emailIndex.estimatedSize());
        stats.put("personalChatIndex.size", personalChatIndex.estimatedSize());

        return stats;
    }

    public void printCacheStats() {
        CacheStats stats = getCacheStatus();
        log.info("📊 Cache Statistics:");
        log.info("   ├─ Active Users: {}", stats.getActivatedUserCount());
        log.info("   ├─ All Users: {}", stats.getAllUserCount());
        log.info("   ├─ Active Chats: {}", stats.getNotDeletedChatCount());
        log.info("   ├─ Total Chats: {}", stats.getChatCount());
        log.info("   ├─ Deleted Members: {}", stats.getDeletedMembersCount());
        log.info("   ├─ Chat Members: {}", stats.getChatMembersCount());
        log.info("   ├─ Admin Rights: {}", stats.getAdminRightsCount());
        log.info("   └─ Verification Tokens: {}", stats.getVerificationTokenCount());
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 10_000)
    public void logDetailedCacheStatus() {
        var cacheStats = getDetailedCacheStatus();
        log.info("---------------------------");
        printCacheStats();
        log.info("📊 Cache Statistics Report");
        log.info("   ├─ 👤 User Cache: size={}, hitRate={}%, missRate={}%, evictions={}",
                cacheStats.get("userCache.estimatedSize"),
                Math.round((Double)cacheStats.get("userCache.hitRate") * 100),
                Math.round((Double)cacheStats.get("userCache.missRate") * 100),
                cacheStats.get("userCache.evictionCount"));
        log.info("   ├─ 💬 Chat Cache: size={}, hitRate={}%, missRate={}%, evictions={}",
                cacheStats.get("chatCache.estimatedSize"),
                Math.round((Double)cacheStats.get("chatCache.hitRate") * 100),
                Math.round((Double)cacheStats.get("chatCache.missRate") * 100),
                cacheStats.get("chatCache.evictionCount"));
        log.info("   ├─ 👥 Chat Member Cache: size={}, hitRate={}%, missRate={}%, evictions={}",
                cacheStats.get("chatMembersCache.estimatedSize"),
                Math.round((Double)cacheStats.get("chatMembersCache.hitRate") * 100),
                Math.round((Double)cacheStats.get("chatMembersCache.missRate") * 100),
                cacheStats.get("chatMembersCache.evictionCount"));
        log.info("   ├─ 🎫 Token Cache: size={}, hitRate={}%, missRate={}%, evictions={}",
                cacheStats.get("tokenCache.estimatedSize"),
                Math.round((Double)cacheStats.get("tokenCache.hitRate") * 100),
                Math.round((Double)cacheStats.get("tokenCache.missRate") * 100),
                cacheStats.get("tokenCache.evictionCount"));
        log.info("   ├─ 📌 Indexes: username={}, email={}, personalChats={}",
                cacheStats.get("usernameIndex.size"),
                cacheStats.get("emailIndex.size"),
                cacheStats.get("personalChatIndex.size"));
        log.info("---------------------------");
    }
}