package com.aios.authz.engine;

import com.aios.authz.domain.Action;
import com.aios.authz.domain.ActionType;
import com.aios.authz.domain.AuthorizationRequest;
import com.aios.authz.domain.AuthorizationResult;
import com.aios.authz.domain.Decision;
import com.aios.authz.domain.Destination;
import com.aios.authz.domain.DestinationKind;
import com.aios.authz.domain.Intent;
import com.aios.authz.domain.Principal;
import com.aios.authz.domain.PrincipalType;
import com.aios.authz.domain.TrustZone;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RbacBaselineEngineTest {

    private final Principal agent42 = new Principal("agent-42", PrincipalType.AGENT);
    private final Intent partnerReport = new Intent("partner-report", "Prepare a report for a partner.");
    private final Destination partnerEmail =
        new Destination("partner.com", DestinationKind.EMAIL, TrustZone.PARTNER);

    @Test
    void allowsWhenAGrantExistsAndNamesTheMatchedPermission() {
        PermissionCatalog catalog = new PermissionCatalog(List.of(
            new PermissionCatalog.Grant("agent-42", ActionType.SEND_EXTERNAL, "*")));
        RbacBaselineEngine engine = new RbacBaselineEngine(catalog);

        Action send = new Action(
            "act-1", ActionType.SEND_EXTERNAL, "report-123", partnerEmail, Set.of("report-123"), null);
        AuthorizationResult result = engine.authorize(
            new AuthorizationRequest("wf-1", agent42, partnerReport, send));

        assertThat(result.decision()).isEqualTo(Decision.ALLOW);
        assertThat(result.explanation()).contains("agent-42").contains("SEND_EXTERNAL");
    }

    @Test
    void deniesWhenNoGrantExistsAndNamesTheMissingPermission() {
        PermissionCatalog catalog = new PermissionCatalog(List.of());
        RbacBaselineEngine engine = new RbacBaselineEngine(catalog);

        Action send = new Action(
            "act-1", ActionType.SEND_EXTERNAL, "report-123", partnerEmail, Set.of("report-123"), null);
        AuthorizationResult result = engine.authorize(
            new AuthorizationRequest("wf-1", agent42, partnerReport, send));

        assertThat(result.decision()).isEqualTo(Decision.DENY);
        assertThat(result.explanation()).contains("no grant").contains("SEND_EXTERNAL");
    }

    @Test
    void neverReturnsAsk() {
        PermissionCatalog catalog = new PermissionCatalog(List.of());
        RbacBaselineEngine engine = new RbacBaselineEngine(catalog);

        Action send = new Action(
            "act-1", ActionType.SEND_EXTERNAL, "report-123", partnerEmail, Set.of("report-123"), null);
        AuthorizationResult result = engine.authorize(
            new AuthorizationRequest("wf-1", agent42, partnerReport, send));

        assertThat(result.decision()).isIn(Decision.ALLOW, Decision.DENY);
    }
}
