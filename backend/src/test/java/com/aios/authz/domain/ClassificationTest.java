package com.aios.authz.domain;

import org.junit.jupiter.api.Test;

import static com.aios.authz.domain.Classification.CONFIDENTIAL;
import static com.aios.authz.domain.Classification.INTERNAL;
import static com.aios.authz.domain.Classification.PUBLIC;
import static com.aios.authz.domain.Classification.RESTRICTED;
import static org.assertj.core.api.Assertions.assertThat;

class ClassificationTest {

    @Test
    void ordersFromPublicToRestricted() {
        assertThat(RESTRICTED.atLeast(PUBLIC)).isTrue();
        assertThat(RESTRICTED.atLeast(RESTRICTED)).isTrue();
        assertThat(PUBLIC.atLeast(INTERNAL)).isFalse();
        assertThat(CONFIDENTIAL.atLeast(INTERNAL)).isTrue();
        assertThat(INTERNAL.atLeast(CONFIDENTIAL)).isFalse();
    }

    @Test
    void maxIsCommutativeAssociativeAndIdempotentOverAllPairs() {
        Classification[] all = Classification.values();

        for (Classification a : all) {
            // idempotent
            assertThat(Classification.max(a, a)).isEqualTo(a);

            for (Classification b : all) {
                // commutative
                assertThat(Classification.max(a, b)).isEqualTo(Classification.max(b, a));

                for (Classification c : all) {
                    // associative
                    Classification leftFirst = Classification.max(Classification.max(a, b), c);
                    Classification rightFirst = Classification.max(a, Classification.max(b, c));
                    assertThat(leftFirst).isEqualTo(rightFirst);
                }
            }
        }
    }

    @Test
    void maxReturnsTheMoreSensitiveClassification() {
        assertThat(Classification.max(PUBLIC, RESTRICTED)).isEqualTo(RESTRICTED);
        assertThat(Classification.max(CONFIDENTIAL, INTERNAL)).isEqualTo(CONFIDENTIAL);
    }
}
