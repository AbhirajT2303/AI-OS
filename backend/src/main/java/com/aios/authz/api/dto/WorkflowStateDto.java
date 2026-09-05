package com.aios.authz.api.dto;

import java.util.List;

/** For debugging and 1H, not a UI — see ENG-38. */
public record WorkflowStateDto(
        String workflowId,
        PrincipalDto initiator,
        IntentDto intent,
        List<ActionRecordDto> trajectory,
        List<ProvenanceNodeDto> provenance) {
}
