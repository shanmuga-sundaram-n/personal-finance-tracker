package com.shan.cyber.tech.financetracker.transaction.domain.service;

import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpendingTotalsQueryServiceTest {

    @Mock private TransactionPersistencePort persistencePort;

    private SpendingTotalsQueryService service;

    private static final UserId       USER  = new UserId(1L);
    private static final CategoryId   CAT_A = new CategoryId(5L);
    private static final CategoryId   CAT_B = new CategoryId(6L);
    private static final LocalDate    FROM  = LocalDate.of(2026, 1, 1);
    private static final LocalDate    TO    = LocalDate.of(2026, 1, 31);

    @BeforeEach
    void setUp() {
        service = new SpendingTotalsQueryService(persistencePort);
    }

    // ── sumExpenses ───────────────────────────────────────────────────────────

    @Test
    void should_returnExpenseTotal_when_transactionsExistForCategory() {
        when(persistencePort.sumExpenseAmount(USER, CAT_A, FROM, TO))
                .thenReturn(new BigDecimal("450.75"));

        BigDecimal result = service.sumExpenses(USER, CAT_A, FROM, TO);

        assertThat(result.compareTo(new BigDecimal("450.75"))).isZero();
        verify(persistencePort).sumExpenseAmount(USER, CAT_A, FROM, TO);
    }

    @Test
    void should_returnZero_when_noExpensesForCategoryInDateRange() {
        when(persistencePort.sumExpenseAmount(USER, CAT_A, FROM, TO))
                .thenReturn(BigDecimal.ZERO);

        BigDecimal result = service.sumExpenses(USER, CAT_A, FROM, TO);

        assertThat(result.compareTo(BigDecimal.ZERO)).isZero();
    }

    // ── sumIncome ─────────────────────────────────────────────────────────────

    @Test
    void should_returnIncomeTotal_when_incomeTransactionsExistForCategory() {
        when(persistencePort.sumIncomeAmountByCategoryAndDateRange(USER, CAT_A, FROM, TO))
                .thenReturn(new BigDecimal("3200.00"));

        BigDecimal result = service.sumIncome(USER, CAT_A, FROM, TO);

        assertThat(result.compareTo(new BigDecimal("3200.00"))).isZero();
        verify(persistencePort).sumIncomeAmountByCategoryAndDateRange(USER, CAT_A, FROM, TO);
    }

    @Test
    void should_returnZero_when_noIncomeForCategoryInDateRange() {
        when(persistencePort.sumIncomeAmountByCategoryAndDateRange(USER, CAT_A, FROM, TO))
                .thenReturn(BigDecimal.ZERO);

        BigDecimal result = service.sumIncome(USER, CAT_A, FROM, TO);

        assertThat(result.compareTo(BigDecimal.ZERO)).isZero();
    }

    // ── net (income - expense) derived assertion ──────────────────────────────

    @Test
    void should_computeNetAsIncomeLessExpense_when_bothTotalsArePositive() {
        when(persistencePort.sumIncomeAmountByCategoryAndDateRange(USER, CAT_A, FROM, TO))
                .thenReturn(new BigDecimal("5000.00"));
        when(persistencePort.sumExpenseAmount(USER, CAT_A, FROM, TO))
                .thenReturn(new BigDecimal("1800.00"));

        BigDecimal income  = service.sumIncome(USER, CAT_A, FROM, TO);
        BigDecimal expense = service.sumExpenses(USER, CAT_A, FROM, TO);
        BigDecimal net     = income.subtract(expense);

        assertThat(net.compareTo(new BigDecimal("3200.00"))).isZero();
    }

    @Test
    void should_returnNegativeNet_when_expensesExceedIncome() {
        when(persistencePort.sumIncomeAmountByCategoryAndDateRange(USER, CAT_A, FROM, TO))
                .thenReturn(new BigDecimal("200.00"));
        when(persistencePort.sumExpenseAmount(USER, CAT_A, FROM, TO))
                .thenReturn(new BigDecimal("800.00"));

        BigDecimal net = service.sumIncome(USER, CAT_A, FROM, TO)
                                .subtract(service.sumExpenses(USER, CAT_A, FROM, TO));

        assertThat(net.compareTo(new BigDecimal("-600.00"))).isZero();
    }

    // ── sumExpenseBatch ───────────────────────────────────────────────────────

    @Test
    void should_returnBatchExpenseTotals_when_multipleCategoriesQueried() {
        List<CategoryId> categories = List.of(CAT_A, CAT_B);
        Map<CategoryId, BigDecimal> batchResult = Map.of(
                CAT_A, new BigDecimal("300.00"),
                CAT_B, new BigDecimal("120.50"));
        when(persistencePort.sumExpenseAmountBatch(USER, categories, FROM, TO)).thenReturn(batchResult);

        Map<CategoryId, BigDecimal> result = service.sumExpenseBatch(USER, categories, FROM, TO);

        assertThat(result).hasSize(2);
        assertThat(result.get(CAT_A).compareTo(new BigDecimal("300.00"))).isZero();
        assertThat(result.get(CAT_B).compareTo(new BigDecimal("120.50"))).isZero();
        verify(persistencePort).sumExpenseAmountBatch(USER, categories, FROM, TO);
    }

    @Test
    void should_returnEmptyMap_when_batchExpenseQueryHasNoMatchingTransactions() {
        List<CategoryId> categories = List.of(CAT_A, CAT_B);
        when(persistencePort.sumExpenseAmountBatch(USER, categories, FROM, TO)).thenReturn(Map.of());

        Map<CategoryId, BigDecimal> result = service.sumExpenseBatch(USER, categories, FROM, TO);

        assertThat(result).isEmpty();
    }

    // ── sumIncomeBatch ────────────────────────────────────────────────────────

    @Test
    void should_returnBatchIncomeTotals_when_multipleCategoriesQueried() {
        List<CategoryId> categories = List.of(CAT_A, CAT_B);
        Map<CategoryId, BigDecimal> batchResult = Map.of(
                CAT_A, new BigDecimal("4000.00"),
                CAT_B, new BigDecimal("500.00"));
        when(persistencePort.sumIncomeAmountBatch(USER, categories, FROM, TO)).thenReturn(batchResult);

        Map<CategoryId, BigDecimal> result = service.sumIncomeBatch(USER, categories, FROM, TO);

        assertThat(result).hasSize(2);
        assertThat(result.get(CAT_A).compareTo(new BigDecimal("4000.00"))).isZero();
        verify(persistencePort).sumIncomeAmountBatch(USER, categories, FROM, TO);
    }
}
