package com.aios.authz.state;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowStateJpaRepository extends JpaRepository<WorkflowStateEntity, String> {
}
