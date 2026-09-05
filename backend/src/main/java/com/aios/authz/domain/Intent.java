package com.aios.authz.domain;

/**
 * The declared objective of a workflow (e.g. {@code prepare_partner_report}),
 * fixed once the workflow opens and immutable for its lifetime.
 *
 * <p>Self-declared by the caller and therefore attacker-influenceable via prompt
 * injection — policies must not treat it as trusted. See docs/DOMAIN_MODEL.md §1
 * and 1H attack ATK-08.
 */
public record Intent(String id, String description) {

    public Intent {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Intent id must not be blank");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Intent description must not be blank");
        }
    }
}
