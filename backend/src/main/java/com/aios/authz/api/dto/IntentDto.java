package com.aios.authz.api.dto;

import jakarta.validation.constraints.NotBlank;

public record IntentDto(@NotBlank String id, @NotBlank String description) {
}
