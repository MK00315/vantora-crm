package com.mtcrm.lead;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {
    Optional<Lead> findByIdAndTenantId(UUID id, UUID tenantId);
    long countByTenantId(UUID tenantId);
    long countByTenantIdAndStage(UUID tenantId, LeadStage stage);
    long countByTenantIdAndStageNotIn(UUID tenantId, List<LeadStage> stages);
    @Query("""
        select year(l.stageChangedAt), month(l.stageChangedAt), coalesce(sum(l.estimatedValue), 0)
        from Lead l where l.tenantId = :tenantId and l.stage = :stage and l.stageChangedAt >= :since
        group by year(l.stageChangedAt), month(l.stageChangedAt)
        """)
    List<Object[]> revenueByMonth(@Param("tenantId") UUID tenantId, @Param("stage") LeadStage stage,
                                  @Param("since") java.time.Instant since);

    @Query("""
        select l from Lead l where l.tenantId = :tenantId
          and (:q = '' or lower(l.firstName) like lower(concat('%', :q, '%'))
            or lower(l.lastName) like lower(concat('%', :q, '%'))
            or lower(coalesce(l.company, '')) like lower(concat('%', :q, '%'))
            or lower(coalesce(l.email, '')) like lower(concat('%', :q, '%')))
          and (:stage is null or l.stage = :stage)
        """)
    Page<Lead> search(@Param("tenantId") UUID tenantId, @Param("q") String query,
                      @Param("stage") LeadStage stage, Pageable pageable);

    @Query("select l.stage, count(l), coalesce(sum(l.estimatedValue), 0) from Lead l where l.tenantId = :tenantId group by l.stage")
    List<Object[]> pipeline(@Param("tenantId") UUID tenantId);

    @Query("select coalesce(sum(l.estimatedValue), 0) from Lead l where l.tenantId = :tenantId and l.stage = :stage")
    BigDecimal sumValueByTenantIdAndStage(@Param("tenantId") UUID tenantId, @Param("stage") LeadStage stage);
}
