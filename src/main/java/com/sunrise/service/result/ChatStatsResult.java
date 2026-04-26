package com.sunrise.service.result;

public record ChatStatsResult(int totalMessages, int deletedForAll, boolean canDeleteForAll) { }