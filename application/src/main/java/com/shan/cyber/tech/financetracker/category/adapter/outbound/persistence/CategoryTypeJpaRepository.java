package com.shan.cyber.tech.financetracker.category.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryTypeJpaRepository extends JpaRepository<CategoryTypeJpaEntity, Short> {
    Optional<CategoryTypeJpaEntity> findByCode(String code);
}
