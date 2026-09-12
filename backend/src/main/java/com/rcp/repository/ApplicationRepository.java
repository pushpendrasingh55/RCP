package com.rcp.repository;

import com.rcp.domain.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

// IMPORTANT: every finder here takes tenantId as an explicit parameter and filters by it in the query
// itself. There is deliberately no "findByApplicationNumber(String)" overload without a tenant - that
// shape of method is exactly the "SELECT ... WHERE application_number = ?" the spec forbids.
// JpaSpecificationExecutor backs the _search endpoint's optional, composable filters (see ApplicationSpecifications).
public interface ApplicationRepository extends JpaRepository<Application, UUID>, JpaSpecificationExecutor<Application> {

    Optional<Application> findByTenantIdAndApplicationNumber(String tenantId, String applicationNumber);

    // Used by the workflow action endpoint: loads the row WITH a pessimistic write lock so that, even
    // under the (rare) window between an optimistic-lock check and commit, two transactions cannot
    // interleave their reads of the same row. Combined with @Version, this belt-and-braces approach
    // means the second of two concurrent actions always fails fast rather than silently overwriting the first.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Application a where a.tenantId = :tenantId and a.applicationNumber = :applicationNumber")
    Optional<Application> findForUpdate(@Param("tenantId") String tenantId,
                                         @Param("applicationNumber") String applicationNumber);

    Optional<Application> findByTenantIdAndRequestReferenceId(String tenantId, String requestReferenceId);
}
