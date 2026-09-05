package com.aios.authz.api;

import com.aios.authz.api.dto.OpenWorkflowRequestDto;
import com.aios.authz.api.dto.OpenWorkflowResponseDto;
import com.aios.authz.domain.Intent;
import com.aios.authz.domain.Principal;
import com.aios.authz.state.WorkflowManager;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Opens a workflow: a principal and a declared intent, nothing else yet. */
@RestController
public class WorkflowController {

    private final WorkflowManager workflowManager;

    public WorkflowController(WorkflowManager workflowManager) {
        this.workflowManager = workflowManager;
    }

    @PostMapping("/workflows")
    public OpenWorkflowResponseDto open(@Valid @RequestBody OpenWorkflowRequestDto request) {
        Principal principal = new Principal(request.principal().id(), request.principal().type());
        Intent intent = new Intent(request.intent().id(), request.intent().description());
        String workflowId = workflowManager.open(principal, intent);
        return new OpenWorkflowResponseDto(workflowId);
    }
}
