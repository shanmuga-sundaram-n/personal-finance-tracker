package com.shan.cyber.tech.financetracker.budget.domain.model;

import com.shan.cyber.tech.financetracker.shared.domain.model.AuditInfo;
import com.shan.cyber.tech.financetracker.shared.domain.model.BudgetId;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.time.LocalDate;
import java.util.Objects;

public class Budget {

    private BudgetId id;
    private final UserId userId;
    private final CategoryId categoryId;
    private final BudgetPeriod periodType;
    private Money amount;
    private final LocalDate startDate;
    private LocalDate endDate;
    private boolean rolloverEnabled;
    private Integer alertThresholdPct;
    private boolean isActive;
    private AuditInfo auditInfo;

    public Budget(BudgetId id, UserId userId, CategoryId categoryId, BudgetPeriod periodType,
                  Money amount, LocalDate startDate, LocalDate endDate,
                  boolean rolloverEnabled, Integer alertThresholdPct, boolean isActive,
                  AuditInfo auditInfo) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId must not be null");
        this.periodType = Objects.requireNonNull(periodType, "periodType must not be null");
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        this.startDate = Objects.requireNonNull(startDate, "startDate must not be null");
        this.endDate = endDate;
        this.rolloverEnabled = rolloverEnabled;
        this.alertThresholdPct = alertThresholdPct;
        this.isActive = isActive;
        this.auditInfo = auditInfo;
    }

    public static Budget create(UserId userId, CategoryId categoryId, BudgetPeriod periodType,
                                 Money amount, LocalDate startDate, LocalDate endDate,
                                 boolean rolloverEnabled, Integer alertThresholdPct) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Budget amount must be positive");
        }
        if (alertThresholdPct != null && (alertThresholdPct < 1 || alertThresholdPct > 100)) {
            throw new IllegalArgumentException("Alert threshold must be between 1 and 100");
        }

        LocalDate resolvedEndDate;
        if (periodType == BudgetPeriod.CUSTOM) {
            if (endDate == null) {
                throw new IllegalArgumentException("End date is required for CUSTOM period");
            }
            if (!endDate.isAfter(startDate)) {
                throw new IllegalArgumentException("End date must be after start date");
            }
            resolvedEndDate = endDate;
        } else {
            resolvedEndDate = endDate != null ? endDate : periodType.calculateEndDate(startDate);
        }

        return new Budget(null, userId, categoryId, periodType, amount, startDate,
                resolvedEndDate, rolloverEnabled, alertThresholdPct, true, null);
    }

    public void update(Money amount, LocalDate endDate, boolean rolloverEnabled, Integer alertThresholdPct) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Budget amount must be positive");
        }
        if (alertThresholdPct != null && (alertThresholdPct < 1 || alertThresholdPct > 100)) {
            throw new IllegalArgumentException("Alert threshold must be between 1 and 100");
        }
        this.amount = amount;
        if (endDate != null) {
            this.endDate = endDate;
        }
        this.rolloverEnabled = rolloverEnabled;
        this.alertThresholdPct = alertThresholdPct;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public BudgetId getId() { return id; }
    public UserId getUserId() { return userId; }
    public CategoryId getCategoryId() { return categoryId; }
    public BudgetPeriod getPeriodType() { return periodType; }
    public Money getAmount() { return amount; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public boolean isRolloverEnabled() { return rolloverEnabled; }
    public Integer getAlertThresholdPct() { return alertThresholdPct; }
    public boolean isActive() { return isActive; }
    public AuditInfo getAuditInfo() { return auditInfo; }

    public void setId(BudgetId id) { this.id = id; }
    public void setAuditInfo(AuditInfo auditInfo) { this.auditInfo = auditInfo; }
}
