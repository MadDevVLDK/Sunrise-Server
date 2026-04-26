package com.sunrise.web.api.request;

import com.sunrise.web.api.annotation.ValidId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class PrivateMessageRequest {
    @ValidId
    private Long tempId;

    @NotBlank
    @Size(max = 10000)
    private String text;

    @ValidId
    private Long userToSendId;
}