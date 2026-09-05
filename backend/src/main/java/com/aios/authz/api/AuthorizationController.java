package com.aios.authz.api;

import com.aios.authz.api.dto.AuthorizeRequestDto;
import com.aios.authz.api.dto.AuthorizeResponseDto;
import com.aios.authz.api.dto.PolicyEvaluationDto;
import com.aios.authz.domain.Action;
import com.aios.authz.domain.AuthorizationRequest;
import com.aios.authz.domain.AuthorizationResult;
import com.aios.authz.domain.Destination;
import com.aios.authz.domain.Intent;
import com.aios.authz.domain.PolicyEvaluation;
import com.aios.authz.domain.Principal;
import com.aios.authz.engine.AuthorizationEngine;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Authorizes one proposed action.
 *
 * <p>Wired to the {@code @Primary} {@link AuthorizationEngine} bean
 * ({@code TrajectoryAwareEngine} — see {@code EngineConfig}, ENG-21), not
 * {@code RbacBaselineEngine} directly. An unknown {@code workflowId} surfaces
 * as {@code UnknownWorkflowException}, mapped to 404 by
 * {@link GlobalExceptionHandler}.
 */
@RestController
public class AuthorizationController {

    private final AuthorizationEngine engine;

    public AuthorizationController(AuthorizationEngine engine) {
        this.engine = engine;
    }

    @PostMapping("/authorize")
    public AuthorizeResponseDto authorize(@Valid @RequestBody AuthorizeRequestDto request) {
        AuthorizationResult result = engine.authorize(toDomain(request));
        return toDto(result);
    }

    private static AuthorizationRequest toDomain(AuthorizeRequestDto dto) {
        Principal principal = new Principal(dto.principal().id(), dto.principal().type());
        Intent intent = new Intent(dto.intent().id(), dto.intent().description());

        Destination destination = new Destination(
            dto.requestedAction().destination().id(),
            dto.requestedAction().destination().kind(),
            dto.requestedAction().destination().trustZone());

        Set<String> inputDataIds = dto.requestedAction().inputDataIds() == null
            ? Set.of()
            : Set.copyOf(dto.requestedAction().inputDataIds());

        Action action = new Action(
            "act-" + UUID.randomUUID(),
            dto.requestedAction().type(),
            dto.requestedAction().resource(),
            destination,
            inputDataIds,
            dto.requestedAction().outputDataId());

        return new AuthorizationRequest(dto.workflowId(), principal, intent, action);
    }

    private static AuthorizeResponseDto toDto(AuthorizationResult result) {
        List<PolicyEvaluationDto> evaluations = result.evaluations().stream()
            .map(AuthorizationController::toDto)
            .toList();

        return new AuthorizeResponseDto(
            result.decision().name(),
            result.explanation(),
            evaluations,
            result.evaluationTime().toMillis());
    }

    private static PolicyEvaluationDto toDto(PolicyEvaluation evaluation) {
        return new PolicyEvaluationDto(
            evaluation.policyId(),
            evaluation.decision().name(),
            evaluation.reason(),
            evaluation.contributingDataIds(),
            evaluation.contributingActionIds());
    }
}
