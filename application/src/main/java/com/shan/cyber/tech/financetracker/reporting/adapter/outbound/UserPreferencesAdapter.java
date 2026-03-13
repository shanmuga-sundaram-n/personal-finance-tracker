package com.shan.cyber.tech.financetracker.reporting.adapter.outbound;

import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.GetCurrentUserQuery;
import com.shan.cyber.tech.financetracker.reporting.domain.port.outbound.UserPreferencesPort;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.springframework.stereotype.Component;

@Component
public class UserPreferencesAdapter implements UserPreferencesPort {

    private final GetCurrentUserQuery getCurrentUserQuery;

    public UserPreferencesAdapter(GetCurrentUserQuery getCurrentUserQuery) {
        this.getCurrentUserQuery = getCurrentUserQuery;
    }

    @Override
    public String getPreferredCurrency(UserId userId) {
        return getCurrentUserQuery.getCurrentUser(userId).preferredCurrency();
    }
}
