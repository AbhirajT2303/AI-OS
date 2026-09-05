package com.aios.authz.api;

import com.aios.authz.api.dto.ActionRecordDto;
import com.aios.authz.api.dto.IntentDto;
import com.aios.authz.api.dto.OpenWorkflowRequestDto;
import com.aios.authz.api.dto.OpenWorkflowResponseDto;
import com.aios.authz.api.dto.PrincipalDto;
import com.aios.authz.api.dto.ProvenanceNodeDto;
import com.aios.authz.api.dto.WorkflowStateDto;
import com.aios.authz.domain.ActionRecord;
import com.aios.authz.domain.Intent;
import com.aios.authz.domain.Principal;
import com.aios.authz.state.AuthorizationState;
import com.aios.authz.state.UnknownWorkflowException;
import com.aios.authz.state.WorkflowManager;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Opens a workflow, and lets it be inspected — for debugging and 1H, not a UI. */
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

    @GetMapping("/workflows/{workflowId}")
    public WorkflowStateDto get(@PathVariable String workflowId) {
        AuthorizationState state = workflowManager.find(workflowId)
            .orElseThrow(() -> new UnknownWorkflowException(workflowId));
        return toDto(state);
    }

    private static WorkflowStateDto toDto(AuthorizationState state) {
        List<ActionRecordDto> trajectory = state.trajectory().actions().stream()
            .map(WorkflowController::toDto)
            .toList();

        Set<String> nodeIds = new LinkedHashSet<>();
        for (ActionRecord record : state.trajectory().actions()) {
            nodeIds.add(record.action().resource());
            if (record.action().outputDataId() != null) {
                nodeIds.add(record.action().outputDataId());
            }
        }
        List<ProvenanceNodeDto> provenance = nodeIds.stream()
            .map(nodeId -> state.provenanceGraph().find(nodeId)
                .map(node -> new ProvenanceNodeDto(
                    nodeId, state.provenanceGraph().effectiveClassification(nodeId).name()))
                .orElse(null))
            .filter(Objects::nonNull)
            .toList();

        return new WorkflowStateDto(
            state.workflowId(),
            new PrincipalDto(state.initiator().id(), state.initiator().type()),
            new IntentDto(state.intent().id(), state.intent().description()),
            trajectory,
            provenance);
    }

    private static ActionRecordDto toDto(ActionRecord record) {
        return new ActionRecordDto(
            record.action().id(),
            record.actor().id(),
            record.action().type().name(),
            record.action().resource(),
            record.action().destination().id(),
            record.decision().name(),
            record.timestamp().toString());
    }
}
