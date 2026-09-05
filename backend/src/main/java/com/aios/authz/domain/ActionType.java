package com.aios.authz.domain;

/** The kind of operation a proposed {@link Action} represents. */
public enum ActionType {
    READ,
    WRITE,
    CALL_TOOL,
    CALL_MODEL,
    TRANSFORM,
    SEND_EXTERNAL,
    DELEGATE,
    DELETE
}
