package com.aios.authz.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntentTest {

    @Test
    void rejectsBlankId() {
        assertThatThrownBy(() -> new Intent("", "prepare a report"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankDescription() {
        assertThatThrownBy(() -> new Intent("partner-report", ""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isImmutableAndValueEqual() {
        Intent a = new Intent("partner-report", "Prepare a report for an external partner.");
        Intent b = new Intent("partner-report", "Prepare a report for an external partner.");

        assertThat(a).isEqualTo(b);
        assertThat(a.id()).isEqualTo("partner-report");
    }
}
