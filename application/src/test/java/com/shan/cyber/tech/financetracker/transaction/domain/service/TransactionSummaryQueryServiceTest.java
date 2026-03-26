package com.shan.cyber.tech.financetracker.transaction.domain.service;

import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.CategorySpending;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.TransactionView;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionPage;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionSummaryQueryServiceTest {

    @Mock private TransactionPersistencePort persistencePort;

    private TransactionSummaryQueryService service;

    private static final UserId     USER    = new UserId(1L);
    private static final CategoryId CAT_A   = new CategoryId(5L);
    private static final CategoryId CAT_B   = new CategoryId(6L);
    private static final LocalDate  FROM    = LocalDate.of(2026, 1, 1);
    private static final LocalDate  TO      = LocalDate.of(2026, 1, 31);

    @BeforeEach
    void setUp() {
        service = new TransactionSummaryQueryService(persistencePort);
    }

    // ── sumByType — INCOME ────────────────────────────────────────────────────

    @Test
    void should_returnIncomeTotal_when_sumByTypeCalledWithIncome() {
        when(persistencePort.sumByType(USER, "INCOME", FROM, TO))
                .thenReturn(new BigDecimal("5200.00"));

        BigDecimal result = service.sumByType(USER, "INCOME", FROM, TO);

        assertThat(result.compareTo(new BigDecimal("5200.00"))).isZero();
        verify(persistencePort).sumByType(USER, "INCOME", FROM, TO);
    }

    // ── sumByType — EXPENSE ───────────────────────────────────────────────────

    @Test
    void should_returnExpenseTotal_when_sumByTypeCalledWithExpense() {
        when(persistencePort.sumByType(USER, "EXPENSE", FROM, TO))
                .thenReturn(new BigDecimal("1850.75"));

        BigDecimal result = service.sumByType(USER, "EXPENSE", FROM, TO);

        assertThat(result.compareTo(new BigDecimal("1850.75"))).isZero();
        verify(persistencePort).sumByType(USER, "EXPENSE", FROM, TO);
    }

    // ── sumByType — net derived assertion ────────────────────────────────────

    @Test
    void should_computePositiveNet_when_incomeExceedsExpense() {
        when(persistencePort.sumByType(USER, "INCOME", FROM, TO))
                .thenReturn(new BigDecimal("4000.00"));
        when(persistencePort.sumByType(USER, "EXPENSE", FROM, TO))
                .thenReturn(new BigDecimal("2500.00"));

        BigDecimal income  = service.sumByType(USER, "INCOME",  FROM, TO);
        BigDecimal expense = service.sumByType(USER, "EXPENSE", FROM, TO);
        BigDecimal net     = income.subtract(expense);

        assertThat(net.compareTo(new BigDecimal("1500.00"))).isZero();
    }

    @Test
    void should_computeNegativeNet_when_expenseExceedsIncome() {
        when(persistencePort.sumByType(USER, "INCOME",  FROM, TO))
                .thenReturn(new BigDecimal("1000.00"));
        when(persistencePort.sumByType(USER, "EXPENSE", FROM, TO))
                .thenReturn(new BigDecimal("3000.00"));

        BigDecimal net = service.sumByType(USER, "INCOME", FROM, TO)
                                .subtract(service.sumByType(USER, "EXPENSE", FROM, TO));

        assertThat(net.compareTo(new BigDecimal("-2000.00"))).isZero();
    }

    @Test
    void should_returnZeroNet_when_incomeEqualsExpense() {
        when(persistencePort.sumByType(USER, "INCOME",  FROM, TO))
                .thenReturn(new BigDecimal("2000.00"));
        when(persistencePort.sumByType(USER, "EXPENSE", FROM, TO))
                .thenReturn(new BigDecimal("2000.00"));

        BigDecimal net = service.sumByType(USER, "INCOME", FROM, TO)
                                .subtract(service.sumByType(USER, "EXPENSE", FROM, TO));

        assertThat(net.compareTo(BigDecimal.ZERO)).isZero();
    }

    // ── sumByCategory ─────────────────────────────────────────────────────────

    @Test
    void should_returnCategoryBreakdown_when_transactionsExist() {
        List<CategorySpending> spending = List.of(
                new CategorySpending(CAT_A.value(), "Groceries", "350.00"),
                new CategorySpending(CAT_B.value(), "Transport", "120.00"));
        when(persistencePort.sumByCategory(USER, FROM, TO, 10)).thenReturn(spending);

        List<CategorySpending> result = service.sumByCategory(USER, FROM, TO, 10);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).categoryId()).isEqualTo(CAT_A.value());
        assertThat(result.get(0).categoryName()).isEqualTo("Groceries");
        assertThat(result.get(0).totalAmount()).isEqualTo("350.00");
        assertThat(result.get(1).categoryId()).isEqualTo(CAT_B.value());
        assertThat(result.get(1).totalAmount()).isEqualTo("120.00");
        verify(persistencePort).sumByCategory(USER, FROM, TO, 10);
    }

    @Test
    void should_returnEmptyList_when_noTransactionsInDateRange() {
        when(persistencePort.sumByCategory(USER, FROM, TO, 10)).thenReturn(List.of());

        List<CategorySpending> result = service.sumByCategory(USER, FROM, TO, 10);

        assertThat(result).isEmpty();
    }

    @Test
    void should_respectLimitParameter_when_sumByCategoryIsCalled() {
        // Port returns what was requested; we verify the limit is forwarded correctly
        when(persistencePort.sumByCategory(USER, FROM, TO, 5)).thenReturn(List.of());

        service.sumByCategory(USER, FROM, TO, 5);

        verify(persistencePort).sumByCategory(USER, FROM, TO, 5);
    }

    @Test
    void should_sumCorrectlyAcrossCategories_when_multipleCategoriesReturnedByPort() {
        List<CategorySpending> spending = List.of(
                new CategorySpending(CAT_A.value(), "Rent", "1500.00"),
                new CategorySpending(CAT_B.value(), "Groceries", "400.00"));
        when(persistencePort.sumByCategory(USER, FROM, TO, 20)).thenReturn(spending);

        List<CategorySpending> result = service.sumByCategory(USER, FROM, TO, 20);

        // Verify amounts are carried through unchanged
        BigDecimal totalFromResult = result.stream()
                .map(s -> new BigDecimal(s.totalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalFromResult.compareTo(new BigDecimal("1900.00"))).isZero();
    }

    // ── recentTransactions ────────────────────────────────────────────────────

    @Test
    void should_returnRecentTransactions_when_calledWithLimit() {
        AccountId accountId = new AccountId(10L);
        TransactionView view1 = new TransactionView(
                100L, accountId.value(), "Checking", CAT_A.value(), "Groceries",
                "55.00", "USD", "EXPENSE", LocalDate.of(2026, 1, 20),
                "Supermarket run", null, null, null, false, false, OffsetDateTime.now());
        TransactionView view2 = new TransactionView(
                101L, accountId.value(), "Checking", CAT_B.value(), "Transport",
                "12.50", "USD", "EXPENSE", LocalDate.of(2026, 1, 18),
                "Bus fare", null, null, null, false, false, OffsetDateTime.now());

        TransactionPage recentPage = new TransactionPage(List.of(view1, view2), 0, 2, 2L, 1);
        when(persistencePort.findRecent(USER, 2)).thenReturn(recentPage);

        List<TransactionView> result = service.recentTransactions(USER, 2);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(100L);
        assertThat(result.get(1).id()).isEqualTo(101L);
        verify(persistencePort).findRecent(USER, 2);
    }

    @Test
    void should_returnEmptyList_when_userHasNoTransactions() {
        TransactionPage emptyPage = new TransactionPage(List.of(), 0, 5, 0L, 0);
        when(persistencePort.findRecent(USER, 5)).thenReturn(emptyPage);

        List<TransactionView> result = service.recentTransactions(USER, 5);

        assertThat(result).isEmpty();
    }
}
