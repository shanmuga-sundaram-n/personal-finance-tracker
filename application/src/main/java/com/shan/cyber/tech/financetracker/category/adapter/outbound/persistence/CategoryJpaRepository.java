package com.shan.cyber.tech.financetracker.category.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, Long> {

    List<CategoryJpaEntity> findByUserIdAndIsActiveTrue(Long userId);

    List<CategoryJpaEntity> findByUserIdIsNullAndIsActiveTrue();

    Optional<CategoryJpaEntity> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT c FROM CategoryJpaEntity c WHERE c.isActive = true " +
            "AND (c.userId IS NULL OR c.userId = :userId)")
    List<CategoryJpaEntity> findSystemAndUserCategories(@Param("userId") Long userId);

    @Query("SELECT c FROM CategoryJpaEntity c WHERE c.isActive = true " +
            "AND (c.userId IS NULL OR c.userId = :userId) " +
            "AND c.categoryType.code = :typeCode")
    List<CategoryJpaEntity> findSystemAndUserCategoriesByType(@Param("userId") Long userId,
                                                              @Param("typeCode") String typeCode);

    @Query("SELECT c FROM CategoryJpaEntity c WHERE c.isActive = true " +
            "AND (c.userId IS NULL OR c.userId = :userId) " +
            "AND c.id = :id")
    Optional<CategoryJpaEntity> findByIdForUser(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT c FROM CategoryJpaEntity c WHERE c.isActive = true " +
            "AND LOWER(c.name) = LOWER(:name) " +
            "AND (c.userId IS NULL OR c.userId = :userId) " +
            "AND ((:parentId IS NULL AND c.parentCategoryId IS NULL) OR c.parentCategoryId = :parentId)")
    Optional<CategoryJpaEntity> findByNameForUser(@Param("userId") Long userId,
                                                   @Param("name") String name,
                                                   @Param("parentId") Long parentId);
}
