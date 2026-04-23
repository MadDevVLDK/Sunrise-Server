package com.sunrise.controller.request;

import jakarta.validation.constraints.NotNull;

@lombok.Getter
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class UpdateSelfChatSettingsRequest {
    @NotNull(message = "isPinned is required")
    public Boolean isPinned;
}
