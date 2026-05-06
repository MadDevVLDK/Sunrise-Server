package com.sunrise.orchestrator.service;

import static org.springframework.transaction.annotation.Propagation.MANDATORY;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

import com.sunrise.cache.entity.*;
import com.sunrise.cache.event.CacheEvent;
import com.sunrise.cache.service.UserCacheService;
import com.sunrise.core.creation.CreateDto;
import com.sunrise.db.result.*;
import com.sunrise.db.service.EventDbService;
import com.sunrise.db.service.LoginHistoryDbService;
import com.sunrise.db.service.UserDbService;
import com.sunrise.helpclass.mapper.OtherMapper;
import com.sunrise.helpclass.mapper.UserMapper;
import com.sunrise.orchestrator.event.EventType;
import com.sunrise.orchestrator.result.*;
import com.sunrise.orchestrator.result.Dto.GlobalEvent;
import com.sunrise.orchestrator.result.Dto.GlobalEventSync;

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
    private final EventDbService dbEventService;


    // ========== USER METHODS ==========


    // Основные методы

    @Transactional(propagation = MANDATORY)
    public void saveUser(@NonNull CreateDto.User user) {
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
            eventPublisher.publishEvent(new CacheEvent.UserProfileUpdated(userId, oldUsername));
        }
        return updated > 0;
    }

    @Transactional(propagation = MANDATORY)
    public boolean updateUserEmail(long userId, String oldEmail, String email, Instant updatedAt) {
        // синхронно в БД
        int updated = dbUserService.updateUserEmail(userId, email, updatedAt);
        if (updated > 0) {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.UserEmailUpdated(userId, oldEmail));
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
    public void saveLoginHistory(String username, CreateDto.LoginHistory loginHistory) {
        // синхронно в бд
        dbUserService.updateLastLogin(username, loginHistory.getLoginAt());
        dbLoginHistoryService.save(OtherMapper.toLoginHistoryEntity(loginHistory));
    }


    // Вспомогательные методы
    
    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public boolean isActiveUser(long userId) {
        // пробуем кеш
        Optional<Cache.UserSecurity> cached = cacheUserService.getSecurity(userId);
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
    
    @Transactional(readOnly = true)
    public Optional<Dto.UserSecurity> getUserSecurity(long userId) {
        // пробуем кеш
        Optional<Cache.UserSecurity> cached = cacheUserService.getSecurity(userId);
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

    @Transactional(readOnly = true)
    public Optional<Dto.UserSecurity> getActiveUserSecurity(long userId) {
        // пробуем кеш
        Optional<Cache.UserSecurity> cached = cacheUserService.getSecurity(userId);
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

    @Transactional(readOnly = true)
    public Optional<Dto.UserSecurity> getActiveUserSecurityByUsername(String username) {
        // пробуем кеш
        Optional<Cache.UserSecurity> cached = cacheUserService.getSecurityByUsername(username);
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

    @Transactional(readOnly = true)
    public Optional<Dto.UserSecurity> getActiveUserSecurityByEmail(String email) {
        // пробуем кеш
        Optional<Cache.UserSecurity> cached = cacheUserService.getSecurityByEmail(email);
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

    @Transactional(readOnly = true)
    public Optional<Dto.UserProfileLight> getUserProfileLight(long userId) {
        // пробуем кеш
        Optional<Cache.UserProfile> cached = cacheUserService.getProfile(userId);
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

    @Transactional(readOnly = true)
    public Optional<Dto.UserProfileFull> getUserProfileFull(long userId) {
        // пробуем кеш
        Optional<Cache.UserProfile> cached = cacheUserService.getProfile(userId);
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

    @Transactional(readOnly = true)
    public Optional<Integer> getUserJwtVersion(long userId) {
        // пробуем кеш
        Optional<Cache.UserSecurity> cached = cacheUserService.getSecurity(userId);
        if (cached.isPresent())
            return cached.map(Cache.UserSecurity::jwtVersion);

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

    private static final int MAX_DELTA_USER_EVENTS = 2000;

    @Transactional(readOnly = true)
    public GlobalEventSync getSyncUser(long userId, long cursor) {
        if (dbEventService.isUserSyncResetRequired(userId, cursor, MAX_DELTA_USER_EVENTS)) {
            return new GlobalEventSync(Collections.emptyList(), false, true);
        } 

        List<UserEventResult> events = dbEventService.getUserEventsAfter(userId, cursor, 101); // +1
        List<GlobalEvent> clientEvents = events.stream()
            .map(proj -> new Dto.GlobalEvent(
                proj.getEventId(),
                EventType.valueOf(proj.getEventType()),
                dbEventService.deserializeEvent(proj.getEventType(), proj.getPayload()),
                proj.getCreatedAt()
            )).limit(100).toList();

        return new GlobalEventSync(clientEvents, events.size() > 100, false);
    }

    @Transactional(readOnly = true)
    public Dto.UsersPage getActiveUserProfileLightsPage(String filter, Long cursor, int limit) {
        // получаем пагинацию из бд
        List<UserProfileResult> rows = dbUserService.getActiveUsersPage(filter, cursor, limit + 1); // берем на одну больше

        boolean hasMore = rows.size() > limit;
        List<UserProfileResult> pageRows = hasMore ? rows.subList(0, limit) : rows;
        List<Dto.UserProfileLight> users = UserMapper.toProfileDTOs(pageRows);
        Long  nextCursor = hasMore ? pageRows.getLast().getId() : null;

        return new Dto.UsersPage(users, nextCursor);
    }
    
    @Transactional(readOnly = true)
    public List<Dto.UserProfileLight> getUserProfileLightsByIds(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Dto.UserProfileLight> userMap = new HashMap<>();

        // Загружаем из кеша
        Set<Long> missingUserIds = new HashSet<>();
        Map<Long, Cache.UserProfile> cachedUsers = cacheUserService.getProfilesByIds(userIds, missingUserIds);

        for (Map.Entry<Long, Cache.UserProfile> entry : cachedUsers.entrySet()) {
            if (!entry.getValue().isDeleted()) {
                userMap.put(entry.getKey(), UserMapper.toProfileLightDTO(entry.getValue()));
            }
        }

        // Загружаем недостающих из БД
        if (!missingUserIds.isEmpty()) {
            List<UserProfileResult> dbUsers = dbUserService.getActiveUserProfileByIds(new ArrayList<>(missingUserIds));
            List<Cache.UserProfile> usersToCache = new ArrayList<>();

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
        List<Dto.UserProfileLight> result = new LinkedList<>();
        for (long userId : userIds) {
            if (userMap.get(userId) instanceof Dto.UserProfileLight member) {
                result.add(member);
            }
        }
        return result;
    }
}