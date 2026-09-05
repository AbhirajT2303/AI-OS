package com.aios.authz.state;

import com.aios.authz.domain.Action;
import com.aios.authz.domain.ActionRecord;
import com.aios.authz.domain.ActionType;
import com.aios.authz.domain.Classification;
import com.aios.authz.domain.DataAsset;
import com.aios.authz.domain.Decision;
import com.aios.authz.domain.DerivedData;
import com.aios.authz.domain.Transformation;
import com.aios.authz.provenance.ProvenanceGraph;

/**
 * The pure state transition: {@code apply(state, record, resolvedAsset) -> state'}.
 * No I/O, no clock reads — {@code record} already carries its timestamp, and any
 * data lookup happens before this is called, not inside it. This is what makes
 * a transition deterministic and safe to property-test.
 *
 * <p>Only an ALLOWed action extends the provenance graph, two ways:
 * <ul>
 *   <li>a READ of a known asset ({@code resolvedAsset} non-null) registers it
 *       as a root node;
 *   <li>any other action with an {@code outputDataId} and at least one
 *       {@code inputDataId} registers a {@link DerivedData} node linking the
 *       output to its inputs — this is what lets {@code ProvenanceBoundaryPolicy}
 *       (ENG-25) reason about lineage rather than just "was anything
 *       confidential ever read".
 * </ul>
 * An output with no declared inputs has nothing to derive a lineage from and is
 * not added — a known, accepted gap rather than a forced, meaningless
 * derivation.
 */
public final class StateTransition {

    private StateTransition() {
    }

    /**
     * @param resolvedAsset the {@link DataAsset} this action reads, if it is an
     *                       allowed READ of a known asset; {@code null} otherwise.
     */
    public static AuthorizationState apply(
            AuthorizationState state, ActionRecord record, DataAsset resolvedAsset) {
        var nextTrajectory = state.trajectory().append(record);

        if (record.decision() != Decision.ALLOW) {
            return new AuthorizationState(
                state.workflowId(), state.initiator(), state.intent(), nextTrajectory, state.provenanceGraph());
        }

        ProvenanceGraph nextGraph = extendGraph(state.provenanceGraph(), record.action(), resolvedAsset);
        return new AuthorizationState(
            state.workflowId(), state.initiator(), state.intent(), nextTrajectory, nextGraph);
    }

    private static ProvenanceGraph extendGraph(ProvenanceGraph graph, Action action, DataAsset resolvedAsset) {
        if (action.type() == ActionType.READ) {
            return resolvedAsset == null ? graph : graph.withNode(resolvedAsset);
        }
        if (action.outputDataId() != null && !action.inputDataIds().isEmpty()) {
            DerivedData derived = new DerivedData(
                action.outputDataId(), action.inputDataIds(), Classification.PUBLIC, transformationFor(action.type()));
            return graph.withNode(derived);
        }
        return graph;
    }

    /**
     * A simple, fixed default per action type. This is metadata only —
     * {@link com.aios.authz.domain.DataNode#derivedFrom()}, not
     * {@code Transformation}, is what {@code ProvenanceGraph} uses to compute
     * classification — so the specific value chosen here never affects a
     * decision.
     */
    private static Transformation transformationFor(ActionType actionType) {
        return switch (actionType) {
            case TRANSFORM -> Transformation.AGGREGATE;
            case CALL_MODEL, CALL_TOOL, WRITE -> Transformation.GENERATE;
            default -> Transformation.COPY;
        };
    }
}
