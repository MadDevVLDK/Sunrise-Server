package com.sunrise.orchestrator.service;

import static org.springframework.transaction.annotation.Propagation.MANDATORY;

import com.sunrise.cache.entity.*;
import com.sunrise.cache.event.CacheEvent;
import com.sunrise.cache.service.ChatCacheService;
import com.sunrise.core.creation.CreateDto;
import com.sunrise.db.result.*;
import com.sunrise.db.service.ChatDbService;
import com.sunrise.db.service.ChatMemberDbService;
import com.sunrise.db.service.EventDbService;
import com.sunrise.db.service.EventDbService.ChatEvent;
import com.sunrise.db.service.EventDbService.ChatUsersEvents;
import com.sunrise.db.service.EventDbService.UserEvent;
import com.sunrise.helpclass.mapper.ChatMapper;
import com.sunrise.helpclass.mapper.ChatMemberMapper;
import com.sunrise.orchestrator.event.EventType;
import com.sunrise.orchestrator.event.IDomainEvent;
import com.sunrise.orchestrator.result.*;
import com.sunrise.orchestrator.result.Dto.GlobalEvent;
import com.sunrise.orchestrator.result.Dto.GlobalEventSync;
import com.sunrise.web.payload.ApiRequest;
import com.sunrise.web.payload.ApiRequest.ChatSyncUnit;
import com.sunrise.web.websocket.event.WsAppEvent;
import com.sunrise.orchestrator.type.ChatType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatOrchestrator {

    private final ApplicationEventPublisher eventPublisher;

    private final ChatCacheService cacheChatService;

    private final ChatDbService dbChatService;
    private final ChatMemberDbService dbChatMemberService;
    private final EventDbService dbEventService;


    // ========== CHAT METHODS ==========


    // Основные методы

    @Transactional(propagation = MANDATORY)
    public void saveGroupChatAndAddMembers(String tempId, CreateDto.GroupChat chat,
                                           CreateDto.ChatMember creator, List<CreateDto.ChatMember> members) {

        List<Long> memberIds = members.stream()
            .map(CreateDto.ChatMember::getUserId).toList();
        
        List<Long> allUserIds = new ArrayList<>(memberIds);
        allUserIds.add(creator.getUserId());

        dbChatService.saveGroupChat(ChatMapper.toEntity(chat), memberIds.toArray(Long[]::new));

        var chatEvent = new IDomainEvent.ChatCreated(
            tempId, chat.getId(), chat.getCreatedAt()
        );
        var usersEvent = new IDomainEvent.UserChatCreated(
            chat.getId(), tempId, chat.getCreatedAt()
        );

        ChatUsersEvents seq = dbEventService.saveForChatAndAllUsersShared(
            chat.getId(), allUserIds, chatEvent, usersEvent
        );

        List<Cache.ChatMember> cacheMembers = new ArrayList<>();
        cacheMembers.add(ChatMemberMapper.toCache(creator));
        cacheMembers.addAll(ChatMemberMapper.toCaches(members));

        eventPublisher.publishEvent(new CacheEvent.ChatWithMembersCreated(ChatMapper.toCache(chat), cacheMembers));
        for (UserEvent userSeq : seq.usersEvent()) {
            eventPublisher.publishEvent(new WsAppEvent.UserChatCreated(userSeq, usersEvent));
        }
    }

    @Transactional(propagation = MANDATORY)
    public void savePersonalChatAndAddMembers(String tempId, CreateDto.PersonalChat chat,
                                              CreateDto.ChatMember creator, CreateDto.ChatMember opponent) {

        // Сохраняем чат и участников в основных таблицах
        dbChatService.savePersonalChat(ChatMapper.toEntity(chat), opponent.getUserId());

        // Формируем событие о создании чата
        var chatEvent = new IDomainEvent.ChatCreated(
            tempId, chat.getId(), chat.getCreatedAt()
        );
        var usersEvent = new IDomainEvent.UserChatCreated(
            chat.getId(), tempId, chat.getCreatedAt()
        );

        ChatUsersEvents seq = dbEventService.saveForChatAndAllUsersShared(
            chat.getId(), 
            List.of(creator.getUserId(), opponent.getUserId()), 
            chatEvent, usersEvent
        );

        eventPublisher.publishEvent(new CacheEvent.ChatWithMembersCreated(
            ChatMapper.toCache(chat),
            List.of(ChatMemberMapper.toCache(creator), ChatMemberMapper.toCache(opponent))
        ));
        
        for (UserEvent userSeq : seq.usersEvent()) {
            eventPublisher.publishEvent(new WsAppEvent.UserChatCreated(userSeq, usersEvent));
        }
    }

    @Transactional(propagation = MANDATORY)
    public boolean updateChatProfile(long chatId, String newName, String newDescription, Instant updatedAt) {
        int updated = dbChatService.updateProfile(chatId, newName, newDescription, updatedAt);
        if (updated > 0) {
            var event = new IDomainEvent.ChatUpdated(
                chatId, newName, newDescription, updatedAt
            );
            ChatEvent seq = dbEventService.saveChatEvent(
                chatId, event
            );
            eventPublisher.publishEvent(new CacheEvent.ChatInvalidated(chatId));
            eventPublisher.publishEvent(new WsAppEvent.ChatUpdated(seq, event));
        }
        return updated > 0;
    }

    @Transactional(propagation = MANDATORY)
    public boolean delete(long chatId, Instant updatedAt) {
        int updated = dbChatService.delete(chatId, updatedAt);
        if (updated > 0) {
            List<Long> memberIds = dbChatMemberService.getAllActiveMemberIds(
                chatId
            );

            var chatEvent = new IDomainEvent.ChatDeleted(
                chatId, updatedAt
            );
            var usersEvent = new IDomainEvent.UserChatDeleted(
                chatId, updatedAt
            );
            
            ChatUsersEvents seq = dbEventService.saveForChatAndAllUsersShared(
                chatId, memberIds, chatEvent, usersEvent
            );
            eventPublisher.publishEvent(new CacheEvent.ChatInvalidated(chatId));
            eventPublisher.publishEvent(new CacheEvent.CleanChatActions(chatId));
            for (UserEvent userSeq : seq.usersEvent()) {
                eventPublisher.publishEvent(new WsAppEvent.UserChatDeleted(userSeq, usersEvent));
            }
        }
        return updated > 0;
    }
    
    
    // Вспомогательные методы
    
    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public Optional<Dto.ChatSecurity> getActivePersonalChat(long userId1, long userId2) {
        // пробуем кеш
        Optional<Cache.Chat> cached = cacheChatService.getPersonalChat(userId1, userId2);
        if (cached.isPresent())
            return cached.filter(Cache.Chat::isActive).map(ChatMapper::toSecurityDTO);

        // грузим из бд
        Optional<ChatProfileResult> dbChat = dbChatService.getActivePersonalChat(userId1, userId2);
        dbChat.ifPresent(chat -> {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.ChatSave(
                ChatMapper.toCache(chat)
            ));
        });
        return dbChat.map(ChatMapper::toSecurityDTO);
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public List<Dto.ChatMeta> getChatsMeta(long userId) {
        List<ChatMetaResult> rows = dbChatService.getChatsMeta(userId);
        return rows.stream().map(ChatMapper::toMetaDTO).toList();
    }

    private static final int MAX_DELTA_EVENTS = 2000;

    @Transactional(readOnly = true)
    public Map<Long, GlobalEventSync> getSyncChats(List<ChatSyncUnit> cursors) {
        if (cursors.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<Long, Long> chatToLastEventId = cursors.stream()
            .collect(Collectors.toMap(ApiRequest.ChatSyncUnit::chatId, ApiRequest.ChatSyncUnit::lastEventId));
        
        Map<Long, Boolean> resetMap = dbEventService.areChatSyncResetRequired(chatToLastEventId, MAX_DELTA_EVENTS);
        Map<Long, GlobalEventSync> result = new HashMap<>();
        for (ChatSyncUnit cursor : cursors) {
            long chatId = cursor.chatId();
            if (resetMap.getOrDefault(chatId, true)) {
                result.put(chatId, new GlobalEventSync(Collections.emptyList(), false, true));
                continue;
            }
            
            List<ChatEventResult> events = dbEventService.getChatEventsAfter(chatId, cursor.lastEventId(), 101); // +1
            List<GlobalEvent> clientEvents = events.stream()
                .map(proj -> new Dto.GlobalEvent(
                    proj.getEventId(),
                    EventType.valueOf(proj.getEventType()),
                    dbEventService.deserializeEvent(proj.getEventType(), proj.getPayload()),
                    proj.getCreatedAt()
                )).limit(100).toList();
            
            result.put(chatId, new GlobalEventSync(clientEvents, events.size() > 100, false));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Dto.ChatStatsResult getChatClearStats(long chatId, long userId) {
        return ChatMapper.toChatStatsDTO(dbChatService.getChatClearStats(chatId, userId));
    }
}