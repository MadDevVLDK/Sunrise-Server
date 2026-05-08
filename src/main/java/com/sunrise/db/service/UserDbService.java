package com.sunrise.db.service;

import com.sunrise.db.entity.User;
import com.sunrise.db.repository.UserRepository;
import com.sunrise.db.result.UserProfileResult;
import com.sunrise.db.result.UserSecurityResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDbService {

    private final UserRepository userRepository;

    @Transactional
    public void saveUser(User user) {
        log.debug("[🗄️] 👤 Saving user: id={}, username={}", user.getId(), user.getUsername());
        userRepository.save(user);
    }

    @Transactional
    public void updateLastLogin(String username, Instant lastLogin) {
        log.debug("[🗄️] 🔐 Updating last login for user: username={}, lastLogin={}", username, lastLogin);
        userRepository.updateLastLogin(username, lastLogin);
    }

    @Transactional
    public int updateUserProfile(long userId, String username, String name, Instant updatedAt) {
        log.debug("[🗄️] 📝 Updating user profile: id={}, username={}, name={}", userId, username, name);
        int result = userRepository.updateProfile(userId, username, name, updatedAt);
        log.debug("[🗄️] 📝 User profile updated: affectedRows={}", result);
        return result;
    }

    @Transactional
    public int updateUserEmail(long userId, String email, Instant updatedAt) {
        log.debug("[🗄️] 📧 Updating user email: id={}, email={}", userId, email);
        int result = userRepository.updateUserEmail(userId, email, updatedAt);
        log.debug("[🗄️] 📧 User email updated: affectedRows={}", result);
        return result;
    }

    @Transactional
    public int updateUserPassword(long userId, String password, Instant updatedAt) {
        log.debug("[🗄️] 🔑 Updating user password: id={}", userId);
        int result = userRepository.updateUserPassword(userId, password, updatedAt);
        log.debug("[🗄️] 🔑 User password updated: affectedRows={}", result);
        return result;
    }

    @Transactional
    public int enableUser(long userId, Instant updatedAt) {
        log.debug("[🗄️] ✅ Enabling user: id={}", userId);
        int result = userRepository.enableUser(userId, updatedAt);
        log.debug("[🗄️] ✅ User enabled: affectedRows={}", result);
        return result;
    }

    @Transactional
    public int disableUser(long userId, Instant updatedAt) {
        log.debug("[🗄️] ❌ Disabling user: id={}", userId);
        int result = userRepository.disableUser(userId, updatedAt);
        log.debug("[🗄️] ❌ User disabled: affectedRows={}", result);
        return result;
    }

    @Transactional
    public int deleteUser(long userId, Instant updatedAt) {
        log.debug("[🗄️] 🗑️ Deleting user: id={}", userId);
        int result = userRepository.deleteUser(userId, updatedAt);
        log.debug("[🗄️] 🗑️ User deleted: affectedRows={}", result);
        return result;
    }

    @Transactional
    public int restoreUser(long userId, Instant updatedAt) {
        log.debug("[🗄️] 🔄 Restoring user: id={}", userId);
        int result = userRepository.restoreUser(userId, updatedAt);
        log.debug("[🗄️] 🔄 User restored: affectedRows={}", result);
        return result;
    }

    @Transactional(readOnly = true)
    public Optional<UserSecurityResult> getUserSecurity(long userId) {
        log.debug("[🗄️] 🔍 Getting user security: id={}", userId);
        return userRepository.getUserSecurity(userId);
    }

    @Transactional(readOnly = true)
    public Optional<UserSecurityResult> getUserSecurityByUsername(String username) {
        log.debug("[🗄️] 🔍 Getting user security by username: {}", username);
        return userRepository.getUserSecurityByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<UserSecurityResult> getUserSecurityByEmail(String email) {
        log.debug("[🗄️] 🔍 Getting user security by email: {}", email);
        return userRepository.getUserSecurityByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<UserProfileResult> getUserProfile(long userId) {
        log.debug("[🗄️] 🔍 Getting user profile: id={}", userId);
        return userRepository.getUserProfile(userId);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResult> getUserProfilesByIds(List<Long> userIds) {
        return userRepository.getUserProfileByIds(userIds);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResult> getActiveUserProfileByIds(List<Long> missingIds) {
        log.debug("[🗄️] 🔍 Getting active user profiles by {} ids", missingIds.size());
        return userRepository.getActiveUserProfileByIds(missingIds);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResult> getActiveUsersPage(String filter, Long cursor, int limit) {
        log.debug("[🗄️] 📄 Getting active users page: filter='{}', cursor={}, limit={}", filter, cursor, limit);
        return userRepository.getActiveUsersPage(filter, cursor, Pageable.ofSize(limit));
    }
}