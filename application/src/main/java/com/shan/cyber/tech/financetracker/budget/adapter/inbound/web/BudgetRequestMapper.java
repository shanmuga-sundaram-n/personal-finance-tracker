package com.shan.cyber.tech.financetracker.budget.adapter.inbound.web;

import com.shan.cyber.tech.financetracker.budget.domain.model.BudgetPeriod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Web-layer mapper responsible for converting inbound string values to
 * {@link BudgetPeriod} domain enum values. Keeps the controller free
 * of direct domain-model enum knowledge and centralises invalid-value
 * handling as a 400 Bad Request.
 */
@Component
public class BudgetRequestMapper {

    /**
     * Converts a budget period type string to {@link BudgetPeriod}.
     *
     * @param periodType the raw string value from the request (e.g. "MONTHLY")
     * @return the matching {@link BudgetPeriod} enum constant
     * @throws ResponseStatusException with HTTP 400 if the value is null, blank,
     *                                  or does not match any known enum constant
     */
    public BudgetPeriod toBudgetPeriod(String periodType) {
        if (periodType == null || periodType.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Budget period type must not be blank");
        }
        try {
            return BudgetPeriod.valueOf(periodType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid budget period type: '" + periodType
                            + "'. Accepted values: WEEKLY, BI_WEEKLY, MONTHLY, QUARTERLY, SEMI_ANNUAL, ANNUALLY, CUSTOM");
        }
    }
}
