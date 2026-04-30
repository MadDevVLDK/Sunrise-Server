package com.sunrise.cache.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sunrise.cache.entity.Cache.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.NavigableSet;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class CacheConfig {

    // ==================== USER CACHES ====================

    @Value("${app.cache.max-size.user:100000}")
    private int userCacheMaxSize;

    @Value("${app.cache.ttl.user:60}")
    private int userCacheTtlMinutes;

    @Value("${app.cache.max-size.username-index:150000}")
    private int usernameIndexMaxSize;

    @Value("${app.cache.ttl.user-username-index:60}")
    private int usernameIndexTtlMinutes;

    @Value("${app.cache.max-size.email-index:150000}")
    private int emailIndexMaxSize;

    @Value("${app.cache.ttl.user-email-index:60}")
    private int emailIndexTtlMinutes;

    // ==================== CHAT CACHES ====================

    @Value("${app.cache.max-size.chat:200000}")
    private int chatCacheMaxSize;

    @Value("${app.cache.ttl.chat:720}")
    private int chatCacheTtlMinutes;

    @Value("${app.cache.max-size.personal-chat-index:100000}")
    private int personalChatIndexMaxSize;

    @Value("${app.cache.ttl.personal-chat-index:720}")
    private int personalChatIndexTtlMinutes;

    @Value("${app.cache.max-size.chat-members:150000}")
    private int chatMembersMaxSize;

    @Value("${app.cache.ttl.chat-members:240}")
    private int chatMembersTtlMinutes;

    // ==================== CHAT MEMBER CACHES ====================

    @Value("${app.cache.max-size.recent-chat-members-ids:50000}")
    private int recentChatMembersIdsMaxSize;

    @Value("${app.cache.ttl.recent-chat-members-ids:20}")
    private int recentChatMembersIdsTtlMinutes;

    // ==================== MESSAGES CACHES ====================

    @Value("${app.cache.max-size.recent-messages-ids:25000}")
    private int recentMessagesIdsMaxSize;

    @Value("${app.cache.ttl.recent-messages-ids:20}")
    private int recentMessagesIdsTtlMinutes;

    // ==================== VERIFICATION TOKEN CACHE ====================

    @Value("${app.cache.max-size.verification-tokens:100000}")
    private int verificationTokenCacheMaxSize;

    @Value("${app.cache.ttl.verification-tokens:60}")
    private int verificationTokenCacheTtlMinutes;


    // ==================== USER SECURITY CACHE ====================

    @Bean
    public Cache<Long, UserSecurity> userSecurityCache() {
        log.info("[⚙️] Initializing User Security Cache (max-size: {}, ttl: {} min)", userCacheMaxSize, userCacheTtlMinutes);
        return Caffeine.newBuilder()
                .maximumSize(userCacheMaxSize)
                .expireAfterAccess(userCacheTtlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Bean
    public Cache<Long, UserProfile> userProfileCache() {
        log.info("[⚙️] Initializing User Profile Cache (max-size: {}, ttl: {} min)", userCacheMaxSize, userCacheTtlMinutes);
        return Caffeine.newBuilder()
                .maximumSize(userCacheMaxSize)
                .expireAfterAccess(userCacheTtlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    // ==================== USER INDEX CACHES ====================

    @Bean
    public Cache<String, Long> usernameIndex() {
        log.info("[⚙️] Initializing Username Index (max-size: {}, ttl: {} min)", usernameIndexMaxSize, usernameIndexTtlMinutes);
        return Caffeine.newBuilder()
                .maximumSize(usernameIndexMaxSize)
                .expireAfterAccess(usernameIndexTtlMinutes, TimeUnit.MINUTES)
                .softValues()
                .recordStats()
                .build();
    }

    @Bean
    public Cache<String, Long> emailIndex() {
        log.info("[⚙️] Initializing Email Index (max-size: {}, ttl: {} min)", emailIndexMaxSize, emailIndexTtlMinutes);
        return Caffeine.newBuilder()
                .maximumSize(emailIndexMaxSize)
                .expireAfterAccess(emailIndexTtlMinutes, TimeUnit.MINUTES)
                .softValues()
                .recordStats()
                .build();
    }

    // ==================== CHAT CACHES ====================

    @Bean
    public Cache<Long, Chat> chatInfoCache() {
        log.info("[⚙️] Initializing Chat Info Cache (max-size: {}, ttl: {} min)", chatCacheMaxSize, chatCacheTtlMinutes);
        return Caffeine.newBuilder()
                .maximumSize(chatCacheMaxSize)
                .expireAfterAccess(chatCacheTtlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Bean
    public Cache<String, Long> personalChatIndex() {
        log.info("[⚙️] Initializing Personal Chat Index (max-size: {}, ttl: {} min)", personalChatIndexMaxSize, personalChatIndexTtlMinutes);
        return Caffeine.newBuilder()
                .maximumSize(personalChatIndexMaxSize)
                .expireAfterAccess(personalChatIndexTtlMinutes, TimeUnit.MINUTES)
                .softValues()
                .recordStats()
                .build();
    }

    // ==================== CHAT MEMBERS CACHE ====================

    @Bean
    public Cache<String, ChatMember> chatMembersCache() {
        log.info("[⚙️] Initializing Chat Members Cache (max-size: {}, ttl: {} min)", chatMembersMaxSize, chatMembersTtlMinutes);
        return Caffeine.newBuilder()
                .maximumSize(chatMembersMaxSize)
                .expireAfterAccess(chatMembersTtlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Bean
    public Cache<Long, NavigableSet<Long>> recentChatMembersIdsCache() {
        log.info("[⚙️] Initializing Chat Members IDs Cache (max-size: {}, ttl: {} min)", recentChatMembersIdsMaxSize, recentChatMembersIdsTtlMinutes);
        return Caffeine.newBuilder()
                .maximumSize(recentChatMembersIdsMaxSize)
                .expireAfterAccess(recentChatMembersIdsTtlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    // ==================== MESSAGE CACHE ====================

    @Bean
    public Cache<Long, Message> messageCache() {
        log.info("[⚙️] Initializing Message Cache (max-size: {}, ttl: {} min)", chatCacheMaxSize, chatCacheTtlMinutes);
        return Caffeine.newBuilder()
                .maximumSize(chatCacheMaxSize)
                .expireAfterAccess(chatCacheTtlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Bean
    public Cache<Long, NavigableSet<Long>> recentMessagesIdsCache() {
        log.info("[⚙️] Initializing Recent Messages IDs Cache (max-size: {}, ttl: {} min)", recentMessagesIdsMaxSize, recentMessagesIdsTtlMinutes);
        return Caffeine.newBuilder()
                .maximumSize(recentMessagesIdsMaxSize)
                .expireAfterAccess(recentMessagesIdsTtlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    // ==================== VERIFICATION TOKEN CACHE ====================

    @Bean
    public Cache<String, VerificationToken> verificationTokenCache() {
        log.info("[⚙️] Initializing Verification Token Cache (max-size: {}, ttl: {} min)", verificationTokenCacheMaxSize, verificationTokenCacheTtlMinutes);
        return Caffeine.newBuilder()
                .maximumSize(verificationTokenCacheMaxSize)
                .expireAfterWrite(verificationTokenCacheTtlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
}