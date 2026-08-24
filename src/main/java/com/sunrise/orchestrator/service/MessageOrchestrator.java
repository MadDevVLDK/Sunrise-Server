package com.sunrise.orchestrator.service;

import static org.springframework.transaction.annotation.Propagation.MANDATORY;

import com.sunrise.cache.entity.*;
import com.sunrise.cache.event.CacheEvent;
import com.sunrise.cache.service.ChatMemberCacheService;
import com.sunrise.cache.service.MessageCacheService;
import com.sunrise.cache.service.UserCacheService;
import com.sunrise.core.creation.CreateDto;
import com.sunrise.db.entity.ChatMember;
import com.sunrise.db.entity.Message;
import com.sunrise.db.result.MessageReadStatusResult;
import com.sunrise.db.result.UserMessageResult;
import com.sunrise.db.result.UserProfileResult;
import com.sunrise.db.service.ChatMemberDbService;
import com.sunrise.db.service.EventDbService;
import com.sunrise.db.service.MessageDbService;
import com.sunrise.db.service.UserDbService;
import com.sunrise.db.service.EventDbService.ChatEvent;
import com.sunrise.db.service.EventDbService.ChatUsersEvents;
import com.sunrise.db.service.EventDbService.UserEvent;
import com.sunrise.helpclass.SnowflakeId;
import com.sunrise.helpclass.mapper.ChatMemberMapper;
import com.sunrise.helpclass.mapper.MessageMapper;
import com.sunrise.helpclass.mapper.OtherMapper;
import com.sunrise.helpclass.mapper.UserMapper;
import com.sunrise.orchestrator.event.IDomainEvent;
import com.sunrise.orchestrator.result.Dto;
import com.sunrise.orchestrator.type.Direction;
import com.sunrise.web.websocket.event.WsAppEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class MessageOrchestrator {

    private final ApplicationEventPublisher eventPublisher;
    
    private final MessageCacheService cacheMessageService;
    private final MessageDbService dbMessageService;

    private final UserCacheService cacheUserService;
    private final UserDbService dbUserService;

    private final ChatMemberCacheService cacheChatMemberService;
    private final ChatMemberDbService dbChatMemberService;

    private final EventDbService eventDbService;


    // ========== MESSAGE METHODS ==========


    // Основные методы

    @Transactional(propagation = MANDATORY)
    public boolean save(String tempId, Instant userProfileUpdatedAt, CreateDto.Message message) {
        dbMessageService.save(MessageMapper.toEntity(message));
        
        List<Long> memberIds = dbChatMemberService.getAllActiveMemberIds(message.getChatId());

        var chatEvent = new IDomainEvent.MessageCreated(
            tempId, message.getChatId(), 
            message.getId(), message.getSentAt()
        );
        var usersEvent = new IDomainEvent.UserChatMessageSent(
            message.getChatId(), message.getId(), message.getSentAt()
        );
        ChatUsersEvents seq = eventDbService.saveForChatAndAllUsersShared(
            message.getChatId(), memberIds, chatEvent, usersEvent
        );
        
        eventPublisher.publishEvent(new CacheEvent.MessageCreated(MessageMapper.toCache(message)));
        eventPublisher.publishEvent(new WsAppEvent.MessageCreatedFull(
            seq.chatEvent(), new IDomainEvent.MessageCreatedFull(
                tempId, message.getChatId(), message.getId(),
                message.getSenderId(), message.getText(),
                userProfileUpdatedAt, message.getSentAt()
            )
        ));
        
        // для пагинации, чтобы клиенту не перезапрашивать 
        for (UserEvent userSeq : seq.usersEvent()) {
            eventPublisher.publishEvent(new WsAppEvent.UserChatMessageSent(userSeq, usersEvent));
        }
        return true;
    }

    @Transactional(propagation = MANDATORY)
    public boolean update(long chatId, long messageId, String newText, Instant updatedAt) {
        int updated = dbMessageService.update(messageId, newText, updatedAt);
        if (updated > 0) {
            var chatEvent = new IDomainEvent.MessageUpdated(
                chatId, messageId, newText, updatedAt
            );
            ChatEvent seq = eventDbService.saveChatEvent(
                chatId, chatEvent
            );
            eventPublisher.publishEvent(new CacheEvent.MessageInvalidated(messageId));
            eventPublisher.publishEvent(new WsAppEvent.MessageInfoUpdated(seq, chatEvent));
        }
        return updated > 0;
    }

    @Transactional(propagation = MANDATORY)
    public boolean delete(long chatId, long messageId, Instant updatedAt) {
        int updated = dbMessageService.delete(messageId, updatedAt);
        if (updated > 0) {
            var chatEvent = new IDomainEvent.MessageDeleted(
                chatId, messageId, updatedAt
            );
            ChatEvent seq = eventDbService.saveChatEvent(
                chatId, chatEvent
            );
            eventPublisher.publishEvent(new CacheEvent.MessageInvalidated(messageId));
            eventPublisher.publishEvent(new WsAppEvent.MessageDeleted(seq, chatEvent));
        }
        return updated > 0;
    }

    @Transactional(propagation = MANDATORY)
    public boolean markMessagesUpToRead(long chatId, long userId, long upToMessageId, Instant readAt) {
        List<Long> markedAsReadMessageIds = dbMessageService.markMessagesUpToRead(chatId, userId, upToMessageId, readAt);
        if (markedAsReadMessageIds.size() > 0) {
            eventPublisher.publishEvent(new WsAppEvent.MessagesReadUpTo(
                new ChatEvent(chatId, SnowflakeId.next()), 
                new IDomainEvent.MessagesReadUpTo(
                    chatId, userId, upToMessageId, markedAsReadMessageIds.size(), readAt
                )
            ));
            eventPublisher.publishEvent(new CacheEvent.MessagesMarkAsReadBatch(markedAsReadMessageIds));
        }
        return true;
    }


    // Вспомогательные методы

    @Transactional(readOnly = true)
    public boolean isActiveInChat(long chatId, long messageId) {
        // пробуем кеш
        Optional<Cache.Message> cacheMessage = cacheMessageService.get(messageId);
        if (cacheMessage.isPresent())
            return cacheMessage.filter(msg -> msg.isActive() && msg.chatId() == chatId).isPresent();

        // грузим из бд
        Optional<Message> dbMessage = dbMessageService.get(chatId, messageId);
        dbMessage.ifPresent(msg -> {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.MessageSave(
                MessageMapper.toCache(msg)
            ));
        });
        return dbMessage.filter(Message::isActive).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean isActiveInChatAndBySender(long chatId, long userId, long messageId) {
        // пробуем кеш
        Optional<Cache.Message> cacheMessage = cacheMessageService.get(messageId);
        if (cacheMessage.isPresent())
            return cacheMessage.filter(msg -> msg.isActive() && msg.chatId() == chatId && msg.senderId() == userId).isPresent();

        // грузим из бд
        Optional<Message> dbMessage = dbMessageService.get(chatId, messageId);
        dbMessage.ifPresent(msg -> {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.MessageSave(
                MessageMapper.toCache(msg)
            ));
        });
        return dbMessage.filter(Message::isActive).filter(msg -> msg.getSenderId() == userId).isPresent();
    }

    @Transactional(readOnly = true)
    public Optional<Dto.Message> getActiveWithReadStatusInChat(long chatId, long userId, long messageId) {
        // грузим из бд
        Optional<UserMessageResult> dbMessage = dbMessageService.getUserMessage(chatId, userId, messageId);
        dbMessage.ifPresent(msg -> {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.MessageSave(
                MessageMapper.toCache(msg)
            ));
        });
        return dbMessage.map(MessageMapper::toUserDTO);
    }
    
    @Transactional(readOnly = true)
    public List<Dto.Message> getActiveWithReadStatusInChatBatch(long chatId, long userId, Set<Long> messageIds) {
        // грузим из бд
        List<UserMessageResult> dbMessages = dbMessageService.getUserMessageBatch(chatId, userId, messageIds);
        if (!dbMessages.isEmpty()){
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.MessagesSave(
                MessageMapper.toCaches(dbMessages)
            ));
        }
        return dbMessages.stream().map(MessageMapper::toUserDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<Dto.MessageReadStatus> getMessageReaders(long messageId){
        // грузим из бд
        List<MessageReadStatusResult> reads = dbMessageService.getMessageReaders(messageId);
        return OtherMapper.toMessageReadDTOs(reads);
    }


    // ПАГИНАЦИЯ !!!!!!!!!

    @Transactional(readOnly = true)
    public Dto.MessagesPage getPage(long chatId, long userId, Long cursor, int limit, Direction direction) {
        if (cursor == null) direction = Direction.BACKWARD;
        
        // Проверяем наличие кеша последних ID для чата
        if (!cacheMessageService.hasRecentIds(chatId)) {
            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(
                new CacheEvent.MessagesRecentIdsInit(chatId)
            );

            // загружаем с бд, после коммита загрузятся сообщения в кеш
            return buildFromDbResultWithoutCache(chatId, userId, cursor, limit, direction);
        }

        // Получаем limit + 1 ID из кеша (с учётом курсора и направления)
        List<Long> neededIds = cacheMessageService.getRecentIdsRange(chatId, cursor, limit + 1, direction);
        if (neededIds.isEmpty()) {
            return new Dto.MessagesPage(Collections.emptyList(), null);
        }

        // Если кеш не полон, то используем кеш
        if (!cacheMessageService.isRecentIdsFull(chatId)) {
            return buildPageFromRecentIds(chatId, neededIds, limit, direction);
        }

        // Если кеш полон
        if (neededIds.size() > limit) {
            // кеш дал достаточно IDs
            return buildPageFromRecentIds(chatId, neededIds, limit, direction);
        } else {
            // кеш не дал достаточно IDs (идём в БД, результат не кешируем)
            return buildFromDbResultWithoutCache(chatId, userId, cursor, limit, direction);
        }
    }

    private Dto.MessagesPage buildFromDbResultWithoutCache(long chatId, long userId, Long cursor, int limit, Direction direction){
        List<UserMessageResult> dbResult = dbMessageService.getPage(chatId, userId, cursor, limit + 1, direction);
        if (dbResult.isEmpty()) {
            return new Dto.MessagesPage(Collections.emptyList(), null);
        }

        Long nextCursor = null;
        if (dbResult.size() > limit) {
            dbResult = dbResult.subList(0, limit);
            if (direction == Direction.BACKWARD) {
                // Для старых сообщений (BACKWARD) следующий курсор = самый старый ID (последний в списке, т.к. порядок DESC)
                nextCursor = dbResult.getLast().getId();
            } else {
                // Для новых сообщений (FORWARD) следующий курсор = самый новый ID (первый в списке)
                nextCursor = dbResult.getFirst().getId();
            }
        }

        List<Dto.Message> result = dbResult.stream().map(MessageMapper::toUserDTO).toList();
        return new Dto.MessagesPage(result, nextCursor);
    }

    private Dto.MessagesPage buildPageFromRecentIds(long chatId, List<Long> ids, int limit, Direction direction) {
        List<Dto.Message> result = new LinkedList<>();
        if (ids.isEmpty()) {
            return new Dto.MessagesPage(result, null);
        }

        // Определяем курсор и удаляем его из пагинации
        Long nextCursor = null;
        if (ids.size() > limit) {
            ids = ids.subList(0, limit);          
            if (direction == Direction.BACKWARD) {
                nextCursor = ids.getLast();
            } else {
                nextCursor = ids.getFirst();
            }
        }

        // Получаем объекты сообщений (из кеша или БД)
        Map<Long, Cache.Message> messages = loadMessagesFromCacheOrDb(ids);
                
        List<Cache.Message> orderedMessages = ids.stream()
                .map(messages::get)
                .filter(Objects::nonNull).toList();

        // Получаем senderId пользователей, чтобы позже их загрузить
        Set<Long> senderIds = orderedMessages.stream()
                .map(Cache.Message::senderId).collect(Collectors.toSet());

        // Пакетно загружаем профили пользователей
        Map<Long, Dto.UserProfileLight> userProfileMap = loadUserProfilesFromCacheOrDb(senderIds);

        // Пакетно загружаем профили участников чата
        Map<Long, Dto.ChatMemberProfile> memberProfileMap = loadChatMemberProfilesFromCacheOrDb(chatId, senderIds);

        // Формируем результат
        for (Cache.Message msg : orderedMessages) {
            Dto.UserProfileLight userProfile = userProfileMap.get(msg.senderId());
            Dto.ChatMemberProfile memberProfile = memberProfileMap.get(msg.senderId());

            Instant profileUpdatedAt = userProfile != null ? userProfile.profileUpdatedAt() : null;
            Instant memberUpdatedAt = memberProfile != null ? memberProfile.updatedAt() : null;

            result.add(MessageMapper.toUserDTO(msg, profileUpdatedAt, memberUpdatedAt));
        }

        return new Dto.MessagesPage(result, nextCursor);
    }

    private Map<Long, Cache.Message> loadMessagesFromCacheOrDb(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Cache.Message> result = new HashMap<>(ids.size());
        List<Long> missingIds = new ArrayList<>();

        for (Long id : ids) {
            cacheMessageService.get(id).ifPresentOrElse(
                (m) -> result.put(id, m),
                () -> missingIds.add(id)
            );
        }

        if (!missingIds.isEmpty()) {
            Map<Long, Cache.Message> cacheMessages = dbMessageService.getMessagesByIds(missingIds)
                        .stream().map(MessageMapper::toCache).collect(Collectors.toMap(Cache.Message::id, Function.identity()));

            if (!cacheMessages.isEmpty()){
                // публикуем для обновления кеша после коммита
                eventPublisher.publishEvent(
                    new CacheEvent.MessagesSave(cacheMessages.values())
                );
            }

            result.putAll(cacheMessages);
        }
        return result;
    }

    private Map<Long, Dto.UserProfileLight> loadUserProfilesFromCacheOrDb(Set<Long> userIds) {
        Map<Long, Dto.UserProfileLight> result = new HashMap<>();
        if (userIds.isEmpty()) {
            return result;
        }

        Set<Long> missingIds = new HashSet<>();

        // Сначала забираем из кеша
        for (Long userId : userIds) {
            cacheUserService.getProfile(userId)
                .map(UserMapper::toProfileLightDTO)
                .ifPresentOrElse(
                    dto -> result.put(userId, dto),
                    () -> missingIds.add(userId)
                );
        }

        // Недостающие грузим из БД одной пачкой
        if (!missingIds.isEmpty()) {
            List<UserProfileResult> dbProfiles = dbUserService.getUserProfilesByIds(new ArrayList<>(missingIds));
            List<Cache.UserProfile> toCache = new ArrayList<>();
            for (UserProfileResult profile : dbProfiles) {
                toCache.add(UserMapper.toProfileCache(profile));
                result.put(profile.getId(), UserMapper.toProfileLightDTO(profile));
            }

            if (!toCache.isEmpty()) {
                // публикуем для обновления кеша после коммита
                eventPublisher.publishEvent(
                    new CacheEvent.UserProfilesSave(toCache)
                );
            }
        }
        return result;
    }
    
    private Map<Long, Dto.ChatMemberProfile> loadChatMemberProfilesFromCacheOrDb(long chatId, Set<Long> userIds) {
        Map<Long, Dto.ChatMemberProfile> result = new HashMap<>();
        if (userIds.isEmpty()) {
            return result;
        }

        Set<Long> missingIds = new HashSet<>();

        // Сначала из кеша
        for (Long userId : userIds) {
            cacheChatMemberService.get(chatId, userId)
                .map(ChatMemberMapper::toProfileDTO)
                .ifPresentOrElse(
                    dto -> result.put(userId, dto),
                    () -> missingIds.add(userId)
                );
        }

        // Недостающие из БД
        if (!missingIds.isEmpty()) {
            List<ChatMember> dbMembers = dbChatMemberService.getBatchByChatAndIds(chatId, new ArrayList<>(missingIds));
            List<Cache.ChatMember> toCache = new ArrayList<>();
            for (ChatMember member : dbMembers) {
                Cache.ChatMember cacheMember = ChatMemberMapper.toCache(member);
                toCache.add(cacheMember);
                result.put(member.getUserId(), ChatMemberMapper.toProfileDTO(member));
            }
            if (!toCache.isEmpty()) {
                // публикуем для обновления кеша после коммита
                eventPublisher.publishEvent(
                    new CacheEvent.ChatMembersSave(chatId, toCache)
                );
            }
        }
        return result;
    }
}