package com.sunrise.db.repository;

import com.sunrise.db.entity.ChatEventDb;
import com.sunrise.db.entity.ChatEventId;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ChatEventRepository extends JpaRepository<ChatEventDb, ChatEventId> {

    @Modifying
    @Transactional
    @Query(value = """
        WITH new_seq AS (
            INSERT INTO chat_seq (chat_id, current_seq)
            VALUES (:chatId, 1)
            ON CONFLICT (chat_id) DO UPDATE SET current_seq = chat_seq.current_seq + 1
            RETURNING current_seq AS seq
        )
        INSERT INTO chat_events (chat_id, seq, event_type, payload, created_at)
        SELECT :chatId, seq, :eventType, CAST(:payload AS jsonb), :createdAt
        FROM new_seq
        """, nativeQuery = true)
    int insertEventWithSeq(@Param("chatId") long chatId,
                           @Param("eventType") String eventType,
                           @Param("payload") String payload,
                           @Param("createdAt") Instant createdAt);


    @Query(value = """
        SELECT * FROM chat_events
        WHERE chat_id = :chatId AND seq > :lastSeq
        ORDER BY seq
        LIMIT :limit
        """, nativeQuery = true)
    List<ChatEventDb> findEventsAfter(@Param("chatId") long chatId, @Param("lastSeq") long lastSeq, @Param("limit") int limit);
}
