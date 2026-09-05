package com.aios.authz.api.dto;

import com.aios.authz.domain.DestinationKind;
import com.aios.authz.domain.TrustZone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DestinationDto(@NotBlank String id, @NotNull DestinationKind kind, @NotNull TrustZone trustZone) {
}
