package com.aios.authz.state;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * The persisted row for one workflow: its id, plus the entire
 * {@link AuthorizationStateSnapshot} serialized as JSON in a single column.
 * Deliberately not normalized into separate tables for actions/delegations/
 * provenance nodes — this is a research prototype's state store, not a
 * reporting system that needs to query into that structure with SQL.
 */
@Entity
@Table(name = "workflow_state")
public class WorkflowStateEntity {

    @Id
    @Column(name = "workflow_id", nullable = false, updatable = false)
    private String workflowId;

    @Lob
    @Column(name = "state_json", nullable = false)
    private String stateJson;

    protected WorkflowStateEntity() {
        // required by JPA
    }

    public WorkflowStateEntity(String workflowId, String stateJson) {
        this.workflowId = workflowId;
        this.stateJson = stateJson;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public String getStateJson() {
        return stateJson;
    }

    public void setStateJson(String stateJson) {
        this.stateJson = stateJson;
    }
}
