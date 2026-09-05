package com.aios.authz.state;

import com.aios.authz.domain.ActionRecord;
import com.aios.authz.domain.DataAsset;
import com.aios.authz.domain.DataNode;
import com.aios.authz.domain.Delegation;
import com.aios.authz.domain.DerivedData;
import com.aios.authz.domain.Intent;
import com.aios.authz.domain.Principal;
import com.aios.authz.domain.Trajectory;
import com.aios.authz.provenance.ProvenanceGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * The flat, JSON-friendly shape {@link AuthorizationState} is persisted as
 * (ENG-30). {@code DataNode} is sealed with exactly two permitted types, so
 * splitting the graph's nodes into two concrete typed lists here avoids
 * polymorphic (de)serialization entirely — no Jackson type-discriminator
 * annotations are needed anywhere, and {@code domain} stays exactly as
 * framework-free as ADR-0001 requires.
 */
public record AuthorizationStateSnapshot(
        String workflowId,
        Principal initiator,
        Intent intent,
        List<ActionRecord> actions,
        List<Delegation> delegations,
        List<DataAsset> dataAssetNodes,
        List<DerivedData> derivedDataNodes) {

    public static AuthorizationStateSnapshot from(AuthorizationState state) {
        List<DataAsset> assets = new ArrayList<>();
        List<DerivedData> derived = new ArrayList<>();
        for (DataNode node : state.provenanceGraph().allNodes()) {
            switch (node) {
                case DataAsset asset -> assets.add(asset);
                case DerivedData derivedData -> derived.add(derivedData);
            }
        }
        return new AuthorizationStateSnapshot(
            state.workflowId(),
            state.initiator(),
            state.intent(),
            state.trajectory().actions(),
            state.trajectory().delegations(),
            assets,
            derived);
    }

    public AuthorizationState toAuthorizationState() {
        ProvenanceGraph graph = new ProvenanceGraph();
        for (DataAsset asset : dataAssetNodes) {
            graph = graph.withNode(asset);
        }
        for (DerivedData derivedData : derivedDataNodes) {
            graph = graph.withNode(derivedData);
        }
        Trajectory trajectory = new Trajectory(workflowId, actions, delegations);
        return new AuthorizationState(workflowId, initiator, intent, trajectory, graph);
    }
}
