package com.shan.cyber.tech.financetracker.budget.adapter.outbound.persistence;

import com.shan.cyber.tech.financetracker.budget.domain.model.Budget;
import com.shan.cyber.tech.financetracker.budget.domain.model.BudgetPeriod;
import com.shan.cyber.tech.financetracker.shared.domain.model.AuditInfo;
import com.shan.cyber.tech.financetracker.shared.domain.model.BudgetId;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.springframework.stereotype.Component;

@Component
public class BudgetJpaMapper {

    public Budget toDomain(BudgetJpaEntity entity) {
        AuditInfo auditInfo = new AuditInfo(
                entity.getCreatedAt(), entity.getUpdatedAt(),
                entity.getCreatedBy(), entity.getUpdatedBy());

        return new Budget(
                new BudgetId(entity.getId()),
                new UserId(entity.getUserId()),
                new CategoryId(entity.getCategoryId()),
                BudgetPeriod.valueOf(entity.getPeriodType()),
                Money.of(entity.getAmount(), entity.getCurrency()),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.isRolloverEnabled(),
                entity.getAlertThresholdPct() != null ? entity.getAlertThresholdPct().intValue() : null,
                entity.isActive(),
                auditInfo);
    }

    public BudgetJpaEntity toJpaEntity(Budget budget) {
        BudgetJpaEntity entity = new BudgetJpaEntity();
        if (budget.getId() != null) {
            entity.setId(budget.getId().value());
        }
        entity.setUserId(budget.getUserId().value());
        entity.setCategoryId(budget.getCategoryId().value());
        entity.setPeriodType(budget.getPeriodType().name());
        entity.setAmount(budget.getAmount().amount());
        entity.setCurrency(budget.getAmount().currency());
        entity.setStartDate(budget.getStartDate());
        entity.setEndDate(budget.getEndDate());
        entity.setRolloverEnabled(budget.isRolloverEnabled());
        entity.setAlertThresholdPct(budget.getAlertThresholdPct() != null ? budget.getAlertThresholdPct().shortValue() : null);
        entity.setActive(budget.isActive());
        return entity;
    }
}
