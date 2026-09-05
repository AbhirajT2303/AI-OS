package com.aios.authz.domain;

import java.util.Objects;
import java.util.Set;

/**
 * Information produced by transforming one or more other {@link DataNode}s.
 * {@code declaredClassification} is this node's classification in isolation —
 * usually {@code PUBLIC}; the real answer comes from
 * {@code ProvenanceGraph.effectiveClassification}, the least upper bound over
 * the transitive closure of {@code derivedFrom}.
 */
public record DerivedData(
        String id,
        Set<String> derivedFrom,
        Classification declaredClassification,
        Transformation transformation)
        implements DataNode {

    public DerivedData {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("DerivedData id must not be blank");
        }
        if (derivedFrom == null || derivedFrom.isEmpty()) {
            throw new IllegalArgumentException(
                "DerivedData must declare at least one source in derivedFrom");
        }
        derivedFrom = Set.copyOf(derivedFrom);
        Objects.requireNonNull(declaredClassification, "DerivedData classification must not be null");
        Objects.requireNonNull(transformation, "DerivedData transformation must not be null");
    }
}
