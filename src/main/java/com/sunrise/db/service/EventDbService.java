package com.sunrise.db.service;

import com.sunrise.db.repository.AppEventRepository;
import com.sunrise.db.result.ChatEventResult;
import com.sunrise.db.result.UserEventResult;
import com.sunrise.helpclass.SnowflakeId;
import com.sunrise.orchestrator.event.EventRegistry;
import com.sunrise.orchestrator.event.EventType;
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
    private final EventRegistry eventRegistry;
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
            .addValue("eventType", eventRegistry.getEventType(chatEvent).name())
            .addValue("payload", eventRegistry.serialize(chatEvent))
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
            .addValue("eventType", eventRegistry.getEventType(userEvent).name())
            .addValue("payload", eventRegistry.serialize(userEvent))
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
            .addValue("eventType", eventRegistry.getEventType(usersEvent).name())
            .addValue("payload", eventRegistry.serialize(usersEvent))
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

    public Map<Long, Boolean> areChatSyncResetRequired(Map<Long, Long> chatToLastEventId, int maxDelta) {
        if (chatToLastEventId.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Long[] chatIds = chatToLastEventId.keySet().toArray(new Long[0]);
        Long[] lastEventIds = chatToLastEventId.values().toArray(new Long[0]);
        
        String sql = """
            SELECT 
                cv.chat_id AS chatId,
                NOT EXISTS (SELECT 1 FROM app_events WHERE id = cv.last_event_id)
                    OR (SELECT COUNT(*) > :maxDelta 
                        FROM chats_events ce2 
                        WHERE ce2.chat_id = cv.chat_id
                            AND ce2.event_id > cv.last_event_id) AS reset_required
            FROM (SELECT unnest(:chatIds) AS chat_id, unnest(:lastEventIds) AS last_event_id) cv
            """;
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("chatIds", chatIds)
            .addValue("lastEventIds", lastEventIds)
            .addValue("maxDelta", maxDelta);
        
        return namedJdbcTemplate.query(sql, params, rs -> {
            Map<Long, Boolean> map = new HashMap<>();
            while (rs.next()) {
                map.put(rs.getLong("chatId"), rs.getBoolean("reset_required"));
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

    public IDomainEvent deserializeEvent(String eventType, String payload) {
        return eventRegistry.deserialize(EventType.valueOf(eventType), payload);
    }

    public record ChatEvent(long chatId, long eventId) { };
    public record UserEvent(long userId, long eventId) { };
    public record ChatUserEvents(ChatEvent chatEvent, UserEvent userEvent) { };
    public record ChatUsersEvents(ChatEvent chatEvent, List<UserEvent> usersEvent) { };
}