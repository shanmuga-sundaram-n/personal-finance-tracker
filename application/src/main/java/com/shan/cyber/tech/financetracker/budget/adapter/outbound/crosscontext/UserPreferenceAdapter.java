package com.shan.cyber.tech.financetracker.budget.adapter.outbound.crosscontext;

import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.UserPreferenceQueryPort;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.GetCurrentUserQuery;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.springframework.stereotype.Component;

@Component
public class UserPreferenceAdapter implements UserPreferenceQueryPort {

    private final GetCurrentUserQuery getCurrentUserQuery;

    public UserPreferenceAdapter(GetCurrentUserQuery getCurrentUserQuery) {
        this.getCurrentUserQuery = getCurrentUserQuery;
    }

    @Override
    public String getPreferredCurrency(UserId userId) {
        try {
            String currency = getCurrentUserQuery.getCurrentUser(userId).preferredCurrency();
            return (currency != null && !currency.isBlank()) ? currency : "USD";
        } catch (Exception e) {
            return "USD";
        }
    }
}
