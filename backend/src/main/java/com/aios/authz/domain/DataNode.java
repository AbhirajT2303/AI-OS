package com.aios.authz.domain;

import java.util.Set;

/**
 * Source data ({@link DataAsset}) and information derived from it
 * ({@link DerivedData}) unified into one type so lineage can be walked
 * uniformly by {@code ProvenanceGraph} — see docs/DOMAIN_MODEL.md §3 [GAP-2]
 * and ADR-0003.
 */
public sealed interface DataNode permits DataAsset, DerivedData {

    String id();

    /** The classification declared for this node in isolation, before lineage is considered. */
    Classification declaredClassification();

    /** The ids of the nodes this one was derived from; empty for a source asset. */
    Set<String> derivedFrom();
}
