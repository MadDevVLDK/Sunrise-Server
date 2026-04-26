package com.sunrise.dataservice.result;

import java.util.List;

public record UserChatsPageDTO(List<ChatProfileDTO> chats, Long nextCursor) { }
