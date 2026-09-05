package com.aios.authz.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * No trajectory field, by design — see {@code domain.AuthorizationRequest}. A
 * client that includes one in the JSON body has it silently dropped by
 * deserialization rather than merged into the decision; see
 * {@code AuthorizationControllerTest.clientSuppliedTrajectoryFieldIsIgnoredNotMerged}.
 */
public record AuthorizeRequestDto(
        @NotBlank String workflowId,
        @NotNull @Valid PrincipalDto principal,
        @NotNull @Valid IntentDto intent,
        @NotNull @Valid RequestedActionDto requestedAction) {
}
