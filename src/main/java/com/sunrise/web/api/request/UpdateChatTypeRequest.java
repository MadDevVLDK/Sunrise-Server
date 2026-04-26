package com.sunrise.web.api.request;

import com.sunrise.dataservice.type.ChatType;
import jakarta.validation.constraints.NotNull;

@lombok.Getter
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class UpdateChatTypeRequest {
    @NotNull(message = "chatType is required")
    private ChatType chatType;
}
