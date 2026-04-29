package com.sunrise.orchestrator.service;

import static org.springframework.transaction.annotation.Propagation.MANDATORY;

import com.sunrise.cache.CacheEvent;
import com.sunrise.cache.entity.*;
import com.sunrise.cache.service.ChatMemberCacheService;
import com.sunrise.cache.service.MessageCacheService;
import com.sunrise.cache.service.UserCacheService;
import com.sunrise.core.creation.CreateMessageDTO;
import com.sunrise.db.entity.ChatMember;
import com.sunrise.db.entity.Message;
import com.sunrise.db.event.ChatEvent;
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
import com.sunrise.orchestrator.result.ChatMemberProfileDTO;
import com.sunrise.orchestrator.result.MessageReadStatusDTO;
import com.sunrise.orchestrator.result.MessagesPageDTO;
import com.sunrise.orchestrator.result.UserMessageDTO;
import com.sunrise.orchestrator.result.UserProfileLightDTO;
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
    public void save(CreateMessageDTO message) {
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
        Optional<CacheMessage> cacheMessage = cacheMessageService.get(messageId);
        if (cacheMessage.isPresent())
            return cacheMessage.filter(msg -> msg.isActive() && msg.getChatId() == chatId).isPresent();

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
        Optional<CacheMessage> cacheMessage = cacheMessageService.get(messageId);
        if (cacheMessage.isPresent())
            return cacheMessage.filter(msg -> msg.isActive() && msg.getChatId() == chatId && msg.getSenderId() == userId).isPresent();

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

    public Optional<UserMessageDTO> getActiveWithReadStatusInChat(long chatId, long userId, long messageId) {
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
    
    public List<UserMessageDTO> getActiveWithReadStatusInChatBatch(long chatId, long userId, Set<Long> messageIds) {
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

    public List<MessageReadStatusDTO> getMessageReaders(long messageId){
        // грузим из бд
        List<MessageReadStatusResult> reads = dbMessageService.getMessageReaders(messageId);
        return OtherMapper.toMessageReadDTOs(reads);
    }


    // ПАГИНАЦИЯ !!!!!!!!!

    public MessagesPageDTO getPage(long chatId, long userId, Long cursor, int limit, Direction direction) {
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
            return new MessagesPageDTO(Collections.emptyList(), null);
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

    private MessagesPageDTO buildFromDbResultWithoutCache(long chatId, long userId, Long cursor, int limit, Direction direction){
        List<UserMessageResult> dbResult = dbMessageService.getPage(chatId, userId, cursor, limit + 1, direction);
        if (dbResult.isEmpty()) {
            return new MessagesPageDTO(Collections.emptyList(), null);
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

        List<UserMessageDTO> result = dbResult.stream().map(MessageMapper::toUserDTO).toList();
        return new MessagesPageDTO(result, nextCursor);
    }

    private MessagesPageDTO buildPageFromRecentIds(long chatId, List<Long> ids, int limit, Direction direction) {
        List<UserMessageDTO> result = new LinkedList<>();
        if (ids.isEmpty()) {
            return new MessagesPageDTO(result, null);
        }

        // Получаем объекты сообщений (из кеша или БД)
        List<CacheMessage> messages = loadMessagesFromCacheOrDb(ids);
        
        // Сортируем в соответствии с порядком ids (который уже правильный)
        Map<Long, CacheMessage> messageMap = messages.stream()
                .collect(Collectors.toMap(CacheMessage::getId, Function.identity()));
                
        List<CacheMessage> orderedMessages = ids.stream()
                .map(messageMap::get)
                .filter(Objects::nonNull).toList();

        // Получаем senderId пользователей, чтобы позже их загрузить
        Set<Long> senderIds = orderedMessages.stream()
                .map(CacheMessage::getSenderId).collect(Collectors.toSet());

        // Пакетно загружаем профили пользователей
        Map<Long, UserProfileLightDTO> userProfileMap = loadUserProfilesFromCacheOrDb(senderIds);

        // Пакетно загружаем профили участников чата
        Map<Long, ChatMemberProfileDTO> memberProfileMap = loadChatMemberProfilesFromCacheOrDb(chatId, senderIds);

        // Формируем результат
        for (CacheMessage msg : orderedMessages) {
            UserProfileLightDTO userProfile = userProfileMap.get(msg.getSenderId());
            ChatMemberProfileDTO memberProfile = memberProfileMap.get(msg.getSenderId());

            Instant profileUpdatedAt = userProfile != null ? userProfile.getProfileUpdatedAt() : null;
            Instant memberUpdatedAt = memberProfile != null ? memberProfile.getUpdatedAt() : null;

            result.add(MessageMapper.toUserDTO(msg, profileUpdatedAt, memberUpdatedAt, msg.isDeleted()));
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
                nextCursor = result.getLast().getId();
            } else {
                result = result.subList(result.size() - limit, result.size());
                nextCursor = result.getFirst().getId();
            }
        }
        return new MessagesPageDTO(result, nextCursor);
    }

    private List<CacheMessage> loadMessagesFromCacheOrDb(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<CacheMessage> result = new ArrayList<>(ids.size());
        List<Long> missingIds = new ArrayList<>();

        for (Long id : ids) {
            cacheMessageService.get(id).ifPresentOrElse(
                result::add,
                () -> missingIds.add(id)
            );
        }

        if (!missingIds.isEmpty()) {
            List<CacheMessage> cacheMessages = dbMessageService.getMessagesByIds(missingIds)
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

    private Map<Long, UserProfileLightDTO> loadUserProfilesFromCacheOrDb(Set<Long> userIds) {
        Map<Long, UserProfileLightDTO> result = new HashMap<>();
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
            List<CacheUserProfile> toCache = new ArrayList<>();
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
    
    private Map<Long, ChatMemberProfileDTO> loadChatMemberProfilesFromCacheOrDb(long chatId, Set<Long> userIds) {
        Map<Long, ChatMemberProfileDTO> result = new HashMap<>();
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
            List<CacheChatMember> toCache = new ArrayList<>();
            for (ChatMember member : dbMembers) {
                CacheChatMember cacheMember = ChatMemberMapper.toCache(member);
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