package com.aios.authz.api.dto;

import java.util.List;

public record AuthorizeResponseDto(
        String decision,
        String explanation,
        List<PolicyEvaluationDto> evaluations,
        long evaluationTimeMs) {
}
