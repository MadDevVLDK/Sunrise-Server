package com.sunrise.orchestrator.service;

import static org.springframework.transaction.annotation.Propagation.MANDATORY;

import com.sunrise.cache.CacheEvent;
import com.sunrise.cache.entity.*;
import com.sunrise.cache.service.ChatMemberCacheService;
import com.sunrise.cache.service.UserCacheService;
import com.sunrise.core.creation.CreateChatMemberDTO;
import com.sunrise.db.entity.ChatMember;
import com.sunrise.db.event.ChatEvent;
import com.sunrise.db.result.UserProfileResult;
import com.sunrise.db.service.ChatEventDbService;
import com.sunrise.db.service.ChatMemberDbService;
import com.sunrise.db.service.UserDbService;
import com.sunrise.helpclass.mapper.ChatEventMapper;
import com.sunrise.helpclass.mapper.ChatMemberMapper;
import com.sunrise.helpclass.mapper.UserMapper;
import com.sunrise.orchestrator.result.ServiceDTO;

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
public class ChatMemberOrchestrator {

    private final ApplicationEventPublisher eventPublisher;
    
    private final ChatMemberCacheService cacheChatMemberService;
    private final ChatMemberDbService dbChatMemberService;

    private final UserCacheService cacheUserService;
    private final UserDbService dbUserService;

    private final ChatEventDbService chatEventDbService;


    // ========== CHAT MEMBER METHODS ==========


    // Основные методы

