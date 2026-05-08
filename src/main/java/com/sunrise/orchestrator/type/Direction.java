package com.sunrise.orchestrator.type;

public enum Direction {
    FORWARD,   // после указанного ID (новые сообщения)
    BACKWARD,  // до указанного ID (старые сообщения)
    RANGE      // сообщения от cursor до endCursor (не включительно)
}
