package com.aios.authz.domain;

import java.util.Objects;
import java.util.Set;

/** A source of information: something read from an enterprise system, not derived. */
public record DataAsset(String id, Classification declaredClassification, String source)
        implements DataNode {

    public DataAsset {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("DataAsset id must not be blank");
        }
        Objects.requireNonNull(declaredClassification, "DataAsset classification must not be null");
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("DataAsset source must not be blank");
        }
    }

    @Override
    public Set<String> derivedFrom() {
        return Set.of();
    }
}
