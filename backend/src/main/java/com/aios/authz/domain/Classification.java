package com.aios.authz.domain;

/**
 * A total order over sensitivity: {@code PUBLIC < INTERNAL < CONFIDENTIAL < RESTRICTED}.
 *
 * <p>This is a deliberate simplification. Real enterprises use lattices with
 * incomparable compartments (e.g. {@code PII} vs {@code TRADE_SECRET}); a total
 * order is sufficient for 1G — see ADR-0003 and docs/DOMAIN_MODEL.md §10.
 *
 * <p>{@link #max} is the primitive the whole thesis rests on: derived information
 * carries the least upper bound of the classifications it was derived from.
 */
public enum Classification {

    PUBLIC(0),
    INTERNAL(1),
    CONFIDENTIAL(2),
    RESTRICTED(3);

    private final int level;

    Classification(int level) {
        this.level = level;
    }

    /** True if this classification is at least as sensitive as {@code other}. */
    public boolean atLeast(Classification other) {
        return this.level >= other.level;
    }

    /** The more sensitive of the two — the least upper bound over this total order. */
    public static Classification max(Classification a, Classification b) {
        return a.level >= b.level ? a : b;
    }
}
