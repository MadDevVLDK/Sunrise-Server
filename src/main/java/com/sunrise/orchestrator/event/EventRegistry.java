package com.sunrise.orchestrator.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class EventRegistry {

    private final ObjectMapper objectMapper;
    private final Map<EventType, Class<? extends IDomainEvent>> eventTypeRegistry;

    public EventRegistry() {
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.eventTypeRegistry = new EnumMap<>(EventType.class);
        
        // USER EVENTS
        eventTypeRegistry.put(EventType.USER_CHAT_CREATED, IDomainEvent.UserChatCreated.class);
        eventTypeRegistry.put(EventType.USER_CHAT_ADDED, IDomainEvent.UserChatAdded.class);
        eventTypeRegistry.put(EventType.USER_CHAT_REMOVED, IDomainEvent.UserChatRemoved.class);
        eventTypeRegistry.put(EventType.USER_CHAT_DELETED, IDomainEvent.UserChatDeleted.class);
        eventTypeRegistry.put(EventType.USER_CHAT_SETTINGS_CHANGED, IDomainEvent.UserChatSettingsChanged.class);
        eventTypeRegistry.put(EventType.USER_CHAT_MESSAGE_SENT, IDomainEvent.UserChatMessageSent.class);

        // CHAT EVENTS
        eventTypeRegistry.put(EventType.CHAT_CREATED, IDomainEvent.ChatCreated.class);
        eventTypeRegistry.put(EventType.CHAT_UPDATED, IDomainEvent.ChatUpdated.class);
        eventTypeRegistry.put(EventType.CHAT_DELETED, IDomainEvent.ChatDeleted.class);

        // MESSAGE EVENTS
        eventTypeRegistry.put(EventType.MESSAGE_CREATED, IDomainEvent.MessageCreated.class);
        eventTypeRegistry.put(EventType.MESSAGE_UPDATED, IDomainEvent.MessageUpdated.class);
        eventTypeRegistry.put(EventType.MESSAGE_DELETED, IDomainEvent.MessageDeleted.class);
        eventTypeRegistry.put(EventType.MESSAGES_READ_UP_TO, IDomainEvent.MessagesReadUpTo.class);
        
        // CHAT MEMBER EVENTS
        eventTypeRegistry.put(EventType.CHAT_MEMBER_ADDED, IDomainEvent.ChatMemberAdded.class);
        eventTypeRegistry.put(EventType.CHAT_MEMBERS_ADDED, IDomainEvent.ChatMembersAdded.class);
        eventTypeRegistry.put(EventType.CHAT_MEMBER_INFO_UPDATE, IDomainEvent.ChatMemberInfoUpdate.class);
        eventTypeRegistry.put(EventType.CHAT_MEMBER_REMOVED, IDomainEvent.ChatMemberRemoved.class);
        eventTypeRegistry.put(EventType.CHAT_MEMBER_ADMIN_UPDATED, IDomainEvent.ChatMemberAdminUpdated.class);
    }

    public IDomainEvent deserialize(EventType type, String payload) {
        Class<? extends IDomainEvent> eventClass = eventTypeRegistry.get(type);
        if (eventClass == null) {
            throw new IllegalArgumentException("Unknown event type: " + type);
        }
        
        try {
            return objectMapper.readValue(payload, eventClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event of type " + type, e);
        }
    }

    public String serialize(IDomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    public EventType getEventType(IDomainEvent event) {
        Class<?> eventClass = event.getClass();
        for (Map.Entry<EventType, Class<? extends IDomainEvent>> entry : eventTypeRegistry.entrySet()) {
            if (entry.getValue().equals(eventClass)) {
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException("Unknown event class: " + eventClass);
    }
}