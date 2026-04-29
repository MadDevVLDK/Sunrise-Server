package com.sunrise.orchestrator.service;

import static org.springframework.transaction.annotation.Propagation.MANDATORY;

import com.sunrise.cache.CacheEvent;
import com.sunrise.cache.entity.*;
import com.sunrise.cache.service.ChatCacheService;
import com.sunrise.core.creation.CreateChatMemberDTO;
import com.sunrise.core.creation.CreateGroupChatDTO;
import com.sunrise.core.creation.CreatePersonalChatDTO;
import com.sunrise.db.entity.ChatEventDb;
import com.sunrise.db.event.ChatEvent;
import com.sunrise.db.result.*;
import com.sunrise.db.service.ChatDbService;
import com.sunrise.db.service.ChatEventDbService;
import com.sunrise.helpclass.mapper.ChatEventMapper;
import com.sunrise.helpclass.mapper.ChatMapper;
import com.sunrise.helpclass.mapper.ChatMemberMapper;
import com.sunrise.orchestrator.result.*;
import com.sunrise.orchestrator.type.ChatType;
import com.sunrise.web.api.ChatController.ChatEventDTO;
import com.sunrise.web.api.ChatController.ChatSyncDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatOrchestrator {

    private final ApplicationEventPublisher eventPublisher;
    private final ChatCacheService cacheChatService;
    private final ChatDbService dbChatService;

    private final ChatEventDbService chatEventDbService;

    // ========== CHAT METHODS ==========


    // Основные методы

    @Transactional(propagation = MANDATORY)
    public void savePersonalChatAndAddMembers(CreatePersonalChatDTO chat, CreateChatMemberDTO creator, CreateChatMemberDTO opponent) {
        // синхронно в бд
        dbChatService.savePersonalChat(
            ChatMapper.toEntity(chat), opponent.getUserId()
        );


        var members = List.of(
            new ChatEvent.ChatCreatedWithMembers.MemberInfo(creator.getUserId(), creator.isAdmin(), creator.getJoinedAt()),
            new ChatEvent.ChatCreatedWithMembers.MemberInfo(opponent.getUserId(), opponent.isAdmin(), opponent.getJoinedAt())
        );
        var event = new ChatEvent.ChatCreatedWithMembers(
            chat.getId(), chat.getName(), 
            chat.getDescription(),
            chat.getChatType().name(),
            chat.getOpponentId(), chat.getMembersCount(),
            chat.getCreatedBy(), members, chat.getCreatedAt()
        );
        chatEventDbService.save(ChatEventMapper.toEntity(event));


        // публикуем для обновления кеша после коммита
        eventPublisher.publishEvent(new CacheEvent.ChatWithMembersCreated(
            ChatMapper.toCache(chat),
            List.of(ChatMemberMapper.toCache(creator), ChatMemberMapper.toCache(opponent))
        ));
    }

    @Transactional(propagation = MANDATORY)
    public void saveGroupChatAndAddMembers(CreateGroupChatDTO chat, CreateChatMemberDTO creator, List<CreateChatMemberDTO> chatMembers) {
        // конвертируем
        List<CacheChatMember> membersWithCreator = new ArrayList<>(chatMembers.size() + 1);
        membersWithCreator.add(ChatMemberMapper.toCache(creator));

        Long[] membersWithoutCreatorIds = new Long[chatMembers.size()];
        for (int i = 0; i < chatMembers.size(); i++) {
            membersWithCreator.add(ChatMemberMapper.toCache(chatMembers.get(i)));
            membersWithoutCreatorIds[i] = chatMembers.get(i).getUserId();
        }

        // синхронно в бд
        dbChatService.saveGroupChat(ChatMapper.toEntity(chat), membersWithoutCreatorIds);


        List<ChatEvent.ChatCreatedWithMembers.MemberInfo> allMembers = new ArrayList<>();
        allMembers.add(new ChatEvent.ChatCreatedWithMembers.MemberInfo(
            creator.getUserId(), creator.isAdmin(), creator.getJoinedAt()
        ));
        for (CreateChatMemberDTO m : chatMembers) {
            allMembers.add(new ChatEvent.ChatCreatedWithMembers.MemberInfo(
                m.getUserId(), m.isAdmin(), m.getJoinedAt()
            ));
        }

        var event = new ChatEvent.ChatCreatedWithMembers(
            chat.getId(), chat.getName(), 
            chat.getDescription(),
            chat.getChatType().name(),
            chat.getOpponentId(), chat.getMembersCount(),
            chat.getCreatedBy(), allMembers, chat.getCreatedAt()
        );
        chatEventDbService.save(ChatEventMapper.toEntity(event));



        // публикуем для обновления кеша после коммита
        eventPublisher.publishEvent(new CacheEvent.ChatWithMembersCreated(
            ChatMapper.toCache(chat),
            membersWithCreator
        ));
    }

    @Transactional(propagation = MANDATORY)
    public boolean updateChatProfile(long chatId, String newName, String newDescription, Instant updatedAt) {
        // синхронно в бд
        int updated = dbChatService.updateProfile(chatId, newName, newDescription, updatedAt);
        if (updated > 0) {
            var event = new ChatEvent.ChatUpdated(
                chatId, newName, newDescription, updatedAt
            );
            chatEventDbService.save(ChatEventMapper.toEntity(event));

            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.ChatInvalidated(chatId));
        }
        return updated > 0;
    }

    @Transactional(propagation = MANDATORY)
    public boolean restore(long chatId, Instant updatedAt) {
        // синхронно в бд
        int updated = dbChatService.restore(chatId, updatedAt);
        if (updated > 0) {
            var event = new ChatEvent.ChatRestored(chatId, updatedAt);
            chatEventDbService.save(ChatEventMapper.toEntity(event));

            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.ChatInvalidated(chatId));
        }
        return updated > 0;
    }

    @Transactional(propagation = MANDATORY)
    public boolean delete(long chatId, Instant updatedAt) {
        // синхронно в бд
        int updated = dbChatService.delete(chatId, updatedAt);
        if (updated > 0) {
            var event = new ChatEvent.ChatDeleted(chatId, updatedAt);
            chatEventDbService.save(ChatEventMapper.toEntity(event));

            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.ChatInvalidated(chatId));
        }
        return updated > 0;
    }
    
    
    // Вспомогательные методы
    
    public Optional<ChatSecurityDTO> getActive(long chatId) {
        // пробуем кеш
        Optional<CacheChat> cacheChat = cacheChatService.get(chatId);
        if (cacheChat.isPresent())
            return cacheChat.filter(CacheChat::isActive).map(ChatMapper::toSecurityDTO);

        // грузим из бд
        Optional<ChatProfileResult> dbChat = dbChatService.get(chatId);
        dbChat.ifPresent(chat -> {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.ChatSave(
                ChatMapper.toCache(chat)
            ));
        });
        return dbChat.filter(chat -> !chat.getIsDeleted()).map(ChatMapper::toSecurityDTO);
    }

    public Optional<ChatSecurityDTO> getPersonalChat(long userId1, long userId2) {
        // пробуем кеш
        Optional<CacheChat> cached = cacheChatService.getPersonalChat(userId1, userId2);
        if (cached.isPresent())
            return cached.map(ChatMapper::toSecurityDTO);

        // грузим из бд
        Optional<ChatProfileResult> dbChat = dbChatService.getPersonalChat(userId1, userId2);
        dbChat.ifPresent(chat -> {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.ChatSave(
                ChatMapper.toCache(chat)
            ));
        });
        return dbChat.map(ChatMapper::toSecurityDTO);
    }

    public boolean isActive(long chatId) {
        // пробуем кеш
        Optional<CacheChat> cacheChat = cacheChatService.get(chatId);
        if (cacheChat.isPresent())
            return cacheChat.filter(CacheChat::isActive).isPresent();

        // грузим из бд
        Optional<ChatProfileResult> dbChat = dbChatService.get(chatId);
        dbChat.ifPresent(chat -> {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.ChatSave(
                ChatMapper.toCache(chat)
            ));
        });
        return dbChat.filter(chat -> !chat.getIsDeleted()).isPresent();
    }

    public Optional<Boolean> isActiveGroupChat(long chatId) {
        // пробуем кеш
        Optional<CacheChat> cacheChat = cacheChatService.get(chatId);
        if (cacheChat.isPresent())
            return cacheChat.filter(CacheChat::isActive).map(CacheChat::isNotPersonal);

        // грузим из бд
        Optional<ChatProfileResult> dbChat = dbChatService.get(chatId);
        dbChat.ifPresent(chat -> {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.ChatSave(
                ChatMapper.toCache(chat)
            ));
        });
        return dbChat.filter(chat -> !chat.getIsDeleted()).map(chat -> ChatType.valueOf(chat.getChatType()).isNotPersonal());
    }

    public Optional<ChatProfileDTO> getUserChat(long chatId, long userId) {
        // загружаем с бд
        Optional<UserChatResult> dbChat = dbChatService.getUserChat(chatId, userId);
        dbChat.ifPresent(chat -> {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.ChatSave(
                ChatMapper.toCache(chat)
            ));
        });
        return dbChat.map(chat -> ChatMapper.toProfileDTO(chat, chatId));
    }

    public List<ChatProfileDTO> getUserChatsByIds(long userId, Long[] chatsIds) {
        // загружаем с бд
        List<UserChatResult> dbChats = dbChatService.getChatsByIds(userId, chatsIds);
        if (!dbChats.isEmpty()){
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.ChatsSave(
                ChatMapper.toCaches(dbChats)
            ));
        }
        return ChatMapper.toProfileDTOs(dbChats, userId);
    }

    public List<ChatMetaDTO> getChatsMeta(long userId) {
        List<ChatMetaResult> rows = dbChatService.getChatsMeta(userId);
        return rows.stream().map(ChatMapper::toMetaDTO).toList();
    }

    public Map<Long, ChatSyncDTO> getSyncChats(Map<Long, Long> chatSeqIds) {
        Map<Long, ChatSyncDTO> result = new HashMap<>();
        for (Map.Entry<Long, Long> entry : chatSeqIds.entrySet()) {
            long chatId = entry.getKey();
            List<ChatEventDb> events = 
                chatEventDbService.getEventsAfter(chatId, entry.getValue(), 101); // на 1 больше
            
            List<ChatEventDTO> clientEvents = events.stream()
                .map(dbEvent -> new ChatEventDTO(dbEvent.getSeq(), dbEvent.getEventType(), ChatEventMapper.toDomain(dbEvent))).toList();

            var chatEvents = new ChatSyncDTO(clientEvents, events.size() > 100);
            result.put(chatId, chatEvents);
        }
        return result;
    }

    public ChatStatsResult getChatClearStats(long chatId, long userId) {
        // загружаем с бд
        return dbChatService.getChatClearStats(chatId, userId);
    }
}