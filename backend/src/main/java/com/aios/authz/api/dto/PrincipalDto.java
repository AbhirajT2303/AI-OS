package com.aios.authz.api.dto;

import com.aios.authz.domain.PrincipalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PrincipalDto(@NotBlank String id, @NotNull PrincipalType type) {
}
