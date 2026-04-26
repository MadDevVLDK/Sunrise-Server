package com.sunrise.dataservice;

import static org.springframework.transaction.annotation.Propagation.MANDATORY;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

import com.sunrise.cache.CacheEvent;
import com.sunrise.cache.CacheService;
import com.sunrise.cache.entity.*;
import com.sunrise.db.entity.ChatMember;
import com.sunrise.db.entity.Message;
import com.sunrise.db.entity.VerificationToken;
import com.sunrise.db.result.*;
import com.sunrise.db.transaction.ChatDbService;
import com.sunrise.db.transaction.ChatMemberDbService;
import com.sunrise.db.transaction.LoginHistoryDbService;
import com.sunrise.db.transaction.MessageDbService;
import com.sunrise.db.transaction.UserDbService;
import com.sunrise.db.transaction.VerificationTokenDbService;
import com.sunrise.dataservice.type.ChatType;
import com.sunrise.dataservice.type.Direction;
import com.sunrise.service.creation.*;
import com.sunrise.dataservice.result.*;
import com.sunrise.helpclass.EntityMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class DataOrchestrator {

    private final ApplicationEventPublisher eventPublisher;

    private final CacheService cacheService;

    private final UserDbService dbUserService;
    private final ChatDbService dbChatService;
    private final ChatMemberDbService dbChatMemberService;
    private final MessageDbService dbMessageService;
    private final VerificationTokenDbService verificationTokenDb;
    private final LoginHistoryDbService dbLoginHistoryService;


    // ========= EVENTS HANDLERS ========

    @PostConstruct
    public void warmUpCache() {
        // TODO: подумать чо буду в при старте загружать
        log.info("Cache warm-up completed");
    }
    @PreDestroy
    public void onShutdown() {
        // TODO: подумать чо буду при завершении делать
        log.info("OnShutdown completed");
    }


    // ========== USER METHODS ==========


    // Основные методы

    @Transactional(propagation = MANDATORY)
    public void saveUser(@NonNull CreateUserDTO user) {
        dbUserService.saveUser(EntityMapper.toUserEntity(user));
        eventPublisher.publishEvent(new CacheEvent.UserCreated(
            EntityMapper.toUserProfileCache(user),
            EntityMapper.toUserSecurityCache(user)
        ));
    }

    @Transactional(propagation = MANDATORY)
    public void updateUserProfile(long userId, String oldUsername, String username, String name, LocalDateTime updatedAt) {
        if (dbUserService.updateUserProfile(userId, username, name, updatedAt) > 0) {
            eventPublisher.publishEvent(new CacheEvent.UserProfileUpdated(userId, oldUsername, username));
        }
    }

    @Transactional(propagation = MANDATORY)
    public void updateUserEmail(long userId, String oldEmail, String email, LocalDateTime updatedAt) {
        if (dbUserService.updateUserEmail(userId, email, updatedAt) > 0) {
            eventPublisher.publishEvent(new CacheEvent.UserEmailUpdated(userId, oldEmail, email));
        }
    }

    @Transactional(propagation = MANDATORY)
    public void updateUserPassword(long userId, String password, LocalDateTime updatedAt) {
        if (dbUserService.updateUserPassword(userId, password, updatedAt) > 0) {
            eventPublisher.publishEvent(new CacheEvent.UserSecurityInvalidated(userId));
        }
    }

    @Transactional(propagation = MANDATORY)
    public void enableUser(long userId, LocalDateTime updatedAt) {
        if (dbUserService.enableUser(userId, updatedAt) > 0) {
            eventPublisher.publishEvent(new CacheEvent.UserSecurityInvalidated(userId));
        }
    }

    @Transactional(propagation = MANDATORY)
    public void deleteUser(long userId, LocalDateTime updatedAt) {
        if (dbUserService.deleteUser(userId, updatedAt) > 0) {
            eventPublisher.publishEvent(new CacheEvent.UserSecurityAndProfileInvalidated(userId));
        }
    }

    @Transactional(propagation = MANDATORY)
    public void restoreUser(long userId, LocalDateTime updatedAt) {
        if (dbUserService.restoreUser(userId, updatedAt) > 0) {
            eventPublisher.publishEvent(new CacheEvent.UserSecurityAndProfileInvalidated(userId));
        }
    }


    // Вспомогательные методы
    
    public boolean existsUserByUsername(String username) {
        // проверяем в кеше
        if (cacheService.existsUserByUsername(username))
            return true;

        // проверяем в бд
        Optional<UserSecurityResult> dbUser = dbUserService.getUserSecurityByUsername(username);
        dbUser.ifPresent(user -> {
            cacheService.saveUserSecurity(EntityMapper.toUserSecurityCache(user)); // восстанавливаем кеш
        });
        return dbUser.isPresent();
    }
    public boolean existsUserByEmail(String email)  {
        // проверяем в кеше
        if (cacheService.existsUserByEmail(email))
            return true;

        // проверяем в бд
        Optional<UserSecurityResult> dbUser = dbUserService.getUserSecurityByEmail(email);
        dbUser.ifPresent(user -> {
            cacheService.saveUserSecurity(EntityMapper.toUserSecurityCache(user)); // восстанавливаем кеш
        });
        return dbUser.isPresent();
    }
    public boolean isActiveUser(long userId) {
        // пробуем кеш
        Optional<CacheUserSecurity> cached = cacheService.getUserSecurity(userId);
        if (cached.isPresent())
            return cached.filter(us -> us.isEnabled() && !us.isDeleted()).isPresent();

        // грузим из бд
        Optional<UserSecurityResult> dbUser = dbUserService.getUserSecurity(userId);
        dbUser.ifPresent(user -> {
            cacheService.saveUserSecurity(EntityMapper.toUserSecurityCache(user)); // восстанавливаем кеш
        });
        return dbUser.filter(us -> us.getIsEnabled() && !us.getIsDeleted()).isPresent();
    }
    
    public Optional<UserSecurityDTO> getUserSecurity(long userId) {
        // пробуем кеш
        Optional<CacheUserSecurity> cached = cacheService.getUserSecurity(userId);
        if (cached.isPresent())
            return cached.map(EntityMapper::toUserSecurityDTO);

        // грузим из бд
        Optional<UserSecurityResult> dbUser = dbUserService.getUserSecurity(userId);
        dbUser.ifPresent(user -> {
            cacheService.saveUserSecurity(EntityMapper.toUserSecurityCache(user)); // восстанавливаем кеш
        });
        return dbUser.map(EntityMapper::toUserSecurityDTO);
    }
    public Optional<UserSecurityDTO> getActiveUserSecurity(long userId) {
        // пробуем кеш
        Optional<CacheUserSecurity> cached = cacheService.getUserSecurity(userId);
        if (cached.isPresent())
            return cached.filter(us -> us.isEnabled() && !us.isDeleted()).map(EntityMapper::toUserSecurityDTO);

        // грузим из бд
        Optional<UserSecurityResult> dbUser = dbUserService.getUserSecurity(userId);
        dbUser.ifPresent(user -> {
            cacheService.saveUserSecurity(EntityMapper.toUserSecurityCache(user)); // восстанавливаем кеш
        });
        return dbUser.filter(us -> us.getIsEnabled() && !us.getIsDeleted()).map(EntityMapper::toUserSecurityDTO);
    }
    public Optional<UserSecurityDTO> getActiveUserSecurityByUsername(String username) {
        // пробуем кеш
        Optional<CacheUserSecurity> cached = cacheService.getUserSecurityByUsername(username);
        if (cached.isPresent())
            return cached.filter(us -> us.isEnabled() && !us.isDeleted()).map(EntityMapper::toUserSecurityDTO);

        //грузим из бд
        Optional<UserSecurityResult> dbUser = dbUserService.getUserSecurityByUsername(username);
        dbUser.ifPresent(user -> {
            cacheService.saveUserSecurityAndUsernameIndex(EntityMapper.toUserSecurityCache(user), username); // восстанавливаем кеш
        });
        return dbUser.filter(us -> us.getIsEnabled() && !us.getIsDeleted()).map(EntityMapper::toUserSecurityDTO);
    }
    public Optional<UserSecurityDTO> getActiveUserSecurityByEmail(String email) {
        // пробуем кеш
        Optional<CacheUserSecurity> cached = cacheService.getUserSecurityByEmail(email);
        if (cached.isPresent())
            return cached.filter(us -> us.isEnabled() && !us.isDeleted()).map(EntityMapper::toUserSecurityDTO);

        //грузим из бд
        Optional<UserSecurityResult> dbUser = dbUserService.getUserSecurityByEmail(email);
        dbUser.ifPresent(user -> {
            cacheService.saveUserSecurity(EntityMapper.toUserSecurityCache(user)); // восстанавливаем кеш
        });
        return dbUser.filter(us -> us.getIsEnabled() && !us.getIsDeleted()).map(EntityMapper::toUserSecurityDTO);
    }
    public Optional<UserProfileLightDTO> getUserProfileLight(long userId) {
        // пробуем кеш
        Optional<CacheUserProfile> cached = cacheService.getUserProfile(userId);
        if (cached.isPresent())
            return cached.map(EntityMapper::toUserProfileLightDTO);

        //грузим из бд
        Optional<UserProfileResult> dbUser = dbUserService.getUserProfile(userId);
        dbUser.ifPresent(user -> {
            cacheService.saveUserProfile(EntityMapper.toUserProfileCache(user)); // восстанавливаем кеш
        });
        return dbUser.map(EntityMapper::toUserProfileLightDTO);
    }
    public Optional<UserProfileFullDTO> getUserProfileFull(long userId) {
        // пробуем кеш
        Optional<CacheUserProfile> cached = cacheService.getUserProfile(userId);
        if (cached.isPresent())
            return cached.map(EntityMapper::toUserProfileFullDTO);

        //грузим из бд
        Optional<UserProfileResult> dbUser = dbUserService.getUserProfile(userId);
        dbUser.ifPresent(user -> {
            cacheService.saveUserProfile(EntityMapper.toUserProfileCache(user)); // восстанавливаем кеш
        });
        return dbUser.map(EntityMapper::toUserProfileFullDTO);
    }
    public Optional<Integer> getUserJwtVersion(long userId) {
        // пробуем кеш
        Optional<CacheUserSecurity> cached = cacheService.getUserSecurity(userId);
        if (cached.isPresent())
            return cached.map(CacheUserSecurity::getJwtVersion);

        // грузим из бд
        Optional<UserSecurityResult> dbUser = dbUserService.getUserSecurity(userId);
        dbUser.ifPresent(user -> {
            cacheService.saveUserSecurity(EntityMapper.toUserSecurityCache(user)); // восстанавливаем кеш
        });
        return dbUser.map(UserSecurityResult::getJwtVersion);
    }

    private Map<Long, UserProfileLightDTO> loadUserProfileLightsWithCache(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, UserProfileLightDTO> userMap = new HashMap<>();

        // Загружаем из кеша
        Set<Long> missingUserIds = new HashSet<>();
        Map<Long, CacheUserProfile> cachedUsers = cacheService.getCacheUsersByIds(userIds, missingUserIds);

        for (Map.Entry<Long, CacheUserProfile> entry : cachedUsers.entrySet()) {
            if (!entry.getValue().isDeleted()) {
                userMap.put(entry.getKey(), EntityMapper.toUserProfileLightDTO(entry.getValue()));
            }
        }

        // Загружаем недостающих из БД
        if (!missingUserIds.isEmpty()) {
            List<UserProfileResult> dbUsers = dbUserService.getActiveUserProfileByIds(new ArrayList<>(missingUserIds));
            List<CacheUserProfile> usersToCache = new ArrayList<>();

            for (UserProfileResult user : dbUsers) {
                usersToCache.add(EntityMapper.toUserProfileCache(user));
                userMap.put(user.getId(), EntityMapper.toUserProfileLightDTO(user));
            }

            if (!usersToCache.isEmpty()) {
                cacheService.saveUsersProfile(usersToCache);
            }
        }

        return userMap;
    }
    public UsersPageDTO getActiveUserProfileLightsPage(String filter, Long cursor, int limit) {
        // получаем пагинацию из бд
        List<UserProfileResult> rows = dbUserService.getActiveUsersPage(filter, cursor, limit + 1); // берем на одну больше

        boolean hasMore = rows.size() > limit;
        List<UserProfileResult> pageRows = hasMore ? rows.subList(0, limit) : rows;
        List<UserProfileLightDTO> users = EntityMapper.toDtoUserProfiles(pageRows);
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
        Map<Long, CacheUserProfile> cachedUsers = cacheService.getCacheUsersByIds(userIds, missingUserIds);

        for (Map.Entry<Long, CacheUserProfile> entry : cachedUsers.entrySet()) {
            if (!entry.getValue().isDeleted()) {
                userMap.put(entry.getKey(), EntityMapper.toUserProfileLightDTO(entry.getValue()));
            }
        }

        // Загружаем недостающих из БД
        if (!missingUserIds.isEmpty()) {
            List<UserProfileResult> dbUsers = dbUserService.getActiveUserProfileByIds(new ArrayList<>(missingUserIds));
            List<CacheUserProfile> usersToCache = new ArrayList<>();

            for (UserProfileResult user : dbUsers) {
                usersToCache.add(EntityMapper.toUserProfileCache(user));
                userMap.put(user.getId(), EntityMapper.toUserProfileLightDTO(user));
            }

            if (!usersToCache.isEmpty()) {
                cacheService.saveUsersProfile(usersToCache);
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


    // ========== LOGIN HISTORY METHODS ==========


    // Основные методы

    @Transactional(propagation = REQUIRES_NEW)
    public void saveLoginHistory(String username, CreateLoginHistoryDTO loginHistory) {
        dbUserService.updateLastLogin(username, loginHistory.getLoginAt());
        dbLoginHistoryService.saveLoginHistory(EntityMapper.toLoginHistoryEntity(loginHistory));
    }


    // ========== CHAT METHODS ==========


    // Основные методы

    @Transactional(propagation = MANDATORY)
    public void savePersonalChatAndAddMembers(CreatePersonalChatDTO chat, CreateChatMemberDTO creator, CreateChatMemberDTO opponent) {
        dbChatService.savePersonalChat(EntityMapper.toChatEntity(chat), opponent.getUserId());
        eventPublisher.publishEvent(new CacheEvent.ChatCreated(
            EntityMapper.toChatCache(chat),
            List.of(EntityMapper.toChatMemberCache(creator), EntityMapper.toChatMemberCache(opponent))
        ));
    }
    @Transactional(propagation = MANDATORY)
    public void saveGroupChatAndAddMembers(CreateGroupChatDTO chat, CreateChatMemberDTO creator, List<CreateChatMemberDTO> chatMembers) {
        // конвертируем
        List<CacheChatMember> membersWithCreator = new ArrayList<>(chatMembers.size() + 1);
        membersWithCreator.add(EntityMapper.toChatMemberCache(creator));

        Long[] membersWithoutCreatorIds = new Long[chatMembers.size()];
        for (int i = 0; i < chatMembers.size(); i++) {
            membersWithCreator.add(EntityMapper.toChatMemberCache(chatMembers.get(i)));
            membersWithoutCreatorIds[i] = chatMembers.get(i).getUserId();
        }

        dbChatService.saveGroupChat(EntityMapper.toChatEntity(chat), membersWithoutCreatorIds);
        eventPublisher.publishEvent(new CacheEvent.ChatCreated(
            EntityMapper.toChatCache(chat),
            membersWithCreator
        ));
    }

    @Transactional(propagation = MANDATORY)
    public void updateChatInfo(long chatId, String newName, String newDescription, LocalDateTime updatedAt) {
        if (dbChatService.updateChatInfo(chatId, newName, newDescription, updatedAt) > 0) {
            eventPublisher.publishEvent(new CacheEvent.ChatInvalidated(chatId));
        }
    }

    @Transactional(propagation = MANDATORY)
    public void restoreChat(long chatId, LocalDateTime updatedAt) {
        if (dbChatService.restoreChat(chatId, updatedAt) > 0) {
            eventPublisher.publishEvent(new CacheEvent.ChatInvalidated(chatId));
        }
    }

    @Transactional(propagation = MANDATORY)
    public void deleteChat(long chatId, LocalDateTime updatedAt) {
        if (dbChatService.deleteChat(chatId, updatedAt) > 0) {
            eventPublisher.publishEvent(new CacheEvent.ChatInvalidated(chatId));
        }
    }


    // Вспомогательные методы
    
    public Optional<ChatSecurityDTO> getActiveChat(long chatId) {
        Optional<CacheChat> cacheChat = cacheService.getChat(chatId);
        if (cacheChat.isPresent())
            return cacheChat.filter(CacheChat::isActive).map(EntityMapper::toChatSecurityDTO);

        Optional<ChatProfileResult> dbChat = dbChatService.getChat(chatId);
        dbChat.ifPresent(chat -> {
            cacheService.saveChat(EntityMapper.toChatCache(chat)); // восстанавливаем в кеш
        });
        return dbChat.filter(chat -> !chat.getIsDeleted()).map(EntityMapper::toChatSecurityDTO);
    }
    public Optional<ChatSecurityDTO> getPersonalChat(long userId1, long userId2) {
        // пробуем кеш
        Optional<CacheChat> cached = cacheService.getPersonalChat(userId1, userId2);
        if (cached.isPresent())
            return cached.map(EntityMapper::toChatSecurityDTO);

        // грузим из бд
        Optional<ChatProfileResult> dbChat = dbChatService.getPersonalChat(userId1, userId2);
        dbChat.ifPresent(chat -> {
            cacheService.saveChat(EntityMapper.toChatCache(chat)); // восстанавливаем в кеш
        });
        return dbChat.map(EntityMapper::toChatSecurityDTO);
    }

    public boolean isActiveChat(long chatId) {
        // пробуем кеш
        Optional<CacheChat> cacheChat = cacheService.getChat(chatId);
        if (cacheChat.isPresent())
            return cacheChat.filter(CacheChat::isActive).isPresent();

        // грузим из бд
        Optional<ChatProfileResult> dbChat = dbChatService.getChat(chatId);
        dbChat.ifPresent(chat -> {
            cacheService.saveChat(EntityMapper.toChatCache(chat)); // восстанавливаем в кеш
        });
        return dbChat.filter(chat -> !chat.getIsDeleted()).isPresent();
    }
    public Optional<Boolean> isActiveGroupChat(long chatId) {
        // пробуем кеш
        Optional<CacheChat> cacheChat = cacheService.getChat(chatId);
        if (cacheChat.isPresent())
            return cacheChat.filter(CacheChat::isActive).map(CacheChat::isNotPersonal);

        // грузим из бд
        Optional<ChatProfileResult> dbChat = dbChatService.getChat(chatId);
        dbChat.ifPresent(chat -> {
            cacheService.saveChat(EntityMapper.toChatCache(chat)); // восстанавливаем в кеш
        });
        return dbChat.filter(chat -> !chat.getIsDeleted()).map(chat -> ChatType.valueOf(chat.getChatType()).isNotPersonal());
    }
    
    public UserChatsPageDTO getUserChatsPage(long userId, Boolean isPinnedCursor, Long lastMsgIdCursor, Long chatIdCursor, int limit) {
        // загружаем с бд
        List<UserChatResult> rows = dbChatService.getUserChatsPage(userId, isPinnedCursor, lastMsgIdCursor, chatIdCursor, limit + 1); // берем на одну больше
        if (rows.isEmpty()) {
            return new UserChatsPageDTO(Collections.emptyList(), null);
        }

        boolean hasMore = rows.size() > limit;
        List<UserChatResult> pageRows = hasMore ? rows.subList(0, limit) : rows;
        Long nextCursor = hasMore ? pageRows.getLast().getId() : null;
        List<ChatProfileDTO> chats = EntityMapper.toChatProfileDTOs(pageRows, userId);

        // кешируем данные
        cacheService.saveChats(EntityMapper.toChatsCache(pageRows));
        return new UserChatsPageDTO(chats, nextCursor);
    }
    public Optional<ChatProfileDTO> getUserChat(long chatId, long userId) {
        // загружаем с бд
        Optional<UserChatResult> dbChat = dbChatService.getUserChat(chatId, userId);
        dbChat.ifPresent(chat -> {
            cacheService.saveChat(EntityMapper.toChatCache(chat)); // кешируем данные
        });
        return dbChat.map(chat -> EntityMapper.toChatProfileDTO(chat, chatId));
    }
    public List<Long> getUserChatIds(long userId) {
        return dbChatService.getUserChatIds(userId); // загружаем с бд
    }
    public ChatStatsResult getChatClearStats(long chatId, long userId) {
        return dbChatService.getChatClearStats(chatId, userId);
    }


    // ========== CHAT MEMBER METHODS ==========


    // Основные методы

    @Transactional(propagation = MANDATORY)
    public void saveOrRestoreChatMember(@NonNull CreateChatMemberDTO chatMember) {
        dbChatMemberService.upsertChatMember(EntityMapper.toChatMemberEntity(chatMember));
        eventPublisher.publishEvent(new CacheEvent.ChatMemberSaved(
            EntityMapper.toChatMemberCache(chatMember)
        ));
    }

    @Transactional(propagation = MANDATORY)
    public void saveOrRestoreChatMembers(long chatId, @NonNull List<CreateChatMemberDTO> chatMembers) {
        // конвертируем
        LocalDateTime joinedAt = chatMembers.getFirst().getJoinedAt();
        Long[] memberIds = new Long[chatMembers.size()];
        for (int i = 0; i < chatMembers.size(); i++) {
            memberIds[i] = chatMembers.get(i).getUserId();
        }

        dbChatMemberService.upsertChatMembers(chatId, memberIds, joinedAt);
        eventPublisher.publishEvent(new CacheEvent.ChatMembersSaved(
            chatId, EntityMapper.toChatMemberCaches(chatMembers)
        ));
    }

    @Transactional(propagation = MANDATORY)
    public void updateChatMemberInfo(long chatId, long userId, String tag, LocalDateTime updatedAt) {
        if (dbChatMemberService.updateChatMemberInfo(chatId, userId, tag, updatedAt) > 0) {
            eventPublisher.publishEvent(new CacheEvent.ChatMemberInvalidated(chatId, userId));
        }
    }

    @Transactional(propagation = MANDATORY)
    public void updateChatMemberAdminRights(long chatId, long userId, boolean isAdmin, LocalDateTime updatedAt) {
        if (dbChatMemberService.updateChatMemberAdminRights(chatId, userId, isAdmin, updatedAt) > 0) {
            eventPublisher.publishEvent(new CacheEvent.ChatMemberInvalidated(chatId, userId));
        }
    }

    @Transactional(propagation = MANDATORY)
    public void updateChatMemberSetting(long chatId, long userId, boolean isPinned, LocalDateTime updatedAt) {
        if (dbChatMemberService.updateChatMemberSettings(chatId, userId, isPinned, updatedAt) > 0) {
            eventPublisher.publishEvent(new CacheEvent.ChatMemberInvalidated(chatId, userId));
        }
    }

    @Transactional(propagation = MANDATORY)
    public void removeUserFromChat(long chatId, long userId, LocalDateTime updatedAt) {
        if (dbChatMemberService.removeChatMember(userId, chatId, updatedAt)) {
            eventPublisher.publishEvent(new CacheEvent.ChatMemberInvalidated(chatId, userId));
        }
    }


    // Вспомогательные методы
    
    public boolean hasActiveChatMember(long chatId, long userId) {
        // проверка в кеше
        Optional<Boolean> hasActiveChatMember = cacheService.hasActiveChatMember(chatId, userId);
        if (hasActiveChatMember.isPresent())
            return hasActiveChatMember.get();

        // проверяем пользователя в чате
        Optional<ChatMember> dbMember = dbChatMemberService.getChatMember(chatId, userId);
        dbMember.ifPresent(member -> {
            cacheService.saveChatMember(EntityMapper.toChatMemberCache(member)); // кешируем
        });
        return dbMember.map(ChatMember::isActive).orElse(false);
    }
    public Optional<Boolean> isActiveAdminInActiveChat(long chatId, long userId) {
        // пробуем кеш
        Optional<Boolean> cached = cacheService.isActiveAdminInActiveChat(chatId, userId);
        if (cached.isPresent())
            return cached;

        // надо найти пользователя, добавить в кеш и отдать
        Optional<ChatMember> dbMember = dbChatMemberService.getActiveChatMember(chatId, userId);
        dbMember.ifPresent(member -> {
            cacheService.saveChatMember(EntityMapper.toChatMemberCache(member));
        });
        return dbMember.map(ChatMember::isAdmin);
    }
    private Map<Long, ChatMemberProfileDTO> loadChatMemberProfilesWithCache(long chatId, @NonNull Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, ChatMemberProfileDTO> memberMap = new HashMap<>();

        // Загружаем из кеша
        Set<Long> missingMemberIds = new HashSet<>();
        Map<Long, CacheChatMember> cachedMembers = cacheService.getChatMembers(chatId, userIds, missingMemberIds);

        for (Map.Entry<Long, CacheChatMember> entry : cachedMembers.entrySet()) {
            CacheChatMember cachedMember = entry.getValue();
            if (!cachedMember.isDeleted()) {
                memberMap.put(entry.getKey(), EntityMapper.toChatMemberProfileDTO(cachedMember));
            }
        }

        // Загружаем недостающих из БД
        if (!missingMemberIds.isEmpty()) {
            List<ChatMember> dbMembers = dbChatMemberService.getActiveChatMembersByIds(chatId, new ArrayList<>(missingMemberIds));
            List<CacheChatMember> membersToCache = new ArrayList<>();

            for (ChatMember member : dbMembers) {
                membersToCache.add(EntityMapper.toChatMemberCache(member));
                memberMap.put(member.getUserId(), EntityMapper.toChatMemberProfileDTO(member));
            }

            if (!membersToCache.isEmpty()) {
                cacheService.saveChatMembers(chatId, membersToCache);
            }
        }

        return memberMap;
    }
    public ChatMembersPageDTO getChatMembersPage(long chatId, Long cursor, int limit) {
        List<Long> userIds = dbChatMemberService.getChatMemberIdsPage(chatId, cursor, limit + 1);
        if (userIds.isEmpty()) {
            return new ChatMembersPageDTO(Collections.emptyList(), null);
        }

        boolean hasMore = userIds.size() > limit;
        List<Long> resultUserIds = hasMore ? userIds.subList(0, limit) : userIds;
        Long nextCursor = hasMore ? resultUserIds.getLast() : null;

        // Загружаем все необходимые данные
        Map<Long, UserProfileLightDTO> userMap = loadUserProfileLightsWithCache(new HashSet<>(resultUserIds));
        Map<Long, ChatMemberProfileDTO> memberMap = loadChatMemberProfilesWithCache(chatId, new HashSet<>(resultUserIds));

        // Формируем результат
        List<ChatMemberProfileFullDTO> result = new LinkedList<>();
        for (long userId : resultUserIds) {
            UserProfileLightDTO user = userMap.get(userId);
            ChatMemberProfileDTO member = memberMap.get(userId);
            if (user == null || member == null) continue;

            result.add(EntityMapper.toChatMemberProfileFullDTO(user, member));
        }
        return new ChatMembersPageDTO(result, nextCursor);
    }
    public List<ChatMemberProfileDTO> getChatMembersByIds(long chatId, @NonNull Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, ChatMemberProfileDTO> memberMap = new HashMap<>();

        // Загружаем из кеша
        Set<Long> missingMemberIds = new HashSet<>();
        Map<Long, CacheChatMember> cachedMembers = cacheService.getChatMembers(chatId, userIds, missingMemberIds);

        for (Map.Entry<Long, CacheChatMember> entry : cachedMembers.entrySet()) {
            CacheChatMember cachedMember = entry.getValue();
            if (!cachedMember.isDeleted()) {
                memberMap.put(entry.getKey(), EntityMapper.toChatMemberProfileDTO(cachedMember));
            }
        }

        // Загружаем недостающих из БД
        if (!missingMemberIds.isEmpty()) {
            List<ChatMember> dbMembers = dbChatMemberService.getActiveChatMembersByIds(chatId, new ArrayList<>(missingMemberIds));
            List<CacheChatMember> membersToCache = new LinkedList<>();

            for (ChatMember member : dbMembers) {
                membersToCache.add(EntityMapper.toChatMemberCache(member));
                memberMap.put(member.getUserId(), EntityMapper.toChatMemberProfileDTO(member));
            }

            if (!membersToCache.isEmpty()) {
                cacheService.saveChatMembers(chatId, membersToCache);
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


    // ========== VERIFICATION TOKEN METHODS ==========


    // Основные методы

    @Transactional(propagation = REQUIRES_NEW)
    public void saveVerificationToken(@NonNull CreateVerificationTokenDTO verificationToken) {
        verificationTokenDb.saveVerificationToken(
            EntityMapper.toVerificationTokenEntity(verificationToken)
        );
        eventPublisher.publishEvent(new CacheEvent.VerificationTokenCreated(
            EntityMapper.toVerificationTokenCache(verificationToken)
        ));
    }
    @Transactional(propagation = REQUIRES_NEW)
    public void deleteVerificationToken(@NonNull String token) {
        verificationTokenDb.deleteVerificationToken(token);
        eventPublisher.publishEvent(new CacheEvent.VerificationTokenDeleted((token)));
    }


    // Вспомогательные методы
    public Optional<VerificationTokenDTO> getVerificationToken(@NonNull String token) {
        Optional<CacheVerificationToken> optToken = cacheService.getVerificationToken(token);
        if(optToken.isPresent())
            return optToken.map(EntityMapper::toVerificationTokenDTO);

        Optional<VerificationToken> optTokenDB = verificationTokenDb.getVerificationToken(token);
        optTokenDB.ifPresent(verificationTokenDB -> {
            cacheService.saveVerificationToken(EntityMapper.toVerificationTokenCache(verificationTokenDB));
        });
        return optTokenDB.map(EntityMapper::toVerificationTokenDTO);
    }


    // ========== MESSAGE METHODS ==========


    // Основные методы

    @Transactional(propagation = MANDATORY)
    public void saveMessage(CreateMessageDTO message) {
        dbMessageService.saveMessage(EntityMapper.toMessageEntity(message));
        eventPublisher.publishEvent(new CacheEvent.MessageCreated(
            EntityMapper.toMessageSecurityCache(message)
        ));
    }

    @Transactional(propagation = MANDATORY)
    public void updateMessage(long messageId, String newText, LocalDateTime updatedAt) {
        if (dbMessageService.updateMessage(messageId, newText, updatedAt) > 0) {
            eventPublisher.publishEvent(new CacheEvent.MessageInvalidated(messageId));
        }
    }

    @Transactional(propagation = MANDATORY)
    public void markMessagesUpToRead(long chatId, long userId, long messageId, LocalDateTime readAt) {
        dbMessageService.markMessagesUpToRead(chatId, userId, messageId, readAt);
    }

    @Transactional(propagation = MANDATORY)
    public void restoreMessage(long messageId, LocalDateTime updatedAt) {
        if (dbMessageService.restoreMessage(messageId, updatedAt) > 0) {
            eventPublisher.publishEvent(new CacheEvent.MessageInvalidated(messageId));
        }
    }

    @Transactional(propagation = MANDATORY)
    public void deleteMessage(long messageId, LocalDateTime updatedAt) {
        if (dbMessageService.deleteMessage(messageId, updatedAt) > 0) {
            eventPublisher.publishEvent(new CacheEvent.MessageInvalidated(messageId));
        }
    }


    // Вспомогательные методы
    public boolean isActiveMessageInChat(long chatId, long messageId) {
        // пробуем кеш
        Optional<CacheMessageSecurity> cacheMessage = cacheService.getMessage(messageId);
        if (cacheMessage.isPresent())
            return cacheMessage.filter(msg -> msg.isActive() && msg.getChatId() == chatId).isPresent();

        // грузим из бд
        Optional<Message> dbMessage = dbMessageService.getMessage(chatId, messageId);
        dbMessage.ifPresent(msg -> {
            cacheService.saveMessage(EntityMapper.toMessageSecurityCache(msg)); // восстанавливаем в кеш
        });
        return dbMessage.filter(Message::isActive).isPresent();
    }
    public boolean isActiveMessageInChatAndIsSender(long chatId, long userId, long messageId) {
        // пробуем кеш
        Optional<CacheMessageSecurity> cacheMessage = cacheService.getMessage(messageId);
        if (cacheMessage.isPresent())
            return cacheMessage.filter(msg -> msg.isActive() && msg.getChatId() == chatId && msg.getSenderId() == userId).isPresent();

        // грузим из бд
        Optional<Message> dbMessage = dbMessageService.getMessage(chatId, messageId);
        dbMessage.ifPresent(msg -> {
            cacheService.saveMessage(EntityMapper.toMessageSecurityCache(msg)); // восстанавливаем в кеш
        });
        return dbMessage.filter(Message::isActive).filter(msg -> msg.getSenderId() == userId).isPresent();
    }

    public Optional<UserMessageDTO> getActiveMessageWithReadStatusInChat(long chatId, long userId, long messageId) {
        // грузим из бд
        Optional<UserMessageResult> dbMessage = dbMessageService.getUserMessage(chatId, userId, messageId);
        dbMessage.ifPresent(msg -> {
            cacheService.saveMessage(EntityMapper.toMessageSecurityCache(msg)); // восстанавливаем в кеш
        });
        return dbMessage.map(msg -> EntityMapper.toUserMessageDTO(msg, true));
    }
    public MessagesPageDTO getChatMessagesPage(long chatId, long userId, Long cursor, int limit, Direction direction) {
        // Получаем Page сообщений из БД
        List<UserMessageResult> dbResult = dbMessageService.getUserMessagePage(chatId, userId, cursor, limit + 1, direction); // Получаем с БД
        if (dbResult.isEmpty()) {
            return new MessagesPageDTO(Collections.emptyList(), null);
        }

        // обрезаем и выясняем курсор (если требуется)
        Long nextCursor = null;
        if (dbResult.size() > limit) {
            if (direction == Direction.FORWARD) {
                dbResult = dbResult.subList(0, limit);
                nextCursor = dbResult.getLast().getId();
            } else {
                dbResult = dbResult.subList(dbResult.size() - limit, dbResult.size());
                nextCursor = dbResult.getFirst().getId();
            }
        }

        // собираем результат
        List<UserMessageDTO> messageMap = new LinkedList<>();
        List<CacheMessageSecurity> messagesToCache = new LinkedList<>();
        for (UserMessageResult message : dbResult) {
            messageMap.add(EntityMapper.toUserMessageDTO(message, message.getIsDeleted()));
            messagesToCache.add(EntityMapper.toMessageSecurityCache(message));
        }

        // кешируем
        cacheService.saveMessages(messagesToCache);
        return new MessagesPageDTO(messageMap, nextCursor);
    }
    public List<MessageReadStatusDTO> getMessageReads(long messageId){
        List<MessageReadStatusResult> reads = dbMessageService.getMessageReaders(messageId);
        return EntityMapper.toMessageReadDTOs(reads);
    }



    // ========== SUB METHODS ==========


    public CacheService.CacheStats getCacheStatus() {
        return cacheService.getCacheStatus();
    }
    @Scheduled(initialDelay = 10_000, fixedRate = 86_400_000) // Каждые 24 часа
    public void cleanupExpiredTokens() {
        try {
            int numDeletedTokens = verificationTokenDb.cleanupExpiredVerificationTokens();
            log.info("[🔧] ✅ Expired tokens cleanup completed. Deleted --> {} tokens", numDeletedTokens);
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error during token cleanup: {}", e.getMessage());
        }
    }
}