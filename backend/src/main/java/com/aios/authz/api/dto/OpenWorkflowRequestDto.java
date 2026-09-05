package com.aios.authz.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record OpenWorkflowRequestDto(
        @NotNull @Valid PrincipalDto principal,
        @NotNull @Valid IntentDto intent) {
}
