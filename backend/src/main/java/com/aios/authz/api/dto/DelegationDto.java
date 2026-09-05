package com.aios.authz.api.dto;

import java.util.Set;

public record DelegationDto(
        String fromPrincipalId,
        String toPrincipalId,
        Set<String> transferredDataIds,
        String timestamp) {
}
