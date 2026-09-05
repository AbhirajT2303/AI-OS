package com.aios.authz.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskBudgetTest {

    @Test
    void remainingIsTotalMinusSpent() {
        RiskBudget budget = new RiskBudget(100, 30);

        assertThat(budget.remaining()).isEqualTo(70);
    }

    @Test
    void rejectsNegativeTotalOrSpent() {
        assertThatThrownBy(() -> new RiskBudget(-1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RiskBudget(100, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
