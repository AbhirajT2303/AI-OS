package com.aios.authz.domain;

/**
 * How a {@link DerivedData} node was produced from its sources. Recorded for
 * lineage but does not by itself alter classification in 1G — whether any
 * transformation should legitimately declassify (e.g. aggregation over a large
 * enough population) is an open research question, deliberately deferred rather
 * than assumed in the thesis's own favour. See docs/DOMAIN_MODEL.md §3 and §10.
 */
public enum Transformation {
    SUMMARIZE,
    AGGREGATE,
    TRANSLATE,
    CLASSIFY,
    EMBED,
    GENERATE,
    COPY
}
