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
    void rejectsBlankWorkflowIdWith400AsAProblemDetail() throws Exception {
        String body = VALID_BODY.replaceFirst("\"wf-1\"", "\"\"");

        mockMvc.perform(post("/authorize").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation failed"))
            .andExpect(jsonPath("$.detail").value(containsString("workflowId")));
    }

    @Test
    void rejectsBlankOutputDataIdWith400ViaTheDomainConstructorNotBeanValidation() throws Exception {
        // outputDataId has no bean-validation annotation on the DTO (it's
        // optional) — a blank-but-non-null value passes DTO validation and only
        // fails in domain.Action's own compact constructor. This exercises the
        // IllegalArgumentException path of GlobalExceptionHandler, distinct from
        // the MethodArgumentNotValidException path above.
        String body = VALID_BODY.replaceFirst(
            "\"inputDataIds\": \\[\"report-123\"\\]",
            "\"inputDataIds\": [\"report-123\"], \"outputDataId\": \" \"");

        mockMvc.perform(post("/authorize").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid request"))
            .andExpect(jsonPath("$.detail").value(containsString("outputDataId")));
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

        // evaluationTimeMs is real wall-clock timing and will legitimately
        // differ between the two calls; normalize it out before comparing so
        // the assertion is about the decision, not measurement noise.
        assertThat(withoutTiming(responseWithForged)).isEqualTo(withoutTiming(responseWithout));
    }

    private static String withoutTiming(String responseBody) {
        return responseBody.replaceFirst("\"evaluationTimeMs\":\\d+", "\"evaluationTimeMs\":0");
    }
}
