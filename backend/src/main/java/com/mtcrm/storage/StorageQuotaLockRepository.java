package com.mtcrm.storage;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StorageQuotaLockRepository extends JpaRepository<StorageQuotaLock, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select guard from StorageQuotaLock guard where guard.id = :id")
    Optional<StorageQuotaLock> findByIdForUpdate(@Param("id") Integer id);
}
