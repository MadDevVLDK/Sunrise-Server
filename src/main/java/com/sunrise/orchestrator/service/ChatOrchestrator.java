package com.sunrise.orchestrator.service;

import static org.springframework.transaction.annotation.Propagation.MANDATORY;

import com.sunrise.cache.entity.*;
import com.sunrise.cache.event.CacheEvent;
import com.sunrise.cache.service.ChatCacheService;
import com.sunrise.core.creation.CreateDto;
import com.sunrise.db.entity.ChatEventDb;
import com.sunrise.db.result.*;
import com.sunrise.db.service.ChatDbService;
import com.sunrise.db.service.ChatEventDbService;
import com.sunrise.helpclass.mapper.ChatEventMapper;
import com.sunrise.helpclass.mapper.ChatMapper;
import com.sunrise.helpclass.mapper.ChatMemberMapper;
import com.sunrise.orchestrator.result.*;
import com.sunrise.orchestrator.type.ChatEventType;
import com.sunrise.orchestrator.type.ChatType;

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
    public Optional<Long> savePersonalChatAndAddMembers(CreateDto.PersonalChat chat, 
                                              CreateDto.ChatMember creator, CreateDto.ChatMember opponent) {
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
        long seq = chatEventDbService.saveAndReturnSeq(
            ChatEventMapper.toEntity(event, ChatEventType.CHAT_CREATED_WITH_MEMBERS)
        );

        // публикуем для обновления кеша после коммита
        eventPublisher.publishEvent(new CacheEvent.ChatWithMembersCreated(
            ChatMapper.toCache(chat),
            List.of(ChatMemberMapper.toCache(creator), ChatMemberMapper.toCache(opponent))
        ));
        return Optional.of(seq);
    }

    @Transactional(propagation = MANDATORY)
    public Optional<Long> updateChatProfile(long chatId, String newName, String newDescription, Instant updatedAt) {
        // синхронно в бд
        int updated = dbChatService.updateProfile(chatId, newName, newDescription, updatedAt);
        if (updated > 0) {
            var event = new ChatEvent.ChatUpdated(
                chatId, newName, newDescription, updatedAt
            );
            long seq = chatEventDbService.saveAndReturnSeq(
                ChatEventMapper.toEntity(event, ChatEventType.CHAT_UPDATED)
            );

            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.ChatInvalidated(chatId));
            return Optional.of(seq);
        }
        return Optional.empty();
    }

    @Transactional(propagation = MANDATORY)
    public Optional<Long> delete(long chatId, Instant updatedAt) {
        // синхронно в бд
        int updated = dbChatService.delete(chatId, updatedAt);
        if (updated > 0) {
            var event = new ChatEvent.ChatDeleted(chatId, updatedAt);
            long seq = chatEventDbService.saveAndReturnSeq(
                ChatEventMapper.toEntity(event, ChatEventType.CHAT_DELETED)
            );

            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.ChatInvalidated(chatId));
            return Optional.of(seq);
        }
        return Optional.empty();
    }
    
    
    // Вспомогательные методы
    
    public Optional<Dto.ChatSecurity> getActive(long chatId) {
        // пробуем кеш
        Optional<Cache.Chat> cacheChat = cacheChatService.get(chatId);
        if (cacheChat.isPresent())
            return cacheChat.filter(Cache.Chat::isActive).map(ChatMapper::toSecurityDTO);

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

    public Optional<Dto.ChatSecurity> getPersonalChat(long userId1, long userId2) {
        // пробуем кеш
        Optional<Cache.Chat> cached = cacheChatService.getPersonalChat(userId1, userId2);
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
        Optional<Cache.Chat> cacheChat = cacheChatService.get(chatId);
        if (cacheChat.isPresent())
            return cacheChat.filter(Cache.Chat::isActive).isPresent();

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
        Optional<Cache.Chat> cacheChat = cacheChatService.get(chatId);
        if (cacheChat.isPresent())
            return cacheChat.filter(Cache.Chat::isActive).map(Cache.Chat::isNotPersonal);

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

    public Optional<Dto.ChatProfile> getUserChat(long chatId, long userId) {
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

    public List<Dto.ChatProfile> getUserChatsByIds(long userId, Long[] chatsIds) {
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

    public List<Dto.ChatMeta> getChatsMeta(long userId) {
        List<ChatMetaResult> rows = dbChatService.getChatsMeta(userId);
        return rows.stream().map(ChatMapper::toMetaDTO).toList();
    }

    public Map<Long, Dto.GlobalChatSync> getSyncChats(Map<Long, Long> chatSeqIds) {
        Map<Long, Dto.GlobalChatSync> result = new HashMap<>();
        for (Map.Entry<Long, Long> entry : chatSeqIds.entrySet()) {
            long chatId = entry.getKey();
            List<ChatEventDb> events = 
                chatEventDbService.getEventsAfter(chatId, entry.getValue(), 101); // на 1 больше
            
            List<Dto.GlobalChatEvent> clientEvents = events.stream()
                .map(dbEvent -> new Dto.GlobalChatEvent(
                    dbEvent.getSeq(), dbEvent.getEventType(), ChatEventMapper.toDomain(dbEvent)
                )).toList();

            Dto.GlobalChatSync chatEvents = new Dto.GlobalChatSync(clientEvents, events.size() > 100);
            result.put(chatId, chatEvents);
        }
        return result;
    }

    public ChatStatsResult getChatClearStats(long chatId, long userId) {
        // загружаем с бд
        return dbChatService.getChatClearStats(chatId, userId);
    }
}