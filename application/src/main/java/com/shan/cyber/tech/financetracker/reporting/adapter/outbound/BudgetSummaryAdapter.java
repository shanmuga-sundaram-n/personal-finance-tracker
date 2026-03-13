package com.shan.cyber.tech.financetracker.reporting.adapter.outbound;

import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.BudgetView;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.GetBudgetsQuery;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.BudgetProgressSummary;
import com.shan.cyber.tech.financetracker.reporting.domain.port.outbound.BudgetSummaryPort;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BudgetSummaryAdapter implements BudgetSummaryPort {

    private final GetBudgetsQuery getBudgetsQuery;

    public BudgetSummaryAdapter(GetBudgetsQuery getBudgetsQuery) {
        this.getBudgetsQuery = getBudgetsQuery;
    }

    @Override
    public List<BudgetProgressSummary> getActiveBudgetAlerts(UserId userId) {
        return getBudgetsQuery.getActiveByUser(userId).stream()
                .filter(b -> b.alertTriggered() || b.percentUsed() >= 70.0)
                .map(this::toSummary)
                .toList();
    }

    private BudgetProgressSummary toSummary(BudgetView view) {
        return new BudgetProgressSummary(
                view.id(),
                view.categoryName(),
                view.percentUsed(),
                view.amount(),
                view.spentAmount());
    }
}
