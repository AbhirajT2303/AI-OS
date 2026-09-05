package com.aios.authz.domain;

/** What a {@link Destination} physically is. */
public enum DestinationKind {
    MODEL,
    TOOL,
    DATASTORE,
    EMAIL,
    HTTP,
    AGENT,
    NONE
}
