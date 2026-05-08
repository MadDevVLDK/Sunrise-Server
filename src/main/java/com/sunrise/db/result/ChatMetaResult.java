package com.sunrise.db.result;

public interface ChatMetaResult {
    Long getChatId();
    Boolean getIsPinned();
    Long getLastMsgId();
    Integer getUnreadCount();
}