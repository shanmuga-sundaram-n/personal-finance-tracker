package com.shan.cyber.tech.financetracker.identity.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface SessionJpaRepository extends JpaRepository<SessionJpaEntity, Long> {

    @Query("SELECT s FROM SessionJpaEntity s WHERE s.token = :token AND s.expiresAt > :now")
    Optional<SessionJpaEntity> findValidSession(@Param("token") String token, @Param("now") OffsetDateTime now);

    @Modifying
    @Query("DELETE FROM SessionJpaEntity s WHERE s.token = :token")
    void deleteByToken(@Param("token") String token);

    @Modifying
    @Query("DELETE FROM SessionJpaEntity s WHERE s.expiresAt < :cutoff")
    void deleteByExpiresAtBefore(@Param("cutoff") OffsetDateTime cutoff);
}
