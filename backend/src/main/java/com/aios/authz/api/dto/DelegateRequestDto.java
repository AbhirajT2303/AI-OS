package com.aios.authz.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record DelegateRequestDto(
        @NotBlank String fromPrincipalId,
        @NotBlank String toPrincipalId,
        @NotEmpty Set<String> transferredDataIds) {
}
