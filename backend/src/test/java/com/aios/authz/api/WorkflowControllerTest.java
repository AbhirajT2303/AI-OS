package com.aios.authz.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
}
