package com.shan.cyber.tech.financetracker.reporting.domain.port.outbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public interface UserPreferencesPort {
    String getPreferredCurrency(UserId userId);
}
