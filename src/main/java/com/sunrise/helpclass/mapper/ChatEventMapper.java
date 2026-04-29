package com.sunrise.helpclass.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sunrise.db.entity.ChatEventDb;
import com.sunrise.db.event.ChatEvent;

import lombok.SneakyThrows;

public class ChatEventMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @SneakyThrows
    public static ChatEventDb toEntity(ChatEvent.IChatEvent domainEvent) {
        ChatEventDb entity = new ChatEventDb();
        entity.setChatId(domainEvent.getChatId());
        entity.setEventType(domainEvent.getClass().getSimpleName());
        entity.setPayload(objectMapper.writeValueAsString(domainEvent));
        entity.setCreatedAt(domainEvent.getCreatedAtDb());
        return entity;
    }

    // TODO: Я БЕЗ ПОНЯТИЯ КАК ПО-ДРУГОМУ СДЕЛАТЬ
    @SneakyThrows
    public static ChatEvent.IChatEvent toDomain(ChatEventDb entity) {
        String className = "com.sunrise.db.event.ChatEvent$" + entity.getEventType();
        return (ChatEvent.IChatEvent) objectMapper.readValue(entity.getPayload(), Class.forName(className));
    }
}