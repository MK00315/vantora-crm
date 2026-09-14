package com.mtcrm.activity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {
    Page<Activity> findAllByTenantId(UUID tenantId, Pageable pageable);
    List<Activity> findByTenantId(UUID tenantId, Pageable pageable);
    List<Activity> findTop8ByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    long countByTenantId(UUID tenantId);
}
