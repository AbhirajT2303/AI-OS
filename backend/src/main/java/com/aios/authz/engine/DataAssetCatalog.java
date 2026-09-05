package com.aios.authz.engine;

import com.aios.authz.domain.DataAsset;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Resolves a resource id to the known {@link DataAsset} for it, so
 * {@code TrajectoryAwareEngine} can populate {@code AuthorizationState.heldAssets}
 * when a READ succeeds. Mirrors {@link PermissionCatalog}'s role for RBAC.
 */
public final class DataAssetCatalog {

    private final Map<String, DataAsset> assetsById;

    public DataAssetCatalog(List<DataAsset> assets) {
        Objects.requireNonNull(assets, "assets must not be null");
        this.assetsById = assets.stream().collect(
            java.util.stream.Collectors.toUnmodifiableMap(DataAsset::id, Function.identity()));
    }

    public Optional<DataAsset> find(String resourceId) {
        return Optional.ofNullable(assetsById.get(resourceId));
    }
}
