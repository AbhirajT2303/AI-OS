package com.aios.authz.state;

import com.aios.authz.domain.ActionRecord;
import com.aios.authz.domain.ActionType;
import com.aios.authz.domain.DataAsset;
import com.aios.authz.domain.Decision;

import java.util.HashSet;
import java.util.Set;

/**
 * The pure state transition: {@code apply(state, record, resolvedAsset) -> state'}.
 * No I/O, no clock reads — {@code record} already carries its timestamp, and any
 * data lookup happens before this is called, not inside it. This is what makes
 * a transition deterministic and safe to property-test.
 */
public final class StateTransition {

    private StateTransition() {
    }

    /**
     * @param resolvedAsset the {@link DataAsset} this action reads, if it is an
     *                       allowed READ of a known asset; {@code null} otherwise.
     *                       Ignored for every other action type or decision.
     */
    public static AuthorizationState apply(
            AuthorizationState state, ActionRecord record, DataAsset resolvedAsset) {
        var nextTrajectory = state.trajectory().append(record);

        if (record.decision() == Decision.ALLOW
                && record.action().type() == ActionType.READ
                && resolvedAsset != null) {
            Set<DataAsset> nextHeld = new HashSet<>(state.heldAssets());
            nextHeld.add(resolvedAsset);
            return new AuthorizationState(
                state.workflowId(), state.initiator(), state.intent(), nextTrajectory, nextHeld);
        }

        return new AuthorizationState(
            state.workflowId(), state.initiator(), state.intent(), nextTrajectory, state.heldAssets());
    }
}
