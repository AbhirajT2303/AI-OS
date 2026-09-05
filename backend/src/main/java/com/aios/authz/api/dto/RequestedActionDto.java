package com.aios.authz.api.dto;

import com.aios.authz.domain.ActionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * No {@code id} field: the caller proposes an action, the server assigns it an
 * id when converting to {@code domain.Action} for trajectory tracking.
 */
public record RequestedActionDto(
        @NotNull ActionType type,
        @NotBlank String resource,
        @NotNull @Valid DestinationDto destination,
        List<String> inputDataIds,
        String outputDataId) {
}
