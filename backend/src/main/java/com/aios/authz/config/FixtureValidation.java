package com.aios.authz.config;

import java.util.Collection;

/**
 * Shared field-level checks for fixture DTOs. Every check throws
 * {@link IllegalArgumentException}; {@link FixtureLoader} catches deserialization
 * failures (Jackson wraps constructor exceptions as an {@code IOException}
 * subtype) and rethrows them as {@link FixtureLoadException}.
 */
final class FixtureValidation {

    private FixtureValidation() {
    }

    static void requireNonBlank(String value, String subject, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(subject + " is missing required field: " + field);
        }
    }

    static void requireOneOf(String value, Collection<String> allowed, String subject, String field) {
        requireNonBlank(value, subject, field);
        if (!allowed.contains(value)) {
            throw new IllegalArgumentException(
                subject + " has invalid " + field + " '" + value + "'. Expected one of " + allowed);
        }
    }

    static void requireNonEmpty(Collection<?> value, String subject, String field) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(subject + " must declare at least one " + field);
        }
    }
}
