package com.aios.authz.api.dto;

public record ActionRecordDto(
        String actionId,
        String actorId,
        String actionType,
        String resource,
        String destinationId,
        String decision,
        String timestamp) {
}
