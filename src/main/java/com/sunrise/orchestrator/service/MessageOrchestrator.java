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
import com.sunrise.db.service.ChatEventDbService;
import com.sunrise.db.service.ChatMemberDbService;
import com.sunrise.db.service.MessageDbService;
import com.sunrise.db.service.UserDbService;
import com.sunrise.helpclass.mapper.ChatEventMapper;
import com.sunrise.helpclass.mapper.ChatMemberMapper;
import com.sunrise.helpclass.mapper.MessageMapper;
import com.sunrise.helpclass.mapper.OtherMapper;
import com.sunrise.helpclass.mapper.UserMapper;
import com.sunrise.orchestrator.result.ChatEvent;
import com.sunrise.orchestrator.result.Dto;
import com.sunrise.orchestrator.type.Direction;

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

    private final ChatEventDbService chatEventDbService;


    // ========== MESSAGE METHODS ==========


    // Основные методы

    @Transactional(propagation = MANDATORY)
    public void save(CreateDto.Message message) {
        // синхронно в бд
        dbMessageService.save(MessageMapper.toEntity(message));

        var event = new ChatEvent.MessageCreated(
            message.getChatId(), message.getId(), 
            message.getSenderId(), message.getText(), 
            message.getSentAt(), Instant.now()
        );
        chatEventDbService.save(ChatEventMapper.toEntity(event));

        // публикуем для обновления кеша после коммита
        eventPublisher.publishEvent(new CacheEvent.MessageCreated(
            MessageMapper.toCache(message)
        ));
    }

    @Transactional(propagation = MANDATORY)
    public boolean update(long chatId, long messageId, String newText, Instant updatedAt) {
        // синхронно в бд
        int updated = dbMessageService.update(messageId, newText, updatedAt);
        if (updated > 0) {
            var event = new ChatEvent.MessageUpdated(
                chatId, messageId, newText, updatedAt
            );
            chatEventDbService.save(ChatEventMapper.toEntity(event));

            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.MessageInvalidated(messageId));
        }
        return updated > 0;
    }

    @Transactional(propagation = MANDATORY)
    public void markMessagesUpToRead(long chatId, long userId, long messageId, Instant readAt) {
        // синхронно в бд
        dbMessageService.markMessagesUpToRead(chatId, userId, messageId, readAt);

        var event = new ChatEvent.MessagesReadUpTo(
            chatId, userId, messageId, readAt
        );
        chatEventDbService.save(ChatEventMapper.toEntity(event));
    }

    @Transactional(propagation = MANDATORY)
    public boolean delete(long chatId, long messageId, Instant updatedAt) {
        // синхронно в бд
        int updated = dbMessageService.delete(messageId, updatedAt);
        if (updated > 0) {
            var event = new ChatEvent.MessageDeleted(
                chatId, messageId, updatedAt
            );
            chatEventDbService.save(ChatEventMapper.toEntity(event));

            // публикуем для обновления кеша после коммита
            eventPublisher.publishEvent(new CacheEvent.MessageInvalidated(messageId));
        }
        return updated > 0;
    }


    // Вспомогательные методы

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

    public List<Dto.MessageReadStatus> getMessageReaders(long messageId){
        // грузим из бд
        List<MessageReadStatusResult> reads = dbMessageService.getMessageReaders(messageId);
        return OtherMapper.toMessageReadDTOs(reads);
    }


    // ПАГИНАЦИЯ !!!!!!!!!

    public Dto.MessagesPage getPage(long chatId, long userId, Long cursor, int limit, Direction direction) {
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
        if (dbResult.size() == limit + 1) {
            if (direction == Direction.FORWARD) {
                dbResult = dbResult.subList(0, limit);
                nextCursor = dbResult.getLast().getId();
            } else {
                dbResult = dbResult.subList(dbResult.size() - limit, dbResult.size());
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

        // Получаем объекты сообщений (из кеша или БД)
        List<Cache.Message> messages = loadMessagesFromCacheOrDb(ids);
        
        // Сортируем в соответствии с порядком ids (который уже правильный)
        Map<Long, Cache.Message> messageMap = messages.stream()
                .collect(Collectors.toMap(Cache.Message::id, Function.identity()));
                
        List<Cache.Message> orderedMessages = ids.stream()
                .map(messageMap::get)
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

        // Для направления FORWARD переворачиваем
        if (direction == Direction.FORWARD) {
            Collections.reverse(result);
        }

        // Определяем курсор и удаляем его из пагинации
        Long nextCursor = null;
        if (result.size() == limit + 1) {
            if (direction == Direction.FORWARD) {
                result = result.subList(0, limit);
                nextCursor = result.getLast().id();
            } else {
                result = result.subList(result.size() - limit, result.size());
                nextCursor = result.getFirst().id();
            }
        }
        return new Dto.MessagesPage(result, nextCursor);
    }

    private List<Cache.Message> loadMessagesFromCacheOrDb(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<Cache.Message> result = new ArrayList<>(ids.size());
        List<Long> missingIds = new ArrayList<>();

        for (Long id : ids) {
            cacheMessageService.get(id).ifPresentOrElse(
                result::add,
                () -> missingIds.add(id)
            );
        }

        if (!missingIds.isEmpty()) {
            List<Cache.Message> cacheMessages = dbMessageService.getMessagesByIds(missingIds)
                        .stream().map(m -> MessageMapper.toCache(m)).toList();

            if (!cacheMessages.isEmpty()){
                // публикуем для обновления кеша после коммита
                eventPublisher.publishEvent(
                    new CacheEvent.MessagesSave(cacheMessages)
                );
            }

            result.addAll(cacheMessages);
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