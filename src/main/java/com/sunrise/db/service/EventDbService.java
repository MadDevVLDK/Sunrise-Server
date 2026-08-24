package com.sunrise.db.service;

import com.sunrise.db.repository.AppEventRepository;
import com.sunrise.db.result.ChatEventResult;
import com.sunrise.db.result.ChatSyncInfo;
import com.sunrise.db.result.UserEventResult;
import com.sunrise.helpclass.SnowflakeId;
import com.sunrise.orchestrator.event.EventRegistry;
import com.sunrise.orchestrator.event.IDomainEvent;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class EventDbService {

    private final AppEventRepository appEventRepository;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    @SneakyThrows
    @Transactional(propagation = Propagation.MANDATORY)
    public ChatEvent saveChatEvent(long chatId, IDomainEvent chatEvent) {
        String sql = """
            WITH inserted_event AS (
                INSERT INTO app_events (id, event_type, payload, created_at)
                VALUES (:eventId, :eventType, :payload::jsonb, :createdAt)
                RETURNING id
            )
            INSERT INTO chats_events (chat_id, event_id)
            SELECT :chatId, id FROM inserted_event
            """;

        long eventId = SnowflakeId.next();
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("eventId", eventId)
            .addValue("eventType", EventRegistry.getEventType(chatEvent).name())
            .addValue("payload", EventRegistry.serialize(chatEvent))
            .addValue("createdAt", chatEvent.getCreatedAt().atOffset(ZoneOffset.UTC))
            .addValue("chatId", chatId);

        namedJdbcTemplate.update(sql, params);
        return new ChatEvent(chatId, eventId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UserEvent saveUserEvent(long userId, IDomainEvent userEvent) {
        String sql = """
            WITH inserted_event AS (
                INSERT INTO app_events (id, event_type, payload, created_at)
                VALUES (:eventId, :eventType, :payload::jsonb, :createdAt)
                RETURNING id
            )
            INSERT INTO users_events (user_id, event_id)
            SELECT :userId, id FROM inserted_event
            """;

        long eventId = SnowflakeId.next();
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("eventId", eventId)
            .addValue("eventType", EventRegistry.getEventType(userEvent).name())
            .addValue("payload", EventRegistry.serialize(userEvent))
            .addValue("createdAt", userEvent.getCreatedAt().atOffset(ZoneOffset.UTC))
            .addValue("userId", userId);

        namedJdbcTemplate.update(sql, params);
        return new UserEvent(userId, eventId);
    }

    public ChatUserEvents saveForChatAndUser(long chatId, long userId, IDomainEvent chatEvent, IDomainEvent userEvent) {
        return new ChatUserEvents(
            saveChatEvent(chatId, chatEvent),
            saveUserEvent(userId, userEvent)
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<UserEvent> saveUserEventSharedBatch(List<Long> userIds, IDomainEvent usersEvent) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyList();

        String sql = """
            WITH inserted_event AS (
                INSERT INTO app_events (id, event_type, payload, created_at)
                VALUES (:eventId, :eventType, :payload::jsonb, :createdAt)
                RETURNING id
            )
            INSERT INTO users_events (user_id, event_id)
            SELECT unnest(:userIds), id FROM inserted_event
            """;

        long eventId = SnowflakeId.next();
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("eventId", eventId)
            .addValue("eventType", EventRegistry.getEventType(usersEvent).name())
            .addValue("payload", EventRegistry.serialize(usersEvent))
            .addValue("createdAt", usersEvent.getCreatedAt().atOffset(ZoneOffset.UTC))
            .addValue("userIds", userIds.toArray(Long[]::new));

        namedJdbcTemplate.update(sql, params);
        return userIds.stream().map(userId -> new UserEvent(userId, eventId)).toList();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ChatUsersEvents saveForChatAndAllUsersShared(long chatId, List<Long> userIds, IDomainEvent chatEvent, IDomainEvent usersEvent) {
        return new ChatUsersEvents(
            saveChatEvent(chatId, chatEvent),
            saveUserEventSharedBatch(userIds, usersEvent)
        );
    }

    public Map<Long, ChatSyncInfo> areChatSyncResetRequired(Map<Long, Long> chatToLastEventId, long userId, int maxDelta) {
        if (chatToLastEventId.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Long[] chatIds = chatToLastEventId.keySet().toArray(new Long[0]);
        Long[] lastEventIds = chatToLastEventId.values().toArray(new Long[0]);
        
        String sql = """
            WITH cv AS (
                SELECT unnest(:chatIds) AS chat_id, 
                       unnest(:lastEventIds) AS last_event_id
            ),
            reset_status AS (
                SELECT 
                    cv.chat_id,
                    (NOT EXISTS (SELECT 1 FROM app_events WHERE id = cv.last_event_id)
                    OR EXISTS (SELECT 1 FROM chats_events ce2 
                        WHERE ce2.chat_id = cv.chat_id AND ce2.event_id > cv.last_event_id
                        HAVING COUNT(*) > :maxDelta)) AS reset_required
                FROM cv
            )
            SELECT 
                rs.chat_id AS chatId,
                rs.reset_required AS resetRequired,
                ucr.last_read_message_id AS lastReadMsgByMe,
                m.max_id AS lastReadMsgByAnyone
            FROM reset_status rs
            LEFT JOIN LATERAL (
                SELECT last_read_message_id 
                FROM user_chat_read_state 
                WHERE user_id = :userId AND chat_id = rs.chat_id
            ) ucr ON NOT rs.reset_required
            LEFT JOIN LATERAL (
                SELECT MAX(id) AS max_id 
                FROM messages 
                WHERE chat_id = rs.chat_id AND read_count > 0
            ) m ON NOT rs.reset_required;
            """;
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("chatIds", chatIds)
            .addValue("lastEventIds", lastEventIds)
            .addValue("maxDelta", maxDelta)
            .addValue("userId", userId);
        
        return namedJdbcTemplate.query(sql, params, rs -> {
            Map<Long, ChatSyncInfo> map = new HashMap<>();
            while (rs.next()) {
                map.put(rs.getLong("chatId"), new ChatSyncInfo(
                    rs.getBoolean("resetRequired"),
                    rs.getLong("lastReadMsgByMe"),
                    rs.getLong("lastReadMsgByAnyone")
                ));
            }
            return map;
        });
    }

    public boolean isUserSyncResetRequired(long userId, long lastEventId, int maxDelta) {
        String sql = """
            SELECT NOT EXISTS (SELECT 1 FROM app_events WHERE id = :lastEventId)
                OR (SELECT COUNT(*) > :maxDelta 
                    FROM users_events 
                    WHERE user_id = :userId AND event_id > :lastEventId)
            """;
            
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("lastEventId", lastEventId)
            .addValue("maxDelta", maxDelta);
        return namedJdbcTemplate.queryForObject(sql, params, Boolean.class);
    }

    public List<UserEventResult> getUserEventsAfter(long userId, long cursor, int limit) {
        return appEventRepository.findUserEventsAfter(userId, cursor, limit);
    }

    public List<ChatEventResult> getChatEventsAfter(long chatId, long cursor, int limit) {
        return appEventRepository.findChatEventsAfter(chatId, cursor, limit);
    }

    public record ChatEvent(long chatId, long eventId) { };
    public record UserEvent(long userId, long eventId) { };
    public record ChatUserEvents(ChatEvent chatEvent, UserEvent userEvent) { };
    public record ChatUsersEvents(ChatEvent chatEvent, List<UserEvent> usersEvent) { };
}