package com.shan.cyber.tech.financetracker.account.adapter.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(schema = "finance_tracker", name = "account_types")
public class AccountTypeJpaEntity {

    @Id
    private Short id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "allows_negative_balance", nullable = false)
    private boolean allowsNegativeBalance;

    @Column(name = "is_liability", nullable = false)
    private boolean isLiability;

    @Column(name = "description")
    private String description;

    protected AccountTypeJpaEntity() {}

    public Short getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isAllowsNegativeBalance() { return allowsNegativeBalance; }
    public boolean isLiability() { return isLiability; }
    public String getDescription() { return description; }
}
