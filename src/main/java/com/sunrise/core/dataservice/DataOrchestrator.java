package com.sunrise.core.dataservice;

import com.sunrise.core.dataservice.dbresult.*;
import com.sunrise.core.dataservice.type.ChatType;
import com.sunrise.core.dataservice.type.Direction;
import com.sunrise.entity.cache.*;
import com.sunrise.entity.creation.*;
import com.sunrise.entity.db.*;
import com.sunrise.entity.dto.*;
import com.sunrise.entity.EntityMapper;

import com.sunrise.entity.pagination.ChatMembersPageDTO;
import com.sunrise.entity.pagination.MessagesPageDTO;
import com.sunrise.entity.pagination.UserChatsPageDTO;
import com.sunrise.entity.pagination.UsersPageDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class DataOrchestrator {

    private final CacheService cacheService;
    private final DBService dbService;

    public DataOrchestrator(CacheService cacheService, DBService dbService) {
        this.cacheService = cacheService;
        this.dbService = dbService;
    }

    @PostConstruct
    public void warmUpCache() {
        // TODO: подумать чо буду в при старте загружать
    }
    @PreDestroy
    public void onShutdown() {
        // TODO: подумать чо буду при завершении делать
    }


    // ========== USER METHODS ==========


    // Основные методы
    public void saveUser(CreateUserDTO user) {
        dbService.saveUser(EntityMapper.toUserEntity(user)); // синхронно в бд
        cacheService.saveUserProfile(EntityMapper.toUserProfileCache(user)); // сохраняем в кеш
        cacheService.saveUserSecurity(EntityMapper.toUserSecurityCache(user)); // сохраняем в кеш
    }
    public void updateLastLogin(String username, LocalDateTime lastLogin) {
        dbService.updateLastLoginAsync(username, lastLogin); // асинхронно в бд
    }
    public void updateUserProfile(long userId, String oldUsername, String username, String name, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.updateUserProfile(userId, username, name, updatedAt) > 0; // синхронно в БД
        if (isUpdated) cacheService.invalidateUserProfileAndUsernameIndex(userId, oldUsername); // обновляем в кеше
    }
    public void updateUserEmail(long userId, String oldEmail, String email, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.updateUserEmail(userId, email, updatedAt) > 0;
        if (isUpdated) cacheService.invalidateUserSecurityAndEmailIndex(userId, oldEmail);
    }
    public void updateUserPassword(long userId, String password, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.updateUserPassword(userId, password, updatedAt) > 0;
        if (isUpdated) cacheService.invalidateUserSecurity(userId);
    }
    public void enableUser(long userId, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.enableUser(userId, updatedAt) > 0; // синхронно в бд
        if (isUpdated) cacheService.invalidateUserSecurity(userId); // сохраняем в кеш
    }
    public void deleteUser(long userId, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.deleteUser(userId, updatedAt) > 0; // синхронно в бд
        if (isUpdated) {
            cacheService.invalidateUserProfile(userId);
            cacheService.invalidateUserSecurity(userId); // сохраняем в кеш
        }
    }
    public void restoreUser(long userId, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.restoreUser(userId, updatedAt) > 0; // синхронно в бд
        if (isUpdated) {
            cacheService.invalidateUserProfile(userId);
            cacheService.invalidateUserSecurity(userId); // сохраняем в кеш
        }
    }


    // Вспомогательные методы
    public boolean existsUserByUsername(String username) {
        // проверяем в кеше
        if (cacheService.existsUserByUsername(username))
            return true;

        // проверяем в бд
        Optional<UserSecurityResult> dbUser = dbService.getUserSecurityByUsername(username);
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
        Optional<UserSecurityResult> dbUser = dbService.getUserSecurityByEmail(email);
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
        Optional<UserSecurityResult> dbUser = dbService.getUserSecurity(userId);
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
        Optional<UserSecurityResult> dbUser = dbService.getUserSecurity(userId);
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
        Optional<UserSecurityResult> dbUser = dbService.getUserSecurity(userId);
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
        Optional<UserSecurityResult> dbUser = dbService.getUserSecurityByUsername(username);
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
        Optional<UserSecurityResult> dbUser = dbService.getUserSecurityByEmail(email);
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
        Optional<UserProfileResult> dbUser = dbService.getUserProfile(userId);
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
        Optional<UserProfileResult> dbUser = dbService.getUserProfile(userId);
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
        Optional<UserSecurityResult> dbUser = dbService.getUserSecurity(userId);
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
            List<UserProfileResult> dbUsers = dbService.getActiveUserProfileByIds(new ArrayList<>(missingUserIds));
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
        List<UserProfileResult> rows = dbService.getActiveUsersPage(filter, cursor, limit + 1); // берем на одну больше

        boolean hasMore = rows.size() > limit;
        List<UserProfileResult> pageRows = hasMore ? rows.subList(0, limit) : rows;
        Map<Long, UserProfileLightDTO> users = EntityMapper.toDtoUserProfiles(pageRows);
        Long  nextCursor = hasMore ? pageRows.getLast().getId() : null;

        return new UsersPageDTO(users, nextCursor);
    }


    // ========== LOGIN HISTORY METHODS ==========


    // Основные методы
    public void saveLoginHistory(CreateLoginHistoryDTO loginHistory) {
        dbService.saveLoginHistoryAsync(EntityMapper.toLoginHistoryEntity(loginHistory)); // асинхронно в бд
    } // TODO: SYNC OUTBOX||KAFKA


    // ========== CHAT METHODS ==========


    // Основные методы
    public void savePersonalChatAndAddMembers(CreatePersonalChatDTO chat, CreateChatMemberDTO creator, CreateChatMemberDTO opponent) {
        // синхронно в бд
        dbService.savePersonalChat(EntityMapper.toChatEntity(chat), opponent.getUserId());

        // сохраняем в кеш
        cacheService.saveChatAndAddMembers(
            EntityMapper.toChatCache(chat),
            List.of(EntityMapper.toChatMemberCache(creator), EntityMapper.toChatMemberCache(opponent))
        );
    }
    public void saveGroupChatAndAddMembers(CreateGroupChatDTO chat, CreateChatMemberDTO creator, List<CreateChatMemberDTO> chatMembers) {
        // конвертируем
        List<CacheChatMember> membersWithCreator = new ArrayList<>(chatMembers.size() + 1);
        membersWithCreator.add(EntityMapper.toChatMemberCache(creator));

        Long[] membersWithoutCreatorIds = new Long[chatMembers.size()];
        for (int i = 0; i < chatMembers.size(); i++) {
            membersWithCreator.add(EntityMapper.toChatMemberCache(chatMembers.get(i)));
            membersWithoutCreatorIds[i] = chatMembers.get(i).getUserId();
        }

        // синхронно в бд
        dbService.saveGroupChat(EntityMapper.toChatEntity(chat), membersWithoutCreatorIds);

        // сохраняем в кеш
        cacheService.saveChatAndAddMembers(
            EntityMapper.toChatCache(chat), membersWithCreator
        );
    }
    public void updateChatInfo(long chatId, String newName, String newDescription, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.updateChatInfo(chatId, newName, newDescription, updatedAt) > 0; // синхронно в бд
        if (isUpdated) cacheService.invalidateChat(chatId); // сохраняем в кеш
    }
    public void restoreChat(long chatId, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.restoreChat(chatId, updatedAt) > 0; // синхронно в бд
        if (isUpdated) cacheService.invalidateChat(chatId); // сохраняем в кеш
    }
    public void deleteChat(long chatId, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.deleteChat(chatId, updatedAt) > 0; // синхронно в бд
        if (isUpdated) cacheService.invalidateChat(chatId); // сохраняем в кеш
    }


    // Вспомогательные методы
    public Optional<ChatSecurityDTO> getActiveChat(long chatId) {
        Optional<CacheChat> cacheChat = cacheService.getChat(chatId);
        if (cacheChat.isPresent())
            return cacheChat.filter(CacheChat::isActive).map(EntityMapper::toChatSecurityDTO);

        Optional<ChatProfileResult> dbChat = dbService.getChat(chatId);
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
        Optional<ChatProfileResult> dbChat = dbService.getPersonalChat(userId1, userId2);
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
        Optional<ChatProfileResult> dbChat = dbService.getChat(chatId);
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
        Optional<ChatProfileResult> dbChat = dbService.getChat(chatId);
        dbChat.ifPresent(chat -> {
            cacheService.saveChat(EntityMapper.toChatCache(chat)); // восстанавливаем в кеш
        });
        return dbChat.filter(chat -> !chat.getIsDeleted()).map(chat -> ChatType.valueOf(chat.getChatType()).isNotPersonal());
    }

    public UserChatsPageDTO getUserChatsPage(long userId, Boolean isPinnedCursor, Long lastMsgIdCursor, Long chatIdCursor, int limit) {
        // загружаем с бд
        List<UserChatResult> rows = dbService.getUserChatsPage(userId, isPinnedCursor, lastMsgIdCursor, chatIdCursor, limit + 1); // берем на одну больше
        if (rows.isEmpty()) {
            return new UserChatsPageDTO(Collections.emptyMap(), null);
        }

        boolean hasMore = rows.size() > limit;
        List<UserChatResult> pageRows = hasMore ? rows.subList(0, limit) : rows;
        Long nextCursor = hasMore ? pageRows.getLast().getId() : null;
        Map<Long, ChatProfileDTO> chats = EntityMapper.toChatProfileDTOs(pageRows);

        // кешируем данные
        cacheService.saveChats(EntityMapper.toChatsCache(pageRows));
        return new UserChatsPageDTO(chats, nextCursor);
    }
    public Optional<ChatProfileDTO> getUserChat(long chatId, long userId) {
        // загружаем с бд
        Optional<UserChatResult> dbChat = dbService.getUserChat(chatId, userId);
        dbChat.ifPresent(chat -> {
            cacheService.saveChat(EntityMapper.toChatCache(chat)); // кешируем данные
        });
        return dbChat.map(EntityMapper::toChatProfileDTO);
    }
    public List<Long> getUserChatIds(long userId) {
        return dbService.getUserChatIds(userId); // загружаем с бд
    }


    // ========== CHAT MEMBER METHODS ==========


    // Основные методы
    public void saveOrRestoreChatMember(CreateChatMemberDTO chatMember) {
        dbService.upsertChatMember(EntityMapper.toChatMemberEntity(chatMember)); // синхронно в бд
        cacheService.saveChatMember(EntityMapper.toChatMemberCache(chatMember)); // сохраняем в кеш
    }
    public void saveOrRestoreChatMembers(long chatId, List<CreateChatMemberDTO> chatMembers) {
        // конвертируем
        LocalDateTime joinedAt = chatMembers.getFirst().getJoinedAt();
        Long[] memberIds = new Long[chatMembers.size()];
        for (int i = 0; i < chatMembers.size(); i++) {
            memberIds[i] = chatMembers.get(i).getUserId();
        }

        dbService.upsertChatMembers(chatId, memberIds, joinedAt); // синхронно в бд
        cacheService.saveChatMembers(chatId, EntityMapper.toChatMemberCaches(chatMembers)); // сохраняем в кеш
    }
    public void updateChatMemberInfo(long chatId, long userId, String tag, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.updateChatMemberInfo(chatId, userId, tag, updatedAt) > 0; // синхронно в бд
        if (isUpdated) cacheService.invalidateChatMember(chatId, userId); // обновляем кэш
    }
    public void updateChatMemberAdminRights(long chatId, long userId, boolean isAdmin, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.updateChatMemberAdminRights(chatId, userId, isAdmin, updatedAt) > 0; // синхронно в бд
        if (isUpdated) cacheService.invalidateChatMember(chatId, userId); // обновляем кэш
    }
    public void updateChatMemberSetting(long chatId, long userId, boolean isPinned, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.updateChatMemberSettings(chatId, userId, isPinned, updatedAt) > 0; // синхронно в бд
        if (isUpdated) cacheService.invalidateChatMember(chatId, userId); // обновляем кэш
    }
    public void removeUserFromChat(long chatId, long userId, LocalDateTime updatedAt) {
        boolean removed = dbService.removeChatMember(userId, chatId, updatedAt); // синхронно в бд
        if (removed) cacheService.invalidateChatMember(chatId, userId); // сохраняем в кеш
    }


    // Вспомогательные методы
    public boolean hasActiveChatMember(long chatId, long userId) {
        // проверка в кеше
        Optional<Boolean> hasActiveChatMember = cacheService.hasActiveChatMember(chatId, userId);
        if (hasActiveChatMember.isPresent())
            return hasActiveChatMember.get();

        // проверяем пользователя в чате
        Optional<ChatMember> dbMember = dbService.getChatMember(chatId, userId);
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
        Optional<ChatMember> dbMember = dbService.getActiveChatMember(chatId, userId);
        dbMember.ifPresent(member -> {
            cacheService.saveChatMember(EntityMapper.toChatMemberCache(member));
        });
        return dbMember.map(ChatMember::isAdmin);
    }

    private Map<Long, ChatMemberProfileDTO> loadChatMemberProfilesWithCache(long chatId, Set<Long> userIds) {
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
            List<ChatMember> dbMembers = dbService.getActiveChatMembersByIds(chatId, new ArrayList<>(missingMemberIds));
            List<CacheChatMember> membersToCache = new ArrayList<>();

            for (ChatMember member : dbMembers) {
                CacheChatMember cacheMember = EntityMapper.toChatMemberCache(member);
                membersToCache.add(cacheMember);
                memberMap.put(member.getUserId(), EntityMapper.toChatMemberProfileDTO(cacheMember));
            }

            if (!membersToCache.isEmpty()) {
                cacheService.saveChatMembers(chatId, membersToCache);
            }
        }

        return memberMap;
    }
    public ChatMembersPageDTO getChatMembersPage(long chatId, Long cursor, int limit) {
        List<Long> userIds = dbService.getChatMemberIdsPage(chatId, cursor, limit + 1);
        if (userIds.isEmpty()) {
            return new ChatMembersPageDTO(Collections.emptyMap(), null);
        }

        boolean hasMore = userIds.size() > limit;
        List<Long> resultUserIds = hasMore ? userIds.subList(0, limit) : userIds;
        Long nextCursor = hasMore ? resultUserIds.getLast() : null;

        // Загружаем все необходимые данные
        Map<Long, UserProfileLightDTO> userMap = loadUserProfileLightsWithCache(new HashSet<>(resultUserIds));
        Map<Long, ChatMemberProfileDTO> memberMap = loadChatMemberProfilesWithCache(chatId, new HashSet<>(resultUserIds));

        // Формируем результат
        Map<Long, ChatMemberProfileFullDTO> result = new LinkedHashMap<>();
        for (long userId : resultUserIds) {
            UserProfileLightDTO user = userMap.get(userId);
            ChatMemberProfileDTO member = memberMap.get(userId);
            if (user == null || member == null) continue;

            result.put(userId, EntityMapper.toChatMemberProfileFullDTO(user, member));
        }
        return new ChatMembersPageDTO(result, nextCursor);
    }
    public Map<Long, ChatMemberProfileFullDTO> getChatMemberByIds(long chatId, Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // Загружаем все необходимые данные
        Map<Long, UserProfileLightDTO> userMap = loadUserProfileLightsWithCache(new HashSet<>(userIds));
        Map<Long, ChatMemberProfileDTO> memberMap = loadChatMemberProfilesWithCache(chatId, new HashSet<>(userIds));

        // Формируем результат
        Map<Long, ChatMemberProfileFullDTO> result = new LinkedHashMap<>();
        for (long userId : userIds) {
            UserProfileLightDTO user = userMap.get(userId);
            ChatMemberProfileDTO member = memberMap.get(userId);
            if(user == null || member == null) continue;

            result.put(userId, EntityMapper.toChatMemberProfileFullDTO(user, member));
        }
        return result;
    }


    // ========== VERIFICATION TOKEN METHODS ==========


    // Основные методы
    public void saveVerificationToken(CreateVerificationTokenDTO verificationToken) {
        dbService.saveVerificationTokenAsync(EntityMapper.toVerificationTokenEntity(verificationToken)); // асинхронно в бд
        cacheService.saveVerificationToken(EntityMapper.toVerificationTokenCache(verificationToken)); // сохраняем в кеш
    } // TODO: SYNC OUTBOX||KAFKA
    public void deleteVerificationToken(String token) {
        dbService.deleteVerificationTokenAsync(token); // асинхронно в бд
        cacheService.invalidateVerificationToken(token); // сохраняем в кеш
    } // TODO: SYNC OUTBOX||KAFKA


    // Вспомогательные методы
    public Optional<VerificationTokenDTO> getVerificationToken(String token) {
        Optional<CacheVerificationToken> optToken = cacheService.getVerificationToken(token);
        if(optToken.isPresent())
            return optToken.map(EntityMapper::toVerificationTokenDTO);

        Optional<VerificationToken> optTokenDB = dbService.getVerificationToken(token);
        optTokenDB.ifPresent(verificationTokenDB -> {
            cacheService.saveVerificationToken(EntityMapper.toVerificationTokenCache(verificationTokenDB));
        });
        return optTokenDB.map(EntityMapper::toVerificationTokenDTO);
    }


    // ========== MESSAGE METHODS ==========


    // Основные методы
    public void saveMessage(CreateMessageDTO message) {
        dbService.saveMessage(EntityMapper.toMessageEntity(message)); // синхронно в бд
        cacheService.saveMessage(EntityMapper.toMessageSecurityCache(message)); // сохраняем в кеш
    }
    public void updateMessage(long messageId, String newText, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.updateMessage(messageId, newText, updatedAt) > 0; // синхронно в бд
        if (isUpdated) cacheService.invalidateMessage(messageId); // сохраняем в кеш
    }
    public void markMessagesUpToRead(long chatId, long userId, long messageId, LocalDateTime readAt) {
        dbService.markMessagesUpToRead(chatId, userId, messageId, readAt); // синхронно в бд
    }
    public void restoreMessage(long messageId, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.restoreMessage(messageId, updatedAt) > 0; // синхронно в бд
        if (isUpdated) cacheService.invalidateMessage(messageId); // сохраняем в кеш
    }
    public void deleteMessage(long messageId, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.deleteMessage(messageId, updatedAt) > 0; // синхронно в бд
        if (isUpdated) cacheService.invalidateMessage(messageId); // сохраняем в кеш
    }


    // Вспомогательные методы
    public boolean isActiveMessageInChat(long chatId, long messageId) {
        // пробуем кеш
        Optional<CacheMessageSecurity> cacheMessage = cacheService.getMessage(messageId);
        if (cacheMessage.isPresent())
            return cacheMessage.filter(msg -> msg.isActive() && msg.getChatId() == chatId).isPresent();

        // грузим из бд
        Optional<Message> dbMessage = dbService.getMessage(chatId, messageId);
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
        Optional<Message> dbMessage = dbService.getMessage(chatId, messageId);
        dbMessage.ifPresent(msg -> {
            cacheService.saveMessage(EntityMapper.toMessageSecurityCache(msg)); // восстанавливаем в кеш
        });
        return dbMessage.filter(Message::isActive).filter(msg -> msg.getSenderId() == userId).isPresent();
    }

    public Optional<UserMessageDTO> getActiveMessageWithReadStatusInChat(long chatId, long userId, long messageId) {
        // грузим из бд
        Optional<UserMessageResult> dbMessage = dbService.getUserMessage(chatId, userId, messageId);
        dbMessage.ifPresent(msg -> {
            cacheService.saveMessage(EntityMapper.toMessageSecurityCache(msg)); // восстанавливаем в кеш
        });
        return dbMessage.map(msg -> EntityMapper.toUserMessageDTO(msg, true));
    }
    public MessagesPageDTO getChatMessagesPage(long chatId, long userId, Long cursor, int limit, Direction direction) {
        // Получаем Page сообщений из БД
        List<UserMessageResult> dbResult = dbService.getUserMessagePage(chatId, userId, cursor, limit + 1, direction); // Получаем с БД
        if (dbResult.isEmpty()) {
            return new MessagesPageDTO(Collections.emptyMap(), null);
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
        Map<Long, UserMessageDTO> messageMap = new LinkedHashMap<>(dbResult.size());
        List<CacheMessageSecurity> messagesToCache = new ArrayList<>(dbResult.size());
        for (UserMessageResult message : dbResult) {
            messageMap.put(message.getId(), EntityMapper.toUserMessageDTO(message, message.getIsDeleted()));
            messagesToCache.add(EntityMapper.toMessageSecurityCache(message));
        }

        // кешируем
        cacheService.saveMessages(messagesToCache);
        return new MessagesPageDTO(messageMap, nextCursor);
    }
    public Map<Long, MessageReadStatusDTO> getMessageReads(long messageId){
        List<MessageReadStatusResult> reads = dbService.getMessageReaders(messageId);
        return EntityMapper.toMessageReadDTOs(reads);
    }

    public ChatStatsResult getChatClearStats(long chatId, long userId) {
        return dbService.getChatMessagesDeletedStats(chatId, userId);
    }



    // ========== SUB METHODS ==========


    public CacheService.CacheStats getCacheStatus() {
        return cacheService.getCacheStatus();
    }
    @Scheduled(initialDelay = 10_000, fixedRate = 86_400_000) // Каждые 24 часа
    public void cleanupExpiredTokens() {
        try {
            int numDeletedTokens = dbService.cleanupExpiredVerificationTokens();
            log.info("[🔧] ✅ Expired tokens cleanup completed. Deleted --> {} tokens", numDeletedTokens);
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error during token cleanup: {}", e.getMessage());
        }
    }
}