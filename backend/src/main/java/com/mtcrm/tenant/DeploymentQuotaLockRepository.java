package com.mtcrm.tenant;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DeploymentQuotaLockRepository extends JpaRepository<DeploymentQuotaLock, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select guard from DeploymentQuotaLock guard where guard.id = :id")
    Optional<DeploymentQuotaLock> findByIdForUpdate(@Param("id") Integer id);
}
