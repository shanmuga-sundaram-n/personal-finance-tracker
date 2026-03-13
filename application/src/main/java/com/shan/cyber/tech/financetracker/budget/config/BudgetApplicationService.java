package com.shan.cyber.tech.financetracker.budget.config;

import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.CreateBudgetCommand;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.CreateBudgetUseCase;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.DeactivateBudgetUseCase;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.UpdateBudgetCommand;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.UpdateBudgetUseCase;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.BudgetEventPublisherPort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.BudgetPersistencePort;
import com.shan.cyber.tech.financetracker.budget.domain.service.BudgetCommandService;
import com.shan.cyber.tech.financetracker.shared.domain.model.BudgetId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BudgetApplicationService implements CreateBudgetUseCase, UpdateBudgetUseCase, DeactivateBudgetUseCase {

    private final BudgetCommandService commandService;

    public BudgetApplicationService(BudgetPersistencePort persistencePort,
                                     BudgetEventPublisherPort eventPublisherPort) {
        this.commandService = new BudgetCommandService(persistencePort, eventPublisherPort);
    }

    @Override
    public BudgetId createBudget(CreateBudgetCommand command) {
        return commandService.createBudget(command);
    }

    @Override
    public void updateBudget(UpdateBudgetCommand command) {
        commandService.updateBudget(command);
    }

    @Override
    public void deactivateBudget(BudgetId budgetId, UserId userId) {
        commandService.deactivateBudget(budgetId, userId);
    }
}