    @Transactional(propagation = MANDATORY)
    public boolean saveOrRestore(@NonNull CreateChatMemberDTO chatMember) {
        // синхронно в бд
        boolean saved = dbChatMemberService.saveOrRestore(ChatMemberMapper.toEntity(chatMember));
        if (saved) {
            var event = new ChatEvent.ChatMemberAdded(
                chatMember.getChatId(), chatMember.getUserId(), 
                chatMember.isAdmin(), chatMember.getJoinedAt(), 
                chatMember.getJoinedAt()
            );
            chatEventDbService.save(ChatEventMapper.toEntity(event));

            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.ChatMemberAdded(
                ChatMemberMapper.toCache(chatMember)
            ));
        }
        return saved;
    }

    @Transactional(propagation = MANDATORY)
    public Long[] saveOrRestoreBatch(long chatId, @NonNull List<CreateChatMemberDTO> chatMembers) {
        // конвертируем
        Instant joinedAt = chatMembers.getFirst().getJoinedAt();
        Long[] memberIds = new Long[chatMembers.size()];
        for (int i = 0; i < chatMembers.size(); i++) {
            memberIds[i] = chatMembers.get(i).getUserId();
        }

        // синхронно в бд
        Long[] addedIds = dbChatMemberService.saveOrRestoreBatch(chatId, memberIds, joinedAt);
        if (addedIds.length > 0) {
            // Фильтруем только тех, кто реально добавлен/восстановлен
            List<Long> addedUserIds = Arrays.asList(addedIds);
            List<Boolean> admins = chatMembers.stream()
                .filter(m -> addedUserIds.contains(m.getUserId()))
                .map(CreateChatMemberDTO::isAdmin).toList();

            var event = new ChatEvent.ChatMembersAdded(
                chatId, addedUserIds,
                admins, joinedAt, Instant.now()
            );
            chatEventDbService.save(ChatEventMapper.toEntity(event));


            // Фильтруем только тех, кто реально добавлен/восстановлен
            List<CreateChatMemberDTO> addedMembers = chatMembers.stream()
                    .filter(m -> Arrays.asList(addedIds).contains(m.getUserId())).toList();

            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.ChatMembersAdded(
                chatId, ChatMemberMapper.toCaches(addedMembers)
            ));
        }
        return addedIds;
    }

    @Transactional(propagation = MANDATORY)
    public boolean updateProfile(long chatId, long userId, String tag, Instant updatedAt) {
        // синхронно в бд
        int updated = dbChatMemberService.updateProfile(chatId, userId, tag, updatedAt);
        if (updated > 0) {
            var event = new ChatEvent.ChatMemberInfoUpdate(
                chatId, userId, tag, updatedAt
            );
            chatEventDbService.save(ChatEventMapper.toEntity(event));

            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(
                new CacheEvent.ChatMemberInvalidated(chatId, userId)
            );
        }
        return updated > 0;
    }

    @Transactional(propagation = MANDATORY)
    public boolean updateAdminRights(long chatId, long userId, boolean isAdmin, Instant updatedAt) {
        // синхронно в бд
        int updated = dbChatMemberService.updateAdminRights(chatId, userId, isAdmin, updatedAt);
        if (updated > 0) {
            var event = new ChatEvent.ChatMemberAdminUpdated(
                chatId, userId, isAdmin, updatedAt
            );
            chatEventDbService.save(ChatEventMapper.toEntity(event));

            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(
                new CacheEvent.ChatMemberInvalidated(chatId, userId)
            );
        }
        return updated > 0;
    }

    @Transactional(propagation = MANDATORY)
    public boolean updateSettings(long chatId, long userId, boolean isPinned, Instant updatedAt) {
        // синхронно в бд
        int updated = dbChatMemberService.updateSettings(chatId, userId, isPinned, updatedAt);
        if (updated > 0) {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(
                new CacheEvent.ChatMemberInvalidated(chatId, userId)
            );
        }
        return updated > 0;
    }

    @Transactional(propagation = MANDATORY)
    public boolean remove(long chatId, long userId, Instant updatedAt) {
        // синхронно в бд
        boolean removed = dbChatMemberService.remove(userId, chatId, updatedAt);
        if (removed) {
            var event = new ChatEvent.ChatMemberRemoved(
                chatId, userId, updatedAt
            );
            chatEventDbService.save(ChatEventMapper.toEntity(event));

            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(
                new CacheEvent.ChatMemberInvalidated(chatId, userId)
            );
        }
        return removed;
    }


    // Вспомогательные методы
    
    public boolean hasActive(long chatId, long userId) {
        // проверка в кеше
        Optional<Boolean> hasActiveChatMember = cacheChatMemberService.hasActive(chatId, userId);
        if (hasActiveChatMember.isPresent())
            return hasActiveChatMember.get();

        // проверяем пользователя в чате
        Optional<ChatMember> dbMember = dbChatMemberService.get(chatId, userId);
        dbMember.ifPresent(member -> {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.ChatMemberSave(
                ChatMemberMapper.toCache(member)
            ));
        });
        return dbMember.map(ChatMember::isActive).orElse(false);
    }

    public Optional<Boolean> isActiveAdmin(long chatId, long userId) {
        // пробуем кеш
        Optional<Boolean> cached = cacheChatMemberService.isActiveAdmin(chatId, userId);
        if (cached.isPresent())
            return cached;

        // надо найти пользователя, добавить в кеш и отдать
        Optional<ChatMember> dbMember = dbChatMemberService.getActive(chatId, userId);
        dbMember.ifPresent(member -> {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.ChatMemberSave(
                ChatMemberMapper.toCache(member)
            ));
        });
        return dbMember.map(ChatMember::isAdmin);
    }

    public List<ChatMemberProfileDTO> getProfilesByIds(long chatId, @NonNull Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, ChatMemberProfileDTO> memberMap = new HashMap<>();

        // Загружаем из кеша
        Set<Long> missingMemberIds = new HashSet<>();
        Map<Long, CacheChatMember> cachedMembers = cacheChatMemberService.getBatch(chatId, userIds, missingMemberIds);

        for (Map.Entry<Long, CacheChatMember> entry : cachedMembers.entrySet()) {
            CacheChatMember cachedMember = entry.getValue();
            if (!cachedMember.isDeleted()) {
                memberMap.put(entry.getKey(), ChatMemberMapper.toProfileDTO(cachedMember));
            }
        }

        // Загружаем недостающих из БД
        if (!missingMemberIds.isEmpty()) {
            List<ChatMember> dbMembers = dbChatMemberService.getActiveBatch(chatId, new ArrayList<>(missingMemberIds));
            List<CacheChatMember> membersToCache = new LinkedList<>();

            for (ChatMember member : dbMembers) {
                membersToCache.add(ChatMemberMapper.toCache(member));
                memberMap.put(member.getUserId(), ChatMemberMapper.toProfileDTO(member));
            }

            if (!membersToCache.isEmpty()) {
                // публикуем для обновления кеша после коммита
                eventPublisher.publishEvent(new CacheEvent.ChatMembersSave(
                    chatId, membersToCache
                ));
            }
        }

        // собираем результат
        List<ChatMemberProfileDTO> result = new LinkedList<>();
        for (long userId : userIds) {
            ChatMemberProfileDTO member = memberMap.get(userId);
            if (member != null) {
                result.add(member);
            }
        }
        return result;
    }

    public ChatMembersPageDTO getPage(long chatId, Long cursor, int limit) {
        if (!cacheChatMemberService.hasResentIds(chatId)) {
            // Публикуем событие для фоновой инициализации после коммита
            eventPublisher.publishEvent(
                new CacheEvent.ChatMembersIdsInit(chatId)
            );

            // Загружаем страницу из БД без кэширования
            return loadPageFromDb(chatId, cursor, limit);
        }
        
        // Получаем limit + 1 ID из кеша (с учётом курсора)
        List<Long> neededIds = cacheChatMemberService.getRecentIdsRange(chatId, cursor, limit + 1);
        if (neededIds.isEmpty()) {
            return new ChatMembersPageDTO(Collections.emptyList(), null);
        }
        
        // Если кеш не полон, то используем кеш
        if (!cacheChatMemberService.isRecentIdsFull(chatId)) {
            return buildPageFromIds(chatId, neededIds, limit);
        }

        // Кэш полон – проверяем, достаточно ли ID
        if (neededIds.size() > limit) {
            return buildPageFromIds(chatId, neededIds, limit);
        } else {
            // Кэш не дал достаточно ID – идём в БД (глубокая пагинация)
            return loadPageFromDb(chatId, cursor, limit);
        }
    }

    private ChatMembersPageDTO buildPageFromIds(long chatId, List<Long> ids, int limit) {
        boolean hasMore = ids.size() > limit;
        List<Long> pageIds = hasMore ? ids.subList(0, limit) : ids;
        Long nextCursor = hasMore ? pageIds.getLast() : null;

        // Загружаем профили пользователей и участников (те же методы)
        Map<Long, UserProfileLightDTO> userMap = loadLightUserProfilesWithCache(new HashSet<>(pageIds));
        Map<Long, ChatMemberProfileDTO> memberMap = loadChatMemberProfilesWithCache(chatId, new HashSet<>(pageIds));

        List<ChatMemberProfileFullDTO> result = new LinkedList<>();
        for (long userId : pageIds) {
            UserProfileLightDTO user = userMap.get(userId);
            ChatMemberProfileDTO member = memberMap.get(userId);
            if (user != null && member != null) {
                result.add(ChatMemberMapper.toProfileFullDTO(user, member));
            }
        }
        return new ChatMembersPageDTO(result, nextCursor);
    }

    // TODO: Заменить потом на пагинацию полную из бд
    private ChatMembersPageDTO loadPageFromDb(long chatId, Long cursor, int limit) {
        List<Long> userIds = dbChatMemberService.getIdsPage(chatId, cursor, limit + 1);
        if (userIds.isEmpty()) {
            return new ChatMembersPageDTO(Collections.emptyList(), null);
        }
        boolean hasMore = userIds.size() > limit;
        List<Long> pageIds = hasMore ? userIds.subList(0, limit) : userIds;
        Long nextCursor = hasMore ? pageIds.getLast() : null;

        Map<Long, UserProfileLightDTO> userMap = loadLightUserProfilesWithCache(new HashSet<>(pageIds));
        Map<Long, ChatMemberProfileDTO> memberMap = loadChatMemberProfilesWithCache(chatId, new HashSet<>(pageIds));

        List<ChatMemberProfileFullDTO> result = new LinkedList<>();
        for (long userId : pageIds) {
            UserProfileLightDTO user = userMap.get(userId);
            ChatMemberProfileDTO member = memberMap.get(userId);
            if (user != null && member != null) {
                result.add(ChatMemberMapper.toProfileFullDTO(user, member));
            }
        }
        return new ChatMembersPageDTO(result, nextCursor);
    }


    // Вспомогательные методы для загрузки профилей пользователей с кешем

    private Map<Long, UserProfileLightDTO> loadLightUserProfilesWithCache(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
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

        return userMap;
    }

    private Map<Long, ChatMemberProfileDTO> loadChatMemberProfilesWithCache(long chatId, @NonNull Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, ChatMemberProfileDTO> memberMap = new HashMap<>();

        // Загружаем из кеша
        Set<Long> missingMemberIds = new HashSet<>();
        Map<Long, CacheChatMember> cachedMembers = cacheChatMemberService.getBatch(chatId, userIds, missingMemberIds);

        for (Map.Entry<Long, CacheChatMember> entry : cachedMembers.entrySet()) {
            CacheChatMember cachedMember = entry.getValue();
            if (!cachedMember.isDeleted()) {
                memberMap.put(entry.getKey(), ChatMemberMapper.toProfileDTO(cachedMember));
            }
        }

        // Загружаем недостающих из БД
        if (!missingMemberIds.isEmpty()) {
            List<ChatMember> dbMembers = dbChatMemberService.getActiveBatch(chatId, new ArrayList<>(missingMemberIds));
            List<CacheChatMember> membersToCache = new ArrayList<>();

            for (ChatMember member : dbMembers) {
                membersToCache.add(ChatMemberMapper.toCache(member));
                memberMap.put(member.getUserId(), ChatMemberMapper.toProfileDTO(member));
            }

            if (!membersToCache.isEmpty()) {
                // публикуем для обновления кеша после коммита
                eventPublisher.publishEvent(
                    new CacheEvent.ChatMembersSave(chatId, membersToCache)
                );
            }
        }

        return memberMap;
    }
}