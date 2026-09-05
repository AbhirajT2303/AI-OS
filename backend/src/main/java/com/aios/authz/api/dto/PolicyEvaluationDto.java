package com.aios.authz.api.dto;

import java.util.List;
import java.util.Set;

public record PolicyEvaluationDto(
        String policyId,
        String decision,
        String reason,
        Set<String> contributingDataIds,
        List<String> contributingActionIds) {
}
