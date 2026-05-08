package com.sunrise.cache.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.sunrise.cache.entity.Cache.UserProfile;
import com.sunrise.cache.entity.Cache.UserSecurity;
import com.sunrise.helpclass.mapper.UserMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class UserCacheService {

    private final Cache<Long, UserSecurity> userSecurityCache;
    private final Cache<Long, UserProfile> userProfileCache;
    private final Cache<String, Long> usernameIndex;
    private final Cache<String, Long> emailIndex;

    public UserCacheService(Cache<Long, UserSecurity> userSecurityCache,
                            Cache<Long, UserProfile> userProfileCache,
                            @Qualifier("usernameIndex") Cache<String, Long> usernameIndex,
                            @Qualifier("emailIndex") Cache<String, Long> emailIndex) {
        this.userSecurityCache = userSecurityCache;
        this.userProfileCache = userProfileCache;
        this.usernameIndex = usernameIndex;
        this.emailIndex = emailIndex;
    }


    // ========== USER METHODS ==========

    public void saveProfiles(Collection<UserProfile> users) {
        for (UserProfile user : users) {
            userProfileCache.put(user.id(), UserMapper.copy(user));
            usernameIndex.put(user.username(), user.id());
        }
        log.debug("[⚡] 👥 Batch saved {} users to cache and updated indexes", users.size());
    }

    public void saveSecurity(UserSecurity user) {
        userSecurityCache.put(user.id(), UserMapper.copy(user));
        emailIndex.put(user.email(), user.id());
        log.debug("[⚡] 👤🔐 Saved user security {} in cache and updated indexes", user.id());
    }

    public void saveSecurityAndUsernameIndex(UserSecurity user, String username) {
        userSecurityCache.put(user.id(), UserMapper.copy(user));
        usernameIndex.put(username, user.id());
        emailIndex.put(user.email(), user.id());
        log.debug("[⚡] 👤🔐 Saved user security {} (username={}) in cache and updated indexes", user.id(), username);
    }

    public void saveProfile(UserProfile user) {
        userProfileCache.put(user.id(), UserMapper.copy(user));
        usernameIndex.put(user.username(), user.id());
        log.debug("[⚡] 👤 Saved user profile {} in cache and updated indexes", user.id());
    }

    public void invalidateSecurity(long userId) {
        userSecurityCache.invalidate(userId);
        log.debug("[⚡] 👤🚫 Invalidated user security {} in cache || invalidateUserSecurity", userId);
    }

    public void invalidateSecurityAndEmailIndex(long userId, String email) {
        userSecurityCache.invalidate(userId);
        emailIndex.invalidate(email);
        log.debug("[⚡] 👤🚫 Invalidated user security {} and email index {} in cache || invalidateUserSecurityAndEmailIndex", userId, email);
    }

    public void invalidateProfile(long userId) {
        userProfileCache.invalidate(userId);
        log.debug("[⚡] 👤🚫 Invalidated user profile {} in cache || invalidateUserProfile", userId);
    }

    public void invalidateProfileAndUsernameIndex(long userId, String username) {
        userProfileCache.invalidate(userId);
        usernameIndex.invalidate(username);
        log.debug("[⚡] 👤🚫 Invalidated user profile {} and username index {} in cache || invalidateUserProfileAndUsernameIndex", userId, username);
    }

    public Map<Long, UserProfile> getProfilesByIds(Collection<Long> userIds, Collection<Long> missingIds) {
        Map<Long, UserProfile> result = new HashMap<>(userIds.size());
        for (Long userId : userIds) {
            Optional<UserProfile> user = getProfile(userId);
            if (user.isPresent()) {
                result.put(userId, user.get());
            } else if (missingIds != null) {
                missingIds.add(userId);
            }
        }
        log.debug("[⚡] 👤🔍 Retrieved {} cached users by ids, {} missing", result.size(), missingIds != null ? missingIds.size() : 0);
        return result;
    }

    public Optional<UserSecurity> getSecurityByUsername(String username) {
        String key = username.toLowerCase();
        Long userId = usernameIndex.getIfPresent(key);
        if (userId == null) {
            log.debug("[⚡] 👤🔍 User security not found by username: {}", username);
            return Optional.empty();
        }
        Optional<UserSecurity> user = getSecurity(userId);
        if (user.isEmpty()) {
            usernameIndex.invalidate(key);
            log.debug("[⚡] 👤🚫 Invalidated username index {} (user not in cache)", username);
        }
        return user;
    }

    public Optional<UserSecurity> getSecurityByEmail(String email) {
        String key = email.toLowerCase();
        Long userId = emailIndex.getIfPresent(key);
        if (userId == null) {
            log.debug("[⚡] 👤🔍 User security not found by email: {}", email);
            return Optional.empty();
        }
        Optional<UserSecurity> user = getSecurity(userId);
        if (user.isEmpty()) {
            emailIndex.invalidate(key);
            log.debug("[⚡] 👤🚫 Invalidated email index {} (user not in cache)", email);
        }
        return user;
    }

    public Optional<UserSecurity> getSecurity(long userId) {
        return Optional.ofNullable(UserMapper.copy(userSecurityCache.getIfPresent(userId)));
    }

    public Optional<UserProfile> getProfile(long userId) {
        return Optional.ofNullable(UserMapper.copy(userProfileCache.getIfPresent(userId)));
    }

    public boolean existsByUsername(String username) {
        return usernameIndex.getIfPresent(username.toLowerCase()) != null;
    }

    public boolean existsByEmail(String email) {
        return emailIndex.getIfPresent(email.toLowerCase()) != null;
    }
}