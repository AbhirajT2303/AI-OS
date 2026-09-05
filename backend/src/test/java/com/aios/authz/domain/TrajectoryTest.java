package com.aios.authz.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TrajectoryTest {

    private final Principal agent = new Principal("agent-42", PrincipalType.AGENT);
    private final Destination internalLlm =
        new Destination("internal-llm", DestinationKind.MODEL, TrustZone.INTERNAL);

    @Test
    void appendReturnsANewInstanceRatherThanMutating() {
        Trajectory original = Trajectory.empty("wf-1");
        ActionRecord record = allowedRead("act-1");

        Trajectory appended = original.append(record);

        assertThat(original.actions()).isEmpty();
        assertThat(appended.actions()).containsExactly(record);
    }

    @Test
    void recordsDeniedAndAskedActionsNotOnlyAllowedOnes() {
        ActionRecord allowed = allowedRead("act-1");
        ActionRecord denied = new ActionRecord(
            "act-2", agent,
            new Action("act-2", ActionType.SEND_EXTERNAL, "report-123", internalLlm, Set.of(), null),
            Decision.DENY, Instant.now());
        ActionRecord asked = new ActionRecord(
            "act-3", agent,
            new Action("act-3", ActionType.SEND_EXTERNAL, "partner-summary", internalLlm, Set.of(), null),
            Decision.ASK, Instant.now());

        Trajectory trajectory = Trajectory.empty("wf-1").append(allowed).append(denied).append(asked);

        assertThat(trajectory.actions()).extracting(ActionRecord::decision)
            .containsExactly(Decision.ALLOW, Decision.DENY, Decision.ASK);
    }

    private ActionRecord allowedRead(String id) {
        Action read = new Action(id, ActionType.READ, "customer-42", Destination.NONE, Set.of(), "customer-42");
        return new ActionRecord(id, agent, read, Decision.ALLOW, Instant.now());
    }
}
