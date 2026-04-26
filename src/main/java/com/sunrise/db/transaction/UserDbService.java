package com.sunrise.db.transaction;

import com.sunrise.db.DBService;
import com.sunrise.db.entity.User;
import com.sunrise.db.result.UserProfileResult;
import com.sunrise.db.result.UserSecurityResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserDbService {

    private final DBService dbService;

    @Transactional
    public void saveUser(User user) {
        dbService.saveUser(user);
    }

    @Transactional
    public void updateLastLogin(String username, LocalDateTime lastLogin) {
        dbService.updateLastLogin(username, lastLogin);
    }

    @Transactional
    public int updateUserProfile(long userId, String username, String name, LocalDateTime updatedAt) {
        return dbService.updateUserProfile(userId, username, name, updatedAt);
    }

    @Transactional
    public int updateUserEmail(long userId, String email, LocalDateTime updatedAt) {
        return dbService.updateUserEmail(userId, email, updatedAt);
    }

    @Transactional
    public int updateUserPassword(long userId, String password, LocalDateTime updatedAt) {
        return dbService.updateUserPassword(userId, password, updatedAt);
    }

    @Transactional
    public int enableUser(long userId, LocalDateTime updatedAt) {
        return dbService.enableUser(userId, updatedAt);
    }

    @Transactional
    public int disableUser(long userId, LocalDateTime updatedAt) {
        return dbService.disableUser(userId, updatedAt);
    }

    @Transactional
    public int deleteUser(long userId, LocalDateTime updatedAt) {
        return dbService.deleteUser(userId, updatedAt);
    }

    @Transactional
    public int restoreUser(long userId, LocalDateTime updatedAt) {
        return dbService.restoreUser(userId, updatedAt);
    }



    
    @Transactional(readOnly = true)
    public Optional<UserSecurityResult> getUserSecurity(long userId) {
        return dbService.getUserSecurity(userId);
    }

    @Transactional(readOnly = true)
    public Optional<UserSecurityResult> getUserSecurityByUsername(String username) {
        return dbService.getUserSecurityByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<UserSecurityResult> getUserSecurityByEmail(String email) {
        return dbService.getUserSecurityByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<UserProfileResult> getUserProfile(long userId) {
        return dbService.getUserProfile(userId);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResult> getActiveUserProfileByIds(List<Long> missingIds) {
        return dbService.getActiveUserProfileByIds(missingIds);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResult> getActiveUsersPage(String filter, Long cursor, int limit) {
        return dbService.getActiveUsersPage(filter, cursor, limit);
    }
}