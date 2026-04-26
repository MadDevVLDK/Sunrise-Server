package com.sunrise.db.result;

public interface ChatStatsResult {
    Integer getTotalMessages();
    Integer getDeletedForAll();
    Boolean getCanDeleteForAll();
}
