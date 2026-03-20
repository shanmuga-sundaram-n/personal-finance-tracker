package com.shan.cyber.tech.financetracker.budget.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that each BudgetPeriod.toMonthlyMultiplier() returns the correct
 * value and that the yearly annualisation formula holds.
 *
 * Multiplier reference:
 *   WEEKLY       = 13/3  ≈ 4.3333...
 *   BI_WEEKLY    = 13/6  ≈ 2.1667...
 *   MONTHLY      = 1
 *   QUARTERLY    = 1/3   ≈ 0.3333...
 *   SEMI_ANNUAL  = 1/6   ≈ 0.1667...
 *   ANNUALLY     = 1/12  ≈ 0.0833...
 *   CUSTOM       = 1 (pass-through)
 */
class BudgetPeriodTest {

    private static final double DELTA = 0.0001;

    @Test
    void weekly_monthlyMultiplier_isThirteenDividedByThree() {
        // 52 weeks / 12 months = 13/3
        double expected = 13.0 / 3.0;
        assertEquals(expected, BudgetPeriod.WEEKLY.toMonthlyMultiplier().doubleValue(), DELTA);
    }

    @Test
    void biWeekly_monthlyMultiplier_isThirteenDividedBySix() {
        // 26 bi-weekly periods / 12 months = 13/6
        double expected = 13.0 / 6.0;
        assertEquals(expected, BudgetPeriod.BI_WEEKLY.toMonthlyMultiplier().doubleValue(), DELTA);
    }

    @Test
    void monthly_monthlyMultiplier_isOne() {
        assertEquals(1.0, BudgetPeriod.MONTHLY.toMonthlyMultiplier().doubleValue(), DELTA);
    }

    @Test
    void quarterly_monthlyMultiplier_isOneThird() {
        // 1 quarter = 3 months → monthly fraction = 1/3
        double expected = 1.0 / 3.0;
        assertEquals(expected, BudgetPeriod.QUARTERLY.toMonthlyMultiplier().doubleValue(), DELTA);
    }

    @Test
    void semiAnnual_monthlyMultiplier_isOneSixth() {
        // 1 semi-annual period = 6 months → monthly fraction = 1/6
        double expected = 1.0 / 6.0;
        assertEquals(expected, BudgetPeriod.SEMI_ANNUAL.toMonthlyMultiplier().doubleValue(), DELTA);
    }

    @Test
    void annually_monthlyMultiplier_isOneTwelfth() {
        // 1 annual period = 12 months → monthly fraction = 1/12
        double expected = 1.0 / 12.0;
        assertEquals(expected, BudgetPeriod.ANNUALLY.toMonthlyMultiplier().doubleValue(), DELTA);
    }

    @Test
    void custom_monthlyMultiplier_isOne() {
        // CUSTOM is a pass-through — no frequency adjustment
        assertEquals(1.0, BudgetPeriod.CUSTOM.toMonthlyMultiplier().doubleValue(), DELTA);
    }

    @Test
    void weekly_yearlyFromMonthly_equals52Weeks() {
        // budgetAmount × toMonthlyMultiplier() × 12 should equal budgetAmount × 52/12 × 12 = budgetAmount × 52
        // For amount 100: 100 × (13/3) × 12 = 100 × 52 = 5200
        BigDecimal amount = new BigDecimal("100");
        BigDecimal yearlyViaMonthly = amount
                .multiply(BudgetPeriod.WEEKLY.toMonthlyMultiplier())
                .multiply(BigDecimal.valueOf(12));
        assertEquals(5200.0, yearlyViaMonthly.doubleValue(), 0.01);
    }
}
