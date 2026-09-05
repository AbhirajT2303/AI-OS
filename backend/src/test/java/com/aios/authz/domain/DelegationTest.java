package com.aios.authz.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DelegationTest {

    @Test
    void rejectsBlankPrincipalIds() {
        assertThatThrownBy(() -> new Delegation("", "agent-B", Set.of("customer-42"), Instant.now()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Delegation("agent-A", "", Set.of("customer-42"), Instant.now()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEmptyTransferredDataIds() {
        assertThatThrownBy(() -> new Delegation("agent-A", "agent-B", Set.of(), Instant.now()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isImmutableAndValueEqual() {
        Instant now = Instant.now();
        Delegation a = new Delegation("agent-A", "agent-B", Set.of("customer-42"), now);
        Delegation b = new Delegation("agent-A", "agent-B", Set.of("customer-42"), now);

        assertThat(a).isEqualTo(b);
        assertThat(a.transferredDataIds()).containsExactly("customer-42");
    }
}
