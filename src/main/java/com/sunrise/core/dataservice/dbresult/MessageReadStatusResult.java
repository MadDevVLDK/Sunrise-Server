package com.sunrise.core.dataservice.dbresult;

import java.time.LocalDateTime;

public interface MessageReadStatusResult {
    Long getUserId();
    LocalDateTime getReadAt();
}
