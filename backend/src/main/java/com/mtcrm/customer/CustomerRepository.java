package com.mtcrm.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByIdAndTenantId(UUID id, UUID tenantId);
    long countByTenantId(UUID tenantId);
    long countByTenantIdAndStatus(UUID tenantId, CustomerStatus status);
    Slice<Customer> findAllByTenantId(UUID tenantId, Pageable pageable);

    @Query("""
        select c from Customer c where c.tenantId = :tenantId
          and (:q = '' or lower(c.name) like lower(concat('%', :q, '%'))
            or lower(coalesce(c.company, '')) like lower(concat('%', :q, '%'))
            or lower(coalesce(c.email, '')) like lower(concat('%', :q, '%')))
          and (:status is null or c.status = :status)
        """)
    Page<Customer> search(@Param("tenantId") UUID tenantId, @Param("q") String query,
                          @Param("status") CustomerStatus status, Pageable pageable);
}
