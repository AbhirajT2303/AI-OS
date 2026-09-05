package com.aios.authz.state;

import com.aios.authz.domain.Intent;
import com.aios.authz.domain.Principal;
import com.aios.authz.domain.PrincipalType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationStateTest {

    private final Principal agent = new Principal("agent-42", PrincipalType.AGENT);
    private final Intent intent = new Intent("partner-report", "Prepare a report for a partner.");

    @Test
    void openStateHasNoActionsAndNoHeldAssets() {
        AuthorizationState state = AuthorizationState.open("wf-1", agent, intent);

        assertThat(state.trajectory().actions()).isEmpty();
        assertThat(state.heldAssets()).isEmpty();
        assertThat(state.trajectory().workflowId()).isEqualTo("wf-1");
    }

    @Test
    void nullHeldAssetsDefaultsToEmptyNotNull() {
        AuthorizationState state = new AuthorizationState(
            "wf-1", agent, intent, com.aios.authz.domain.Trajectory.empty("wf-1"), null);

        assertThat(state.heldAssets()).isNotNull().isEmpty();
    }

    @Test
    void rejectsBlankWorkflowId() {
        assertThatThrownBy(() -> AuthorizationState.open("", agent, intent))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
