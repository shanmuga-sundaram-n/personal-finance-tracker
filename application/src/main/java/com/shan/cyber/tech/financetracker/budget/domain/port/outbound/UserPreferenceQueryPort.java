package com.shan.cyber.tech.financetracker.budget.domain.port.outbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public interface UserPreferenceQueryPort {
    String getPreferredCurrency(UserId userId);
}
