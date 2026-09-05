package com.aios.authz.harness;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScenarioLoaderTest {

    private final ScenarioLoader loader = new ScenarioLoader();

    @Test
    void loadsScenarioAWithItsThreeSteps() {
        Scenario scenario = loader.load("/scenarios/scenario-a-public-data.json");

        assertThat(scenario.id()).isEqualTo("scenario-a-public-data");
        assertThat(scenario.steps()).hasSize(3);
        assertThat(scenario.steps().get(2).expectedTrajectory())
            .isEqualTo(com.aios.authz.domain.Decision.ALLOW);
    }

    @Test
    void loadsScenarioBWhereRbacAllowsButTrajectoryDeniesStep2() {
        Scenario scenario = loader.load("/scenarios/scenario-b-confidential-external-model.json");

        ScenarioStep step2 = scenario.steps().get(1);
        assertThat(step2.expectedRbac()).isEqualTo(com.aios.authz.domain.Decision.ALLOW);
        assertThat(step2.expectedTrajectory()).isEqualTo(com.aios.authz.domain.Decision.DENY);
    }

    @Test
    void missingScenarioFailsLoudly() {
        assertThatThrownBy(() -> loader.load("/scenarios/does-not-exist.json"))
            .isInstanceOf(ScenarioLoadException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void malformedActionTypeFailsLoudly() {
        assertThatThrownBy(() -> loader.load("/scenarios/malformed-scenario.json"))
            .isInstanceOf(ScenarioLoadException.class);
    }
}
