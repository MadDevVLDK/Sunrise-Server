package com.sunrise.db.result;

public record ChatSyncInfo(boolean syncIsRequired, Long lastReadMsgByMe, Long lastReadMsgByAnyone) {}