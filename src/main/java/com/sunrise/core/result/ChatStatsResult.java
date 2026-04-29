package com.sunrise.core.result;

public record ChatStatsResult(int totalMessages, int deletedForAll, boolean canDeleteForAll) { }