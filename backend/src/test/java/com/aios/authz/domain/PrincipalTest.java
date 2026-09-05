package com.aios.authz.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrincipalTest {

    @Test
    void rejectsBlankId() {
        assertThatThrownBy(() -> new Principal("", PrincipalType.AGENT))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Principal("   ", PrincipalType.AGENT))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullId() {
        assertThatThrownBy(() -> new Principal(null, PrincipalType.AGENT))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullType() {
        assertThatThrownBy(() -> new Principal("agent-42", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void isImmutableAndValueEqual() {
        Principal a = new Principal("agent-42", PrincipalType.AGENT);
        Principal b = new Principal("agent-42", PrincipalType.AGENT);

        assertThat(a).isEqualTo(b);
        assertThat(a.id()).isEqualTo("agent-42");
        assertThat(a.type()).isEqualTo(PrincipalType.AGENT);
    }
}
