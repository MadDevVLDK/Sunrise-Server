package com.sunrise.core.service;

import com.sunrise.helpclass.exception.MyErrorCode;
import com.sunrise.helpclass.exception.MyException;
import com.sunrise.orchestrator.DataValidator;
import com.sunrise.orchestrator.result.Dto.*;
import com.sunrise.orchestrator.service.UserOrchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserOrchestrator userOrchestrator;

    private final DataValidator validator;

    @Transactional
    public void updateProfile(long userId, String newUsername, String newName) {
        validator.validateActiveUser(userId);

        UserProfileLight user = userOrchestrator.getUserProfileLight(userId)
            .orElseThrow(() -> new MyException(
                MyErrorCode.USER_NOT_FOUND_OR_DELETED,
                "User not found or is deleted -> " + userId
            ));

        boolean usernameNotChanged = user.username().equals(newUsername);
        boolean nameNotChanged = user.name().equals(newName);

        if (usernameNotChanged && nameNotChanged) {
            throw new MyException(
                MyErrorCode.VALIDATION_ERROR,
                "Data has not changed for user -> " + userId
            );
        }

        if (!usernameNotChanged && userOrchestrator.existsUserByUsername(newUsername)) {
            throw new MyException(
                MyErrorCode.USERNAME_TAKEN,
                "Username already taken -> " + newUsername
            );
        }

        userOrchestrator.updateUserProfile(userId, user.username(), newUsername, newName, Instant.now());
        log.info("[🔧] ✅ User {} updated profile", userId);
    }

    @Transactional
    public void deleteUser(long userIdToDeleted, long userWhoDelete) {
        if (userIdToDeleted != userWhoDelete) {
            validator.validateActiveUser(userWhoDelete);
        }
        validator.validateActiveUser(userIdToDeleted);

        Instant updatedAt = Instant.now();
        userOrchestrator.deleteUser(userIdToDeleted, updatedAt);

        log.info("[🔧] ✅ User {} deleted profile {}", userWhoDelete, userIdToDeleted);
    }

    @Transactional(readOnly = true)
    public UserProfileLight getMyProfile(long userId) {
        validator.validateActiveUser(userId);

        UserProfileLight profile = userOrchestrator.getUserProfileLight(userId)
            .orElseThrow(() -> new MyException(
                MyErrorCode.USER_NOT_FOUND_OR_DELETED,
                "User not found or is deleted -> " + userId
            ));

        log.debug("[🔧] ✅ Loaded profile for user {}", userId);
        return profile;
    }

    @Transactional(readOnly = true)
    public UserProfileLight getOtherProfileLight(long currentUserId, long otherUserId) {
        validator.validateActiveUser(currentUserId);
        validator.validateActiveUser(otherUserId);

        UserProfileLight profile = userOrchestrator.getUserProfileLight(otherUserId)
            .orElseThrow(() -> new MyException(
                MyErrorCode.USER_NOT_FOUND_OR_DELETED,
                "User not found or is deleted -> " + otherUserId
            ));

        log.debug("[🔧] ✅ User {} retrieved light profile of user {}", currentUserId, otherUserId);
        return profile;
    }

    @Transactional(readOnly = true)
    public UserProfileFull getOtherProfileFull(long currentUserId, long otherUserId) {
        validator.validateActiveUser(currentUserId);
        validator.validateActiveUser(otherUserId);

        UserProfileFull profile = userOrchestrator.getUserProfileFull(otherUserId)
            .orElseThrow(() -> new MyException(
                MyErrorCode.USER_NOT_FOUND_OR_DELETED,
                "User not found or is deleted -> " + otherUserId
            ));

        log.debug("[🔧] ✅ User {} retrieved full profile of user {}", currentUserId, otherUserId);
        return profile;
    }

    @Transactional(readOnly = true)
    public List<UserProfileLight> getOtherProfileLightByIds(long currentUser, Set<Long> userIds) {
        validator.validateActiveUser(currentUser);

        List<UserProfileLight> profiles = userOrchestrator.getUserProfileLightsByIds(userIds);
        log.debug("[🔧] ✅ User {} retrieved profiles of {} users", currentUser, profiles.size());
        return profiles;
    }

    @Transactional(readOnly = true)
    public UsersPage getActiveUsersPage(long userId, String filter, Long cursor, int limit) {
        validator.validateActiveUser(userId);

        UsersPage usersPage = userOrchestrator.getActiveUserProfileLightsPage(filter, cursor, limit);
        log.debug("[🔧] ✅ Got {} users with filter='{}', nextCursor={}, limit={}", usersPage.users().size(), filter, cursor, limit);
        return usersPage;
    }

    @Transactional(readOnly = true)
    public UserGlobalEventSync syncUserEvents(long userId, long cursor) {
        validator.validateActiveUser(userId);

        UserGlobalEventSync sync = userOrchestrator.getSyncUser(userId, cursor);
        log.debug("[🔧] ✅ User {} synced events", userId);
        return sync;
    }
}