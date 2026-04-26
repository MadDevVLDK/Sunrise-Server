package com.sunrise.dataservice.result;

import java.util.List;

public record MessagesPageDTO(List<UserMessageDTO> messages, Long nextCursor) {}