package com.sunrise.db.repository;

import com.sunrise.db.entity.AppEvent;
import com.sunrise.db.result.ChatEventResult;
import com.sunrise.db.result.UserEventResult;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppEventRepository extends JpaRepository<AppEvent, Long> {

    @Query(value = 
        """
        SELECT
            ue.user_id AS userId,
            ue.event_id AS eventId,
            ae.event_type AS eventType,
            ae.payload AS payload,
            ae.created_at AS createdAt
        FROM users_events ue
        INNER JOIN app_events ae ON ae.id = ue.event_id
        WHERE ue.user_id = :userId AND ue.event_id > :lastEventId
        ORDER BY ue.event_id
        LIMIT :limit
        """, nativeQuery = true)
    List<UserEventResult> findUserEventsAfter(@Param("userId") long userId, @Param("lastEventId") long lastEventId, @Param("limit") int limit);

    @Query(value = 
        """
        SELECT
            ce.chat_id AS chatId,
            ce.event_id AS eventId,
            ae.event_type AS eventType,
            ae.payload AS payload,
            ae.created_at AS createdAt
        FROM chats_events ce
        INNER JOIN app_events ae ON ae.id = ce.event_id
        WHERE ce.chat_id = :chatId AND ce.event_id > :lastEventId
        ORDER BY ce.event_id
        LIMIT :limit
        """, nativeQuery = true)
    List<ChatEventResult> findChatEventsAfter(@Param("chatId") long chatId, @Param("lastEventId") long lastEventId, @Param("limit") int limit);
}