package com.aios.authz.domain;

import java.util.Objects;
import java.util.Set;

/**
 * A proposed operation. {@code destination} is never null — actions with no
 * external effect pass {@link Destination#NONE} explicitly, so "no destination"
 * can never be confused with "forgot to set it".
 *
 * <p>{@code inputDataIds} names the data nodes this action consumes;
 * {@code outputDataId}, if present, is the data node it produces. These are what
 * let the engine extend the provenance graph as a side effect of authorizing an
 * action, rather than requiring the caller to declare lineage itself — see
 * docs/DOMAIN_MODEL.md §4.
 */
public record Action(
        String id,
        ActionType type,
        String resource,
        Destination destination,
        Set<String> inputDataIds,
        String outputDataId) {

    public Action {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Action id must not be blank");
        }
        Objects.requireNonNull(type, "Action type must not be null");
        if (resource == null || resource.isBlank()) {
            throw new IllegalArgumentException("Action resource must not be blank");
        }
        Objects.requireNonNull(destination,
            "Action destination must not be null — pass Destination.NONE explicitly");
        inputDataIds = inputDataIds == null ? Set.of() : Set.copyOf(inputDataIds);
        if (outputDataId != null && outputDataId.isBlank()) {
            throw new IllegalArgumentException("Action outputDataId must not be blank if present");
        }
    }
}
