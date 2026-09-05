package com.aios.authz.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String VALID_BODY = """
        {
          "workflowId": "wf-1",
          "principal": { "id": "agent-42", "type": "AGENT" },
          "intent": { "id": "partner-report", "description": "Prepare a report for a partner." },
          "requestedAction": {
            "type": "SEND_EXTERNAL",
            "resource": "report-123",
            "destination": { "id": "partner.com", "kind": "EMAIL", "trustZone": "PARTNER" },
            "inputDataIds": ["report-123"]
          }
        }
        """;

    @Test
    void allowsWhenAgent42SendsExternallyPerFixturePermissions() throws Exception {
        mockMvc.perform(post("/authorize").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.decision").value("ALLOW"))
            .andExpect(jsonPath("$.explanation").value(containsString("agent-42")));
    }

    @Test
    void rejectsBlankWorkflowIdWith400() throws Exception {
        String body = VALID_BODY.replaceFirst("\"wf-1\"", "\"\"");

        mockMvc.perform(post("/authorize").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMissingRequiredNestedFieldWith400() throws Exception {
        String body = VALID_BODY.replaceFirst("\"intent\": \\{[^}]*}", "\"intent\": {}");

        mockMvc.perform(post("/authorize").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void clientSuppliedTrajectoryFieldIsIgnoredNotMerged() throws Exception {
        String bodyWithForgedTrajectory = VALID_BODY.replaceFirst(
            "\\}\\s*$",
            ", \"trajectory\": { \"actions\": [ { \"type\": \"DENY\", \"resource\": \"customer-42\" } ] } }");

        String responseWithout = mockMvc.perform(
                post("/authorize").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String responseWithForged = mockMvc.perform(
                post("/authorize").contentType(MediaType.APPLICATION_JSON).content(bodyWithForgedTrajectory))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(responseWithForged).isEqualTo(responseWithout);
    }
}
