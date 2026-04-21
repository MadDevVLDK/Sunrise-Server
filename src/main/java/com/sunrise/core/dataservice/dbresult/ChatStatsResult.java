package com.sunrise.core.dataservice.dbresult;

public interface ChatStatsResult {
    Integer getTotalMessages();
    Integer getDeletedForAll();
    Boolean getCanDeleteForAll();
}
