package com.sunrise.cache.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sunrise.cache.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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


    // ==================== VERIFICATION TOKEN CACHE ====================

    @Value("${app.cache.max-size.verification-tokens:100000}")
    private int verificationTokenCacheMaxSize;

    @Value("${app.cache.ttl.verification-tokens:60}")
    private int verificationTokenCacheTtlMinutes;


    // ==================== PAGINATION CACHES ====================

    @Value("${app.cache.max-size.user_chats_pagination:25000}")
    private int userChatsPaginationMaxSize;

    @Value("${app.cache.ttl.user_chats_pagination:20}")
    private int userChatsPaginationTtlMinutes;

    @Value("${app.cache.max-size.chat_members_pagination:50000}")
    private int chatMembersPaginationMaxSize;

    @Value("${app.cache.ttl.chat_members_pagination:20}")
    private int chatMembersPaginationTtlMinutes;


    // ==================== USER SECURITY CACHE ====================

    @Bean
    public Cache<Long, CacheUserSecurity> userSecurityCache() {
        log.info("[⚙️] Initializing User Security Cache (max-size: {}, ttl: {} min)", userCacheMaxSize, userCacheTtlMinutes);
        return Caffeine.newBuilder()
                .maximumSize(userCacheMaxSize)
                .expireAfterAccess(userCacheTtlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Bean
    public Cache<Long, CacheUserProfile> userProfileCache() {
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
                .softValues()  // При нехватке памяти удаляются
                .recordStats()
                .build();
    }

    @Bean
    public Cache<String, Long> emailIndex() {
        log.info("[⚙️] Initializing Email Index (max-size: {}, ttl: {} min)", emailIndexMaxSize, emailIndexTtlMinutes);
        return Caffeine.newBuilder()
                .maximumSize(emailIndexMaxSize)
                .expireAfterAccess(emailIndexTtlMinutes, TimeUnit.MINUTES)
                .softValues()  // При нехватке памяти удаляются
                .recordStats()
                .build();
    }


    // ==================== CHAT CACHES ====================

    @Bean
    public Cache<Long, CacheChat> chatInfoCache() {
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
                .softValues()  // При нехватке памяти удаляются
                .recordStats()
                .build();
    }


    // ==================== CHAT MEMBERS CACHE ====================

    @Bean
    public Cache<Long, CacheChatMembersContainer> chatMembersCache() {
        log.info("[⚙️] Initializing Chat Members Cache (max-size: {}, ttl: {} min)", chatMembersMaxSize, chatMembersTtlMinutes);
        return Caffeine.newBuilder()
                .maximumSize(chatMembersMaxSize)
                .expireAfterAccess(chatMembersTtlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }


    // ==================== MESSAGE CACHE ====================

    @Bean
    public Cache<Long, CacheMessageSecurity> messageCache() {
        log.info("[⚙️] Initializing Message Cache (max-size: {}, ttl: {} min)", chatCacheMaxSize, chatCacheTtlMinutes);
        return Caffeine.newBuilder()
                .maximumSize(chatCacheMaxSize)
                .expireAfterAccess(chatCacheTtlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }


    // ==================== VERIFICATION TOKEN CACHE ====================

    @Bean
    public Cache<String, CacheVerificationToken> verificationTokenCache() {
        log.info("[⚙️] Initializing Verification Token Cache (max-size: {}, ttl: {} min)", verificationTokenCacheMaxSize, verificationTokenCacheTtlMinutes);
        return Caffeine.newBuilder()
                .maximumSize(verificationTokenCacheMaxSize)
                .expireAfterWrite(verificationTokenCacheTtlMinutes, TimeUnit.MINUTES)  // Write, не Access!
                .recordStats()
                .build();
    }


    // ==================== PAGINATION CACHES ====================

    @Bean
    public Cache<String, Object> userChatsPaginationCache() {
        log.info("[⚙️] Initializing User Chats Pagination Cache (max-size: {}, ttl: {} min)", userChatsPaginationMaxSize, userChatsPaginationTtlMinutes);
        return Caffeine.newBuilder()
                .maximumSize(userChatsPaginationMaxSize)
                .expireAfterAccess(userChatsPaginationTtlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Bean
    public Cache<String, Object> chatMembersPaginationCache() {
        log.info("[⚙️] Initializing Chat Members Pagination Cache (max-size: {}, ttl: {} min)", chatMembersPaginationMaxSize, chatMembersPaginationTtlMinutes);
        return Caffeine.newBuilder()
                .maximumSize(chatMembersPaginationMaxSize)
                .expireAfterAccess(chatMembersPaginationTtlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
}

