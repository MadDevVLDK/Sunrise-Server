package com.sunrise.core.dataservice.type;

public enum ChatType {
    PERSONAL(2, 2),
    BIG_GROUP(1, 1000);

    private final int minMemberCount;
    private final int maxMemberCount;

    ChatType(int minMemberCount, int maxMemberCount) {
        this.minMemberCount = minMemberCount;
        this.maxMemberCount = maxMemberCount;
    }

    public boolean isMembersInBound(int memberCount) { return minMemberCount <= memberCount && memberCount <= maxMemberCount; }
    public boolean isPersonal() { return this == PERSONAL; }
    public boolean isNotPersonal() { return this != PERSONAL; }
    public boolean isActionsEnabled(int memberCount) { return memberCount <= 50; } // только в больших группах отключаем действия
}