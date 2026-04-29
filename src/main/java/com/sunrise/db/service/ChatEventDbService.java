package com.sunrise.db.service;

import java.util.List;

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

    public List<ChatEventDb> getEventsAfter(long chatId, long lastSeq, int limit) {
        return chatEventRepository.findEventsAfter(chatId, lastSeq, limit);
    }
}
