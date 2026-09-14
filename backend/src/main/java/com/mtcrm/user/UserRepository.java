package com.mtcrm.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    Optional<AppUser> findByIdAndTenantId(UUID id, UUID tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from AppUser user where user.id = :id")
    Optional<AppUser> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from AppUser user where user.id = :id and user.tenantId = :tenantId")
    Optional<AppUser> findByIdAndTenantIdForUpdate(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
    long countByTenantId(UUID tenantId);
    long countByTenantIdAndRoleAndStatus(UUID tenantId, Role role, UserStatus status);
    boolean existsByEmailIgnoreCase(String email);

    @Query("""
        select u from AppUser u where u.tenantId = :tenantId
          and (:q = '' or lower(u.firstName) like lower(concat('%', :q, '%'))
            or lower(u.lastName) like lower(concat('%', :q, '%'))
            or lower(u.email) like lower(concat('%', :q, '%')))
          and (:role is null or u.role = :role)
          and (:status is null or u.status = :status)
        """)
    Page<AppUser> search(@Param("tenantId") UUID tenantId, @Param("q") String query,
                         @Param("role") Role role, @Param("status") UserStatus status, Pageable pageable);
}
