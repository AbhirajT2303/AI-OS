package com.aios.authz.domain;

import java.util.Objects;

/** The entity performing an action: a user, an agent, or a service. */
public record Principal(String id, PrincipalType type) {

    public Principal {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Principal id must not be blank");
        }
        Objects.requireNonNull(type, "Principal type must not be null");
    }
}
