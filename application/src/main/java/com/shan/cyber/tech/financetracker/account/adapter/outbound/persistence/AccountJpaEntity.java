package com.shan.cyber.tech.financetracker.account.adapter.outbound.persistence;

import com.shan.cyber.tech.financetracker.shared.adapter.outbound.persistence.AuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

@Entity
@Table(schema = "finance_tracker", name = "accounts")
public class AccountJpaEntity extends AuditableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accounts_id_gen")
    @SequenceGenerator(name = "accounts_id_gen", sequenceName = "finance_tracker.accounts_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_type_id", nullable = false)
    private AccountTypeJpaEntity accountType;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "initial_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal initialBalance;

    @Column(name = "current_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentBalance;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "institution_name", length = 100)
    private String institutionName;

    @Column(name = "account_number_last4", length = 4)
    private String accountNumberLast4;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "include_in_net_worth", nullable = false)
    private boolean includeInNetWorth;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected AccountJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public AccountTypeJpaEntity getAccountType() { return accountType; }
    public void setAccountType(AccountTypeJpaEntity accountType) { this.accountType = accountType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getInitialBalance() { return initialBalance; }
    public void setInitialBalance(BigDecimal initialBalance) { this.initialBalance = initialBalance; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }
    public String getAccountNumberLast4() { return accountNumberLast4; }
    public void setAccountNumberLast4(String accountNumberLast4) { this.accountNumberLast4 = accountNumberLast4; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public boolean isIncludeInNetWorth() { return includeInNetWorth; }
    public void setIncludeInNetWorth(boolean includeInNetWorth) { this.includeInNetWorth = includeInNetWorth; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
