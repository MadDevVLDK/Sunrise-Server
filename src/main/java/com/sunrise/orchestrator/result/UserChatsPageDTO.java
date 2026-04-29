package com.sunrise.orchestrator.result;

import java.util.List;

public record UserChatsPageDTO(List<ChatProfileDTO> chats, Long nextCursor) { }
