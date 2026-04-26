package com.sunrise.dataservice.type;

public enum MessageType {
    COMMON(true, true),
    IMMUTABLE(true, false),
    SYSTEM(false, false);

    private final boolean canSendWithAttachments;
    private final boolean canEditOrDelete;

    MessageType(boolean canSendWithAttachments, boolean canEditOrDelete) {
        this.canSendWithAttachments = canSendWithAttachments;
        this.canEditOrDelete = canEditOrDelete;
    }

    public boolean canSendWithAttachments() { return canSendWithAttachments; }
    public boolean canEditOrDelete() { return canEditOrDelete; }
}
