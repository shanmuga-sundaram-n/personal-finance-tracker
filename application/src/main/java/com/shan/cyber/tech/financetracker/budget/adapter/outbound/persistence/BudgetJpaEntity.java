package com.shan.cyber.tech.financetracker.budget.adapter.outbound.persistence;

import com.shan.cyber.tech.financetracker.shared.adapter.outbound.persistence.AuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(schema = "finance_tracker", name = "budgets")
public class BudgetJpaEntity extends AuditableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "budgets_id_gen")
    @SequenceGenerator(name = "budgets_id_gen", sequenceName = "finance_tracker.budgets_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "period_type", nullable = false, length = 20)
    private String periodType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "rollover_enabled", nullable = false)
    private boolean rolloverEnabled;

    @Column(name = "alert_threshold_pct", columnDefinition = "smallint")
    private Short alertThresholdPct;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    protected BudgetJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getPeriodType() { return periodType; }
    public void setPeriodType(String periodType) { this.periodType = periodType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public boolean isRolloverEnabled() { return rolloverEnabled; }
    public void setRolloverEnabled(boolean rolloverEnabled) { this.rolloverEnabled = rolloverEnabled; }
    public Short getAlertThresholdPct() { return alertThresholdPct; }
    public void setAlertThresholdPct(Short alertThresholdPct) { this.alertThresholdPct = alertThresholdPct; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
