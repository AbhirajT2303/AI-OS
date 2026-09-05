package com.aios.authz.api;

import com.aios.authz.domain.Intent;
import com.aios.authz.domain.Principal;
import com.aios.authz.domain.PrincipalType;
import com.aios.authz.state.WorkflowManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkflowManager workflowManager;

    @Test
    void opensAWorkflowAndReturnsItsId() throws Exception {
        String body = """
            {
              "principal": { "id": "agent-42", "type": "AGENT" },
              "intent": { "id": "partner-report", "description": "Prepare a report for a partner." }
            }
            """;

        mockMvc.perform(post("/workflows").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workflowId").isNotEmpty());
    }

    @Test
    void rejectsMissingIntentWith400() throws Exception {
        String body = """
            {
              "principal": { "id": "agent-42", "type": "AGENT" }
            }
            """;

        mockMvc.perform(post("/workflows").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getReturnsTheFreshlyOpenedWorkflowsStateWithAnEmptyTrajectory() throws Exception {
        String workflowId = workflowManager.open(
            new Principal("agent-42", PrincipalType.AGENT),
            new Intent("partner-report", "Prepare a report for a partner."));

        mockMvc.perform(get("/workflows/{workflowId}", workflowId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workflowId").value(workflowId))
            .andExpect(jsonPath("$.initiator.id").value("agent-42"))
            .andExpect(jsonPath("$.intent.id").value("partner-report"))
            .andExpect(jsonPath("$.trajectory").isEmpty())
            .andExpect(jsonPath("$.delegations").isEmpty())
            .andExpect(jsonPath("$.provenance").isEmpty());
    }

    @Test
    void getRejectsAnUnknownWorkflowIdWith404() throws Exception {
        mockMvc.perform(get("/workflows/{workflowId}", "wf-never-opened"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Unknown workflow"));
    }

    @Test
    void delegateRejectsAnUnknownWorkflowIdWith404() throws Exception {
        String body = """
            { "fromPrincipalId": "agent-A", "toPrincipalId": "agent-B", "transferredDataIds": ["customer-42"] }
            """;

        mockMvc.perform(post("/workflows/{workflowId}/delegate", "wf-never-opened")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isNotFound());
    }

    @Test
    void delegateRejectsDataTheWorkflowDoesNotHoldWith400() throws Exception {
        String workflowId = workflowManager.open(
            new Principal("agent-42", PrincipalType.AGENT),
            new Intent("partner-report", "Prepare a report for a partner."));
        String body = """
            { "fromPrincipalId": "agent-A", "toPrincipalId": "agent-B", "transferredDataIds": ["customer-42"] }
            """;

        mockMvc.perform(post("/workflows/{workflowId}/delegate", workflowId)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("customer-42")));
    }

    @Test
    void delegateSucceedsAfterTheDataHasBeenReadIntoTheWorkflow() throws Exception {
        String workflowId = workflowManager.open(
            new Principal("agent-42", PrincipalType.AGENT),
            new Intent("partner-report", "Prepare a report for a partner."));

        String authorizeBody = """
            {
              "workflowId": "%s",
              "principal": { "id": "agent-42", "type": "AGENT" },
              "intent": { "id": "partner-report", "description": "Prepare a report for a partner." },
              "requestedAction": {
                "type": "READ",
                "resource": "customer-42",
                "destination": { "id": "none", "kind": "NONE", "trustZone": "INTERNAL" },
                "outputDataId": "customer-42"
              }
            }
            """.formatted(workflowId);
        mockMvc.perform(post("/authorize").contentType(MediaType.APPLICATION_JSON).content(authorizeBody))
            .andExpect(status().isOk());

        String delegateBody = """
            { "fromPrincipalId": "agent-A", "toPrincipalId": "agent-B", "transferredDataIds": ["customer-42"] }
            """;
        mockMvc.perform(post("/workflows/{workflowId}/delegate", workflowId)
                .contentType(MediaType.APPLICATION_JSON).content(delegateBody))
            .andExpect(status().isOk());

        mockMvc.perform(get("/workflows/{workflowId}", workflowId))
            .andExpect(jsonPath("$.trajectory.length()").value(1))
            .andExpect(jsonPath("$.delegations.length()").value(1))
            .andExpect(jsonPath("$.delegations[0].transferredDataIds[0]").value("customer-42"));
    }
}
