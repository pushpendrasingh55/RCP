package com.rcp.repository;

import com.rcp.domain.TransitionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransitionHistoryRepository extends JpaRepository<TransitionHistory, UUID> {

    // Always scoped by both applicationId AND tenantId, for the same reason as ApplicationRepository -
    // defence in depth, even though applicationId alone is already unique per application.
    List<TransitionHistory> findByTenantIdAndApplicationIdOrderByCreatedTimeAsc(String tenantId, UUID applicationId);
}
