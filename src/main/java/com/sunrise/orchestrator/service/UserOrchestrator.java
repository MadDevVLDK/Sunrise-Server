package com.sunrise.orchestrator.service;

import static org.springframework.transaction.annotation.Propagation.MANDATORY;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

import com.sunrise.cache.CacheEvent;
import com.sunrise.cache.entity.*;
import com.sunrise.cache.service.UserCacheService;
import com.sunrise.core.creation.CreateLoginHistoryDTO;
import com.sunrise.core.creation.CreateUserDTO;
import com.sunrise.db.result.*;
import com.sunrise.db.service.LoginHistoryDbService;
import com.sunrise.db.service.UserDbService;
import com.sunrise.helpclass.mapper.OtherMapper;
import com.sunrise.helpclass.mapper.UserMapper;
import com.sunrise.orchestrator.result.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserOrchestrator {

    private final ApplicationEventPublisher eventPublisher;
    
    private final UserCacheService cacheUserService;
    private final UserDbService dbUserService;
    
    private final LoginHistoryDbService dbLoginHistoryService;


    // ========== USER METHODS ==========


    // Основные методы

    @Transactional(propagation = MANDATORY)
    public void saveUser(@NonNull CreateUserDTO user) {
        // синхронно в бд
        dbUserService.saveUser(UserMapper.toEntity(user));

        // публикуем для обновления кеша после коммита
        eventPublisher.publishEvent(new CacheEvent.UserCreated(
            UserMapper.toProfileCache(user),
            UserMapper.toSecurityCache(user)
        ));
    }

    @Transactional(propagation = MANDATORY)
    public boolean updateUserProfile(long userId, String oldUsername, String username, String name, Instant updatedAt) {
        // синхронно в БД
        int updated = dbUserService.updateUserProfile(userId, username, name, updatedAt);
        if (updated > 0) {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.UserProfileUpdated(userId, oldUsername, username));
        }
        return updated > 0;
    }

    @Transactional(propagation = MANDATORY)
    public boolean updateUserEmail(long userId, String oldEmail, String email, Instant updatedAt) {
        // синхронно в БД
        int updated = dbUserService.updateUserEmail(userId, email, updatedAt);
        if (updated > 0) {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.UserEmailUpdated(userId, oldEmail, email));
        }
        return updated > 0;
    }

    @Transactional(propagation = MANDATORY)
    public boolean updateUserPassword(long userId, String password, Instant updatedAt) {
        // синхронно в БД
        int updated = dbUserService.updateUserPassword(userId, password, updatedAt);
        if (updated > 0) {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.UserSecurityInvalidated(userId));
        }
        return updated > 0;
    }

    @Transactional(propagation = MANDATORY)
    public boolean enableUser(long userId, Instant updatedAt) {
        // синхронно в БД
        int updated = dbUserService.enableUser(userId, updatedAt);
        if (updated > 0) {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.UserSecurityInvalidated(userId));
        }
        return updated > 0;
    }

    @Transactional(propagation = MANDATORY)
    public boolean deleteUser(long userId, Instant updatedAt) {
        // синхронно в БД
        int updated = dbUserService.deleteUser(userId, updatedAt);
        if (updated > 0) {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.UserSecurityAndProfileInvalidated(userId));
        }
        return updated > 0;
    }

    @Transactional(propagation = MANDATORY)
    public boolean restoreUser(long userId, Instant updatedAt) {
        // синхронно в БД
        int updated = dbUserService.restoreUser(userId, updatedAt);
        if (updated > 0) {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.UserSecurityAndProfileInvalidated(userId));
        }
        return updated > 0;
    }

    
    // Вспомогательные метод для сохранения истории входа

    @Transactional(propagation = REQUIRES_NEW)
    public void saveLoginHistory(String username, CreateLoginHistoryDTO loginHistory) {
        // синхронно в бд
        dbUserService.updateLastLogin(username, loginHistory.getLoginAt());
        dbLoginHistoryService.save(OtherMapper.toLoginHistoryEntity(loginHistory));
    }


    // Вспомогательные методы
    
    public boolean existsUserByUsername(String username) {
        // проверяем в кеше
        if (cacheUserService.existsByUsername(username))
            return true;

        // проверяем в бд
        Optional<UserSecurityResult> dbUser = dbUserService.getUserSecurityByUsername(username);
        dbUser.ifPresent(user -> {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.UserSecuritySave(
                UserMapper.toSecurityCache(user)
            ));
        });
        return dbUser.isPresent();
    }

    public boolean existsUserByEmail(String email)  {
        // проверяем в кеше
        if (cacheUserService.existsByEmail(email))
            return true;

        // проверяем в бд
        Optional<UserSecurityResult> dbUser = dbUserService.getUserSecurityByEmail(email);
        dbUser.ifPresent(user -> {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.UserSecuritySave(
                UserMapper.toSecurityCache(user)
            ));
        });
        return dbUser.isPresent();
    }

    public boolean isActiveUser(long userId) {
        // пробуем кеш
        Optional<CacheUserSecurity> cached = cacheUserService.getSecurity(userId);
        if (cached.isPresent())
            return cached.filter(us -> us.isEnabled() && !us.isDeleted()).isPresent();

        // грузим из бд
        Optional<UserSecurityResult> dbUser = dbUserService.getUserSecurity(userId);
        dbUser.ifPresent(user -> {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.UserSecuritySave(
                UserMapper.toSecurityCache(user)
            ));
        });
        return dbUser.filter(us -> us.getIsEnabled() && !us.getIsDeleted()).isPresent();
    }
    
    public Optional<UserSecurityDTO> getUserSecurity(long userId) {
        // пробуем кеш
        Optional<CacheUserSecurity> cached = cacheUserService.getSecurity(userId);
        if (cached.isPresent())
            return cached.map(UserMapper::toSecurityDTO);

        // грузим из бд
        Optional<UserSecurityResult> dbUser = dbUserService.getUserSecurity(userId);
        dbUser.ifPresent(user -> {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.UserSecuritySave(
                UserMapper.toSecurityCache(user)
            ));
        });
        return dbUser.map(UserMapper::toSecurityDTO);
    }

    public Optional<UserSecurityDTO> getActiveUserSecurity(long userId) {
        // пробуем кеш
        Optional<CacheUserSecurity> cached = cacheUserService.getSecurity(userId);
        if (cached.isPresent())
            return cached.filter(us -> us.isEnabled() && !us.isDeleted()).map(UserMapper::toSecurityDTO);

        // грузим из бд
        Optional<UserSecurityResult> dbUser = dbUserService.getUserSecurity(userId);
        dbUser.ifPresent(user -> {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.UserSecuritySave(
                UserMapper.toSecurityCache(user)
            ));
        });
        return dbUser.filter(us -> us.getIsEnabled() && !us.getIsDeleted()).map(UserMapper::toSecurityDTO);
    }

    public Optional<UserSecurityDTO> getActiveUserSecurityByUsername(String username) {
        // пробуем кеш
        Optional<CacheUserSecurity> cached = cacheUserService.getSecurityByUsername(username);
        if (cached.isPresent())
            return cached.filter(us -> us.isEnabled() && !us.isDeleted()).map(UserMapper::toSecurityDTO);

        // грузим из бд
        Optional<UserSecurityResult> dbUser = dbUserService.getUserSecurityByUsername(username);
        dbUser.ifPresent(user -> {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.UserSecuritySave(
                UserMapper.toSecurityCache(user)
            ));
        });
        return dbUser.filter(us -> us.getIsEnabled() && !us.getIsDeleted()).map(UserMapper::toSecurityDTO);
    }

    public Optional<UserSecurityDTO> getActiveUserSecurityByEmail(String email) {
        // пробуем кеш
        Optional<CacheUserSecurity> cached = cacheUserService.getSecurityByEmail(email);
        if (cached.isPresent())
            return cached.filter(us -> us.isEnabled() && !us.isDeleted()).map(UserMapper::toSecurityDTO);

        // грузим из бд
        Optional<UserSecurityResult> dbUser = dbUserService.getUserSecurityByEmail(email);
        dbUser.ifPresent(user -> {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.UserSecuritySave(
                UserMapper.toSecurityCache(user)
            ));
        });
        return dbUser.filter(us -> us.getIsEnabled() && !us.getIsDeleted()).map(UserMapper::toSecurityDTO);
    }

    public Optional<UserProfileLightDTO> getUserProfileLight(long userId) {
        // пробуем кеш
        Optional<CacheUserProfile> cached = cacheUserService.getProfile(userId);
        if (cached.isPresent())
            return cached.map(UserMapper::toProfileLightDTO);

        // грузим из бд
        Optional<UserProfileResult> dbUser = dbUserService.getUserProfile(userId);
        dbUser.ifPresent(user -> {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.UserProfileSave(
                UserMapper.toProfileCache(user)
            ));
        });
        return dbUser.map(UserMapper::toProfileLightDTO);
    }

    public Optional<UserProfileFullDTO> getUserProfileFull(long userId) {
        // пробуем кеш
        Optional<CacheUserProfile> cached = cacheUserService.getProfile(userId);
        if (cached.isPresent())
            return cached.map(UserMapper::toProfileFullDTO);

        // грузим из бд
        Optional<UserProfileResult> dbUser = dbUserService.getUserProfile(userId);
        dbUser.ifPresent(user -> {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.UserProfileSave(
                UserMapper.toProfileCache(user)
            ));
        });
        return dbUser.map(UserMapper::toProfileFullDTO);
    }

    public Optional<Integer> getUserJwtVersion(long userId) {
        // пробуем кеш
        Optional<CacheUserSecurity> cached = cacheUserService.getSecurity(userId);
        if (cached.isPresent())
            return cached.map(CacheUserSecurity::getJwtVersion);

        // грузим из бд
        Optional<UserSecurityResult> dbUser = dbUserService.getUserSecurity(userId);
        dbUser.ifPresent(user -> {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.UserSecuritySave(
                UserMapper.toSecurityCache(user)
            ));
        });
        return dbUser.map(UserSecurityResult::getJwtVersion);
    }


    // Вспомогательный метод для загрузки профилей пользователей с кешем

    public UsersPageDTO getActiveUserProfileLightsPage(String filter, Long cursor, int limit) {
        // получаем пагинацию из бд
        List<UserProfileResult> rows = dbUserService.getActiveUsersPage(filter, cursor, limit + 1); // берем на одну больше

        boolean hasMore = rows.size() > limit;
        List<UserProfileResult> pageRows = hasMore ? rows.subList(0, limit) : rows;
        List<UserProfileLightDTO> users = UserMapper.toProfileDTOs(pageRows);
        Long  nextCursor = hasMore ? pageRows.getLast().getId() : null;

        return new UsersPageDTO(users, nextCursor);
    }
    
    public List<UserProfileLightDTO> getUserProfileLightsByIds(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, UserProfileLightDTO> userMap = new HashMap<>();

        // Загружаем из кеша
        Set<Long> missingUserIds = new HashSet<>();
        Map<Long, CacheUserProfile> cachedUsers = cacheUserService.getProfilesByIds(userIds, missingUserIds);

        for (Map.Entry<Long, CacheUserProfile> entry : cachedUsers.entrySet()) {
            if (!entry.getValue().isDeleted()) {
                userMap.put(entry.getKey(), UserMapper.toProfileLightDTO(entry.getValue()));
            }
        }

        // Загружаем недостающих из БД
        if (!missingUserIds.isEmpty()) {
            List<UserProfileResult> dbUsers = dbUserService.getActiveUserProfileByIds(new ArrayList<>(missingUserIds));
            List<CacheUserProfile> usersToCache = new ArrayList<>();

            for (UserProfileResult user : dbUsers) {
                usersToCache.add(UserMapper.toProfileCache(user));
                userMap.put(user.getId(), UserMapper.toProfileLightDTO(user));
            }

            if (!usersToCache.isEmpty()) {
                // публикуем для обновления кеша после коммита
                eventPublisher.publishEvent(
                    new CacheEvent.UserProfilesSave(usersToCache)
                );
            }
        }

        // собираем результат
        List<UserProfileLightDTO> result = new LinkedList<>();
        for (long userId : userIds) {
            UserProfileLightDTO member = userMap.get(userId);
            if (member != null) {
                result.add(member);
            }
        }
        return result;
    }
}