package com.aios.authz.state;

import com.aios.authz.domain.Action;
import com.aios.authz.domain.ActionRecord;
import com.aios.authz.domain.ActionType;
import com.aios.authz.domain.Classification;
import com.aios.authz.domain.DataAsset;
import com.aios.authz.domain.Decision;
import com.aios.authz.domain.Delegation;
import com.aios.authz.domain.Destination;
import com.aios.authz.domain.DestinationKind;
import com.aios.authz.domain.DerivedData;
import com.aios.authz.domain.Intent;
import com.aios.authz.domain.Principal;
import com.aios.authz.domain.PrincipalType;
import com.aios.authz.domain.Transformation;
import com.aios.authz.domain.TrustZone;
import com.aios.authz.provenance.ProvenanceGraph;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No live Postgres is available in this environment: H2's
 * {@code MODE=PostgreSQL} compatibility mode verifies
 * {@link PostgresAuthorizationStateStore} automatically instead — see
 * docker-compose.yml for the real target and the note on
 * {@code spring.autoconfigure.exclude} in application.properties for why this
 * test has to re-enable JPA auto-configuration just for itself.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=",
    "spring.datasource.url=jdbc:h2:mem:authz-postgres-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PostgresAuthorizationStateStoreTest {

    @Autowired
    private WorkflowStateJpaRepository repository;

    private final Principal agent = new Principal("agent-42", PrincipalType.AGENT);
    private final Intent intent = new Intent("partner-report", "Prepare a report for a partner.");

    private PostgresAuthorizationStateStore newStore() {
        return new PostgresAuthorizationStateStore(repository);
    }

    @Test
    void findReturnsEmptyForAnUnknownWorkflow() {
        assertThat(newStore().find("wf-never-saved")).isEmpty();
    }

    @Test
    void savedStateIsFoundByWorkflowId() {
        PostgresAuthorizationStateStore store = newStore();
        AuthorizationState state = AuthorizationState.open("wf-1", agent, intent);

        store.save(state);

        assertThat(store.find("wf-1")).contains(state);
    }

    @Test
    void savingTwiceForTheSameWorkflowIdUpdatesRatherThanDuplicating() {
        PostgresAuthorizationStateStore store = newStore();
        store.save(AuthorizationState.open("wf-1", agent, intent));

        Action read = new Action(
            "act-1", ActionType.READ, "customer-42", Destination.NONE, Set.of(), "customer-42");
        ActionRecord record = new ActionRecord("act-1", agent, read, Decision.ALLOW, Instant.now());
        DataAsset customer42 = new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db");
        AuthorizationState updated = StateTransition.apply(
            store.find("wf-1").orElseThrow(), record, customer42);
        store.save(updated);

        assertThat(repository.count()).isEqualTo(1);
        assertThat(store.find("wf-1").orElseThrow().heldAssets()).containsExactly(customer42);
    }

    @Test
    void roundTripsTrajectoryDelegationsAndDerivedProvenanceExactly() {
        PostgresAuthorizationStateStore store = newStore();

        DataAsset customer42 = new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db");
        DerivedData summary = new DerivedData(
            "summary-1", Set.of("customer-42"), Classification.PUBLIC, Transformation.SUMMARIZE);
        ProvenanceGraph graph = new ProvenanceGraph().withNode(customer42).withNode(summary);

        Destination internalLlm = new Destination("internal-llm", DestinationKind.MODEL, TrustZone.INTERNAL);
        Action read = new Action(
            "act-1", ActionType.READ, "customer-42", Destination.NONE, Set.of(), "customer-42");
        Action callModel = new Action(
            "act-2", ActionType.CALL_MODEL, "customer-42", internalLlm, Set.of("customer-42"), "summary-1");
        var trajectory = com.aios.authz.domain.Trajectory.empty("wf-1")
            .append(new ActionRecord("act-1", agent, read, Decision.ALLOW, Instant.now()))
            .append(new ActionRecord("act-2", agent, callModel, Decision.ALLOW, Instant.now()))
            .appendDelegation(new Delegation("agent-A", "agent-B", Set.of("customer-42"), Instant.now()));

        AuthorizationState state = new AuthorizationState("wf-1", agent, intent, trajectory, graph);
        store.save(state);

        AuthorizationState reloaded = store.find("wf-1").orElseThrow();

        assertThat(reloaded.trajectory().actions()).hasSize(2);
        assertThat(reloaded.trajectory().delegations()).hasSize(1);
        assertThat(reloaded.provenanceGraph().effectiveClassification("summary-1"))
            .isEqualTo(Classification.CONFIDENTIAL);
        assertThat(reloaded.heldAssets()).containsExactly(customer42);
    }
}
