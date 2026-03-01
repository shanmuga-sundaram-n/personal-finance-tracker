package com.shan.cyber.tech.financetracker.account.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, Long> {

    List<AccountJpaEntity> findByUserIdAndIsActiveTrue(Long userId);

    Optional<AccountJpaEntity> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndIsActiveTrue(Long userId);

    @Query("SELECT a FROM AccountJpaEntity a WHERE a.userId = :userId AND LOWER(a.name) = LOWER(:name) AND a.isActive = true")
    Optional<AccountJpaEntity> findByUserIdAndNameIgnoreCase(@Param("userId") Long userId, @Param("name") String name);
}
