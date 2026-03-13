package com.shan.cyber.tech.financetracker.category.adapter.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(schema = "finance_tracker", name = "category_types")
public class CategoryTypeJpaEntity {

    @Id
    private Short id;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    protected CategoryTypeJpaEntity() {}

    public Short getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
}
