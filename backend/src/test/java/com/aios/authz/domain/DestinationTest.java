package com.aios.authz.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DestinationTest {

    @Test
    void noneIsInternalAndKindNone() {
        assertThat(Destination.NONE.kind()).isEqualTo(DestinationKind.NONE);
        assertThat(Destination.NONE.trustZone()).isEqualTo(TrustZone.INTERNAL);
    }

    @Test
    void rejectsBlankId() {
        assertThatThrownBy(() -> new Destination("", DestinationKind.MODEL, TrustZone.EXTERNAL))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullKindOrTrustZone() {
        assertThatThrownBy(() -> new Destination("x", null, TrustZone.EXTERNAL))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Destination("x", DestinationKind.MODEL, null))
            .isInstanceOf(NullPointerException.class);
    }
}
