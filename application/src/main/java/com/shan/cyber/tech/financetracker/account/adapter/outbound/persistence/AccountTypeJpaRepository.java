package com.shan.cyber.tech.financetracker.account.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountTypeJpaRepository extends JpaRepository<AccountTypeJpaEntity, Short> {
    Optional<AccountTypeJpaEntity> findByCode(String code);
}
