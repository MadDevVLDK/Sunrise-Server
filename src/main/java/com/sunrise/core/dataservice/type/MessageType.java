package com.sunrise.core.dataservice.type;

public enum MessageType {
    COMMON(true),
    SYSTEM(false);

    private final boolean canSendAttachments;

    MessageType(boolean canSendAttachments) {
        this.canSendAttachments = canSendAttachments;
    }

    public boolean canSendAttachment() { return canSendAttachments; }
}
