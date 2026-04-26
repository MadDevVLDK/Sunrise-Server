package com.sunrise.web.api.request;

import com.sunrise.web.api.annotation.ValidId;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

@lombok.Getter
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class GetUserProfilesByIds {
    @NotNull(message = "userIds is required")
    @NotEmpty(message = "userIds cannot be empty")
    @Size(max = 100, message = "Cannot get more than 100 userIds in one request")
    private Set<@ValidId Long> userIds;
}
