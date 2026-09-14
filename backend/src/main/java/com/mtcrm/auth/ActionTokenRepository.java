package com.mtcrm.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ActionTokenRepository extends JpaRepository<ActionToken, UUID> {
    Optional<ActionToken> findByTokenHashAndType(String tokenHash, ActionToken.Type type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from ActionToken token where token.tokenHash = :tokenHash and token.type = :type")
    Optional<ActionToken> findByTokenHashAndTypeForUpdate(@Param("tokenHash") String tokenHash,
                                                          @Param("type") ActionToken.Type type);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update ActionToken token set token.usedAt = :usedAt, token.version = token.version + 1
        where token.userId = :userId and token.type = :type and token.usedAt is null
        """)
    int invalidateUnusedForUser(@Param("userId") UUID userId, @Param("type") ActionToken.Type type,
                                @Param("usedAt") Instant usedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ActionToken token where token.expiresAt < :cutoff or (token.usedAt is not null and token.usedAt < :cutoff)")
    int deleteRetiredBefore(@Param("cutoff") Instant cutoff);
}
