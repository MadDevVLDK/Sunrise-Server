package com.sunrise.db.service;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sunrise.db.entity.ChatEventDb;
import com.sunrise.db.repository.ChatEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Service
@RequiredArgsConstructor
public class ChatEventDbService {
    
    private final ChatEventRepository chatEventRepository;
    private final JdbcTemplate jdbcTemplate;

    @SneakyThrows
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(ChatEventDb event) {
        int inserted = chatEventRepository.insertEventWithSeq(
            event.getChatId(),
            event.getEventType(),
            event.getPayload(),
            event.getCreatedAt()
        );
        if (inserted == 0) {
            throw new RuntimeException("Failed to insert event for chat " + event.getChatId());
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public long saveAndReturnSeq(ChatEventDb event) {
        String sql = """
            WITH new_seq AS (
                INSERT INTO chat_seq (chat_id, current_seq)
                VALUES (?, 1)
                ON CONFLICT (chat_id) DO UPDATE SET current_seq = chat_seq.current_seq + 1
                RETURNING current_seq
            )
            INSERT INTO chat_events (chat_id, seq, event_type, payload, created_at)
            SELECT ?, seq, ?, CAST(? AS jsonb), ?
            FROM new_seq
            RETURNING seq
        """;
        return jdbcTemplate.queryForObject(sql, Long.class,
            event.getChatId(),
            event.getChatId(),
            event.getEventType(),
            event.getPayload(),
            event.getCreatedAt()
        );
    }

    public List<ChatEventDb> getEventsAfter(long chatId, long lastSeq, int limit) {
        return chatEventRepository.findEventsAfter(chatId, lastSeq, limit);
    }
}
