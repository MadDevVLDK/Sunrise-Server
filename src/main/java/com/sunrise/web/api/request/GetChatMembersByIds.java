package com.sunrise.web.api.request;

import com.sunrise.web.api.annotation.ValidId;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

@lombok.Getter
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class GetChatMembersByIds {
    @NotNull(message = "members is required")
    @NotEmpty(message = "members cannot be empty")
    @Size(max = 100, message = "Cannot get more than 100 members in one request")
    private Set<@ValidId Long> members;
}
