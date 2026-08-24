package com.sunrise.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sunrise.cache.entity.Cache.UserProfile;
import com.sunrise.cache.entity.Cache.UserSecurity;
import com.sunrise.cache.service.UserCacheService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserCacheServiceTest {

    private Cache<Long, UserSecurity> userSecurityCache;
    private Cache<Long, UserProfile> userProfileCache;
    private Cache<String, Long> usernameIndex;
    private Cache<String, Long> emailIndex;
    private UserCacheService userCacheService;

    @BeforeEach
    void setUp() {
        userSecurityCache = Caffeine.newBuilder().build();
        userProfileCache = Caffeine.newBuilder().build();
        usernameIndex = Caffeine.newBuilder().build();
        emailIndex = Caffeine.newBuilder().build();
        userCacheService = new UserCacheService(userSecurityCache, userProfileCache, usernameIndex, emailIndex);
    }

    // ========== SAVE METHODS ==========

    @Test
    void saveProfile_shouldStoreProfileAndUsernameIndex() {
        UserProfile profile = new UserProfile(1L, "john_doe", "John", Instant.now(), Instant.now(), null, false);
        userCacheService.saveProfile(profile);

        assertThat(userProfileCache.getIfPresent(1L)).isEqualTo(profile);
        assertThat(usernameIndex.getIfPresent("john_doe")).isEqualTo(1L);
    }

    @Test
    void saveSecurity_shouldStoreSecurityAndEmailIndex() {
        UserSecurity security = new UserSecurity(1L, "john@example.com", "hashed", 1, true, null, false);
        userCacheService.saveSecurity(security);

        assertThat(userSecurityCache.getIfPresent(1L)).isEqualTo(security);
        assertThat(emailIndex.getIfPresent("john@example.com")).isEqualTo(1L);
    }

    @Test
    void saveSecurityAndUsernameIndex_shouldStoreAll() {
        UserSecurity security = new UserSecurity(1L, "john@ex.com", "hash", 1, true, null, false);
        userCacheService.saveSecurityAndUsernameIndex(security, "johnny");

        assertThat(userSecurityCache.getIfPresent(1L)).isEqualTo(security);
        assertThat(usernameIndex.getIfPresent("johnny")).isEqualTo(1L);
        assertThat(emailIndex.getIfPresent("john@ex.com")).isEqualTo(1L);
    }

    @Test
    void saveProfiles_shouldStoreMultipleProfilesAndIndexes() {
        UserProfile p1 = new UserProfile(1L, "u1", "U1", Instant.now(), Instant.now(), null, false);
        UserProfile p2 = new UserProfile(2L, "u2", "U2", Instant.now(), Instant.now(), null, false);
        userCacheService.saveProfiles(List.of(p1, p2));

        assertThat(userProfileCache.getIfPresent(1L)).isEqualTo(p1);
        assertThat(userProfileCache.getIfPresent(2L)).isEqualTo(p2);
        assertThat(usernameIndex.getIfPresent("u1")).isEqualTo(1L);
        assertThat(usernameIndex.getIfPresent("u2")).isEqualTo(2L);
    }

    // ========== GET METHODS ==========

    @Test
    void getProfile_whenExists_shouldReturn() {
        UserProfile profile = new UserProfile(1L, "john", "John", Instant.now(), Instant.now(), null, false);
        userProfileCache.put(1L, profile);

        Optional<UserProfile> result = userCacheService.getProfile(1L);

        assertThat(result).contains(profile);
    }

    @Test
    void getProfile_whenNotExists_shouldReturnEmpty() {
        Optional<UserProfile> result = userCacheService.getProfile(99L);
        assertThat(result).isEmpty();
    }

    @Test
    void getSecurity_whenExists_shouldReturn() {
        UserSecurity security = new UserSecurity(1L, "e@ma.il", "pwd", 2, true, null, false);
        userSecurityCache.put(1L, security);

        Optional<UserSecurity> result = userCacheService.getSecurity(1L);

        assertThat(result).contains(security);
    }

    @Test
    void getSecurityByUsername_whenExists_shouldReturn() {
        usernameIndex.put("john", 1L);
        UserSecurity security = new UserSecurity(1L, "e@ma.il", "pwd", 2, true, null, false);
        userSecurityCache.put(1L, security);

        Optional<UserSecurity> result = userCacheService.getSecurityByUsername("john");

        assertThat(result).contains(security);
    }

    @Test
    void getSecurityByUsername_whenIndexMissing_shouldReturnEmpty() {
        Optional<UserSecurity> result = userCacheService.getSecurityByUsername("unknown");
        assertThat(result).isEmpty();
    }

    @Test
    void getSecurityByUsername_whenIndexPresentButUserMissing_shouldInvalidateIndex() {
        usernameIndex.put("stale", 1L);
        // userSecurityCache пуст

        Optional<UserSecurity> result = userCacheService.getSecurityByUsername("stale");

        assertThat(result).isEmpty();
        assertThat(usernameIndex.getIfPresent("stale")).isNull();
    }

    @Test
    void getSecurityByEmail_whenExists_shouldReturn() {
        emailIndex.put("user@ex.com", 1L);
        UserSecurity security = new UserSecurity(1L, "user@ex.com", "pwd", 1, true, null, false);
        userSecurityCache.put(1L, security);

        Optional<UserSecurity> result = userCacheService.getSecurityByEmail("user@ex.com");

        assertThat(result).contains(security);
    }

    @Test
    void getSecurityByEmail_whenIndexPresentButUserMissing_shouldInvalidateIndex() {
        emailIndex.put("stale@ex.com", 1L);
        Optional<UserSecurity> result = userCacheService.getSecurityByEmail("stale@ex.com");
        assertThat(result).isEmpty();
        assertThat(emailIndex.getIfPresent("stale@ex.com")).isNull();
    }

    @Test
    void getProfilesByIds_shouldReturnPresentAndCollectMissing() {
        UserProfile p1 = new UserProfile(1L, "a", "A", Instant.now(), Instant.now(), null, false);
        UserProfile p2 = new UserProfile(2L, "b", "B", Instant.now(), Instant.now(), null, false);
        userProfileCache.put(1L, p1);
        // 2 missing

        Set<Long> missingIds = Set.of(2L);
        Map<Long, UserProfile> result = userCacheService.getProfilesByIds(Set.of(1L, 2L), missingIds);

        assertThat(result).containsEntry(1L, p1).doesNotContainKey(2L);
        assertThat(missingIds).containsExactly(2L);
    }

    // ========== EXISTS METHODS ==========

    @Test
    void existsByUsername_whenIndexPresent_shouldReturnTrue() {
        usernameIndex.put("john", 1L);
        assertThat(userCacheService.existsByUsername("john")).isTrue();
    }

    @Test
    void existsByUsername_whenIndexMissing_shouldReturnFalse() {
        assertThat(userCacheService.existsByUsername("unknown")).isFalse();
    }

    @Test
    void existsByEmail_whenIndexPresent_shouldReturnTrue() {
        emailIndex.put("a@b.com", 1L);
        assertThat(userCacheService.existsByEmail("a@b.com")).isTrue();
    }

    // ========== INVALIDATE METHODS ==========

    @Test
    void invalidateSecurity_shouldRemoveFromCache() {
        userSecurityCache.put(1L, mockSecurity());
        userCacheService.invalidateSecurity(1L);
        assertThat(userSecurityCache.getIfPresent(1L)).isNull();
    }

    @Test
    void invalidateSecurityAndEmailIndex_shouldRemoveBoth() {
        userSecurityCache.put(1L, mockSecurity());
        emailIndex.put("john@ex.com", 1L);
        userCacheService.invalidateSecurityAndEmailIndex(1L, "john@ex.com");
        assertThat(userSecurityCache.getIfPresent(1L)).isNull();
        assertThat(emailIndex.getIfPresent("john@ex.com")).isNull();
    }

    @Test
    void invalidateProfile_shouldRemoveProfile() {
        userProfileCache.put(1L, mockProfile());
        userCacheService.invalidateProfile(1L);
        assertThat(userProfileCache.getIfPresent(1L)).isNull();
    }

    @Test
    void invalidateProfileAndUsernameIndex_shouldRemoveBoth() {
        userProfileCache.put(1L, mockProfile());
        usernameIndex.put("john", 1L);
        userCacheService.invalidateProfileAndUsernameIndex(1L, "john");
        assertThat(userProfileCache.getIfPresent(1L)).isNull();
        assertThat(usernameIndex.getIfPresent("john")).isNull();
    }

    private UserSecurity mockSecurity() {
        return new UserSecurity(1L, "john@ex.com", "hash", 1, true, null, false);
    }

    private UserProfile mockProfile() {
        return new UserProfile(1L, "john", "John", Instant.now(), Instant.now(), null, false);
    }
}