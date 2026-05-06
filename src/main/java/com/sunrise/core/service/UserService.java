package com.sunrise.core.service;

import com.sunrise.core.result.*;
import com.sunrise.helpclass.ValidationException;
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
    public ResultNoArgs updateProfile(long userId, String newUsername, String newName) {
        try {
            validator.validateActiveUser(userId);

            UserProfileLight user = userOrchestrator.getUserProfileLight(userId)
                    .orElseThrow(() -> new ValidationException("User not found"));

            boolean usernameNotChanged = user.username().equals(newUsername);
            boolean nameNotChanged = user.name().equals(newName);

            // Проверяем, что данные изменились
            if (usernameNotChanged && nameNotChanged) {
                throw new ValidationException("Data has not changed");
            }

            if (!usernameNotChanged && userOrchestrator.existsUserByUsername(newUsername)) {
                throw new ValidationException("Username already taken");
            }

            // Обновляем профиль
            userOrchestrator.updateUserProfile(userId, user.username(), newUsername, newName, Instant.now());

            log.info("[🔧] ✅ User {} updated profile", userId);
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to update profile for user {}: {}", userId, e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error updating profile for user {}: {}", userId, e.getMessage());
            return ResultNoArgs.error("Update profile failed due to server error");
        }
    }
    
    @Transactional
    public ResultNoArgs deleteUser(long userIdToDeleted, long userWhoDelete) {
        try {
            if (userIdToDeleted != userWhoDelete) {
                validator.validateActiveUser(userWhoDelete);
            }
            validator.validateActiveUser(userIdToDeleted);

            // удаляем
            Instant updatedAt = Instant.now();
            userOrchestrator.deleteUser(userIdToDeleted, updatedAt);

            log.info("[🔧] ✅ User {} deleted profile {}", userWhoDelete, userIdToDeleted);
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to delete profile for user {}: {}", userWhoDelete, e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error deleting profile for user {}: {}", userWhoDelete, e.getMessage());
            return ResultNoArgs.error("Update profile failed due to server error");
        }
    }

    @Transactional(readOnly = true)
    public ResultOneArg<UserProfileLight> getMyProfile(long userId) {
        try {
            validator.validateActiveUser(userId);

            UserProfileLight profile = userOrchestrator.getUserProfileLight(userId)
                    .orElseThrow(() -> new ValidationException("User not found or is deleted"));

            log.debug("[🔧] ✅ Loaded profile for user {}", userId);
            return ResultOneArg.success(profile);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get self profile for user {}: {}", userId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting self profile for user {}: {}", userId, e.getMessage());
            return ResultOneArg.error("Get profile failed due to server error");
        }
    }
    
    @Transactional(readOnly = true)
    public ResultOneArg<UserProfileLight> getOtherProfileLight(long currentUserId, long otherUserId) {
        try {
            validator.validateActiveUser(currentUserId);
            validator.validateActiveUser(otherUserId);

            UserProfileLight profile = userOrchestrator.getUserProfileLight(otherUserId)
                    .orElseThrow(() -> new ValidationException("User not found or is deleted"));

            log.debug("[🔧] ✅ User {} retrieved light profile of user {}", currentUserId, otherUserId);
            return ResultOneArg.success(profile);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get other light profile for user {}: {}", otherUserId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting other light profile for user {}: {}", otherUserId, e.getMessage());
            return ResultOneArg.error("Get profile failed due to server error");
        }
    }
    
    @Transactional(readOnly = true)
    public ResultOneArg<UserProfileFull> getOtherProfileFull(long currentUserId, long otherUserId) {
        try {
            validator.validateActiveUser(currentUserId);
            validator.validateActiveUser(otherUserId);

            UserProfileFull profile = userOrchestrator.getUserProfileFull(otherUserId)
                    .orElseThrow(() -> new ValidationException("User not found or is deleted"));

            log.debug("[🔧] ✅ User {} retrieved full profile of user {}", currentUserId, otherUserId);
            return ResultOneArg.success(profile);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get other full profile for user {}: {}", otherUserId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting other full profile for user {}: {}", otherUserId, e.getMessage());
            return ResultOneArg.error("Get profile failed due to server error");
        }
    }

    @Transactional(readOnly = true)
    public ResultOneArg<List<UserProfileLight>> getOtherProfileLightByIds(long currentUser, Set<Long> userIds) {
        try {
            validator.validateActiveUser(currentUser);

            List<UserProfileLight> profiles = userOrchestrator.getUserProfileLightsByIds(userIds);

            log.debug("[🔧] ✅ User {} retrieved full profile of {} users", currentUser, profiles.size());
            return ResultOneArg.success(profiles);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get other full profile for {} users: {}", userIds.size(), e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting other full profile for {} users: {}", userIds.size(), e.getMessage());
            return ResultOneArg.error("Get profile failed due to server error");
        }
    }
    
    @Transactional(readOnly = true)
    public ResultOneArg<UsersPage> getActiveUsersPage(long userId, String filter, Long cursor, int limit) {
        try {
            validator.validateActiveUser(userId);

            UsersPage usersPage = userOrchestrator.getActiveUserProfileLightsPage(filter, cursor, limit);

            log.debug("[🔧] ✅ Get {} users with filter='{}', nextCursor={}, limit={}", usersPage.users().size(), filter, cursor, limit);
            return ResultOneArg.success(usersPage);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to getFilteredUsers: {}", e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error during getFilteredUsers: {}", e.getMessage());
            return ResultOneArg.error("Get filtered users failed due to server error");
        }
    }

    @Transactional(readOnly = true)
    public ResultOneArg<GlobalEventSync> syncUserEvents(long userId, long cursor) {
        try {
            validator.validateActiveUser(userId);
            GlobalEventSync sync = userOrchestrator.getSyncUser(userId, cursor);
            return ResultOneArg.success(sync);
        } catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to sync user events for user {}: {}", userId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        } catch (Exception e) {
            log.error("[🔧] ⚠️ Error syncing user events for user {}: {}", userId, e.getMessage());
            return ResultOneArg.error("syncUserEvents failed due to server error");
        }
    }
}