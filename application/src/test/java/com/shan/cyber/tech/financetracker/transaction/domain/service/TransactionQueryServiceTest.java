package com.shan.cyber.tech.financetracker.transaction.domain.service;

import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.TransactionId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import com.shan.cyber.tech.financetracker.transaction.domain.exception.TransactionNotFoundException;
import com.shan.cyber.tech.financetracker.transaction.domain.model.TransactionType;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.TransactionView;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionFilter;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionPage;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionQueryServiceTest {

    @Mock private TransactionPersistencePort persistencePort;

    private TransactionQueryService service;

    private static final UserId USER       = new UserId(1L);
    private static final UserId OTHER_USER = new UserId(99L);
    private static final AccountId ACCOUNT = new AccountId(10L);
    private static final CategoryId CAT    = new CategoryId(5L);

    @BeforeEach
    void setUp() {
        service = new TransactionQueryService(persistencePort);
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private TransactionView sampleView(long id) {
        return new TransactionView(
                id, ACCOUNT.value(), "Checking", CAT.value(), "Groceries",
                "75.5000", "USD", "EXPENSE",
                LocalDate.of(2026, 1, 15), "Weekly shop", "Tesco", null,
                null, false, false, OffsetDateTime.now());
    }

    // ── getById — happy path ──────────────────────────────────────────────────

    @Test
    void should_returnTransactionView_when_transactionBelongsToAuthenticatedUser() {
        TransactionId txId = new TransactionId(42L);
        TransactionView expected = sampleView(42L);
        when(persistencePort.findViewById(txId, USER)).thenReturn(Optional.of(expected));

        TransactionView result = service.getById(txId, USER);

        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.amount()).isEqualTo("75.5000");
        assertThat(result.type()).isEqualTo("EXPENSE");
        assertThat(result.accountName()).isEqualTo("Checking");
    }

    // ── getById — another user's transaction ─────────────────────────────────

    @Test
    void should_throwTransactionNotFoundException_when_transactionBelongsToAnotherUser() {
        TransactionId txId = new TransactionId(42L);
        // Persistence returns empty because userId doesn't match (multi-tenant isolation)
        when(persistencePort.findViewById(txId, OTHER_USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(txId, OTHER_USER))
                .isInstanceOf(TransactionNotFoundException.class);
    }

    @Test
    void should_throwTransactionNotFoundException_when_transactionIdDoesNotExist() {
        TransactionId missing = new TransactionId(9999L);
        when(persistencePort.findViewById(missing, USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(missing, USER))
                .isInstanceOf(TransactionNotFoundException.class);
    }

    // ── getTransactions — filter: no filters ─────────────────────────────────

    @Test
    void should_returnAllUserTransactions_when_noFiltersApplied() {
        TransactionFilter filter = new TransactionFilter(
                USER, null, null, null, null, null, null, null);
        List<TransactionView> views = List.of(sampleView(1L), sampleView(2L), sampleView(3L));
        TransactionPage page = new TransactionPage(views, 0, 20, 3L, 1);
        when(persistencePort.findByFilter(filter, 0, 20)).thenReturn(page);

        TransactionPage result = service.getTransactions(filter, 0, 20);

        assertThat(result.content()).hasSize(3);
        assertThat(result.totalElements()).isEqualTo(3L);
    }

    // ── getTransactions — filter: by account ─────────────────────────────────

    @Test
    void should_returnTransactionsForAccount_when_accountFilterApplied() {
        TransactionFilter filter = new TransactionFilter(
                USER, ACCOUNT, null, null, null, null, null, null);
        List<TransactionView> views = List.of(sampleView(10L));
        TransactionPage page = new TransactionPage(views, 0, 20, 1L, 1);
        when(persistencePort.findByFilter(filter, 0, 20)).thenReturn(page);

        TransactionPage result = service.getTransactions(filter, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).accountId()).isEqualTo(ACCOUNT.value());
    }

    // ── getTransactions — filter: by type ────────────────────────────────────

    @Test
    void should_returnOnlyExpenseTransactions_when_typeFilterIsExpense() {
        TransactionFilter filter = new TransactionFilter(
                USER, null, null, TransactionType.EXPENSE, null, null, null, null);
        TransactionView expenseView = new TransactionView(
                20L, ACCOUNT.value(), "Checking", CAT.value(), "Groceries",
                "150.0000", "USD", "EXPENSE",
                LocalDate.of(2026, 1, 10), "Weekly shop", null, null,
                null, false, false, OffsetDateTime.now());
        TransactionPage page = new TransactionPage(List.of(expenseView), 0, 20, 1L, 1);
        when(persistencePort.findByFilter(filter, 0, 20)).thenReturn(page);

        TransactionPage result = service.getTransactions(filter, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).type()).isEqualTo("EXPENSE");
    }

    // ── getTransactions — filter: by category ────────────────────────────────

    @Test
    void should_returnTransactionsForCategory_when_categoryFilterApplied() {
        TransactionFilter filter = new TransactionFilter(
                USER, null, CAT, null, null, null, null, null);
        List<TransactionView> views = List.of(sampleView(30L), sampleView(31L));
        TransactionPage page = new TransactionPage(views, 0, 20, 2L, 1);
        when(persistencePort.findByFilter(filter, 0, 20)).thenReturn(page);

        TransactionPage result = service.getTransactions(filter, 0, 20);

        assertThat(result.content()).hasSize(2);
        assertThat(result.content()).allMatch(v -> v.categoryId().equals(CAT.value()));
    }

    // ── getTransactions — filter: by date range ───────────────────────────────

    @Test
    void should_returnTransactionsWithinDateRange_when_dateRangeFilterApplied() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to   = LocalDate.of(2026, 1, 31);
        TransactionFilter filter = new TransactionFilter(
                USER, null, null, null, from, to, null, null);

        TransactionView jan15View = new TransactionView(
                40L, ACCOUNT.value(), "Checking", CAT.value(), "Groceries",
                "88.0000", "USD", "EXPENSE", LocalDate.of(2026, 1, 15),
                "Mid-month shop", null, null, null, false, false, OffsetDateTime.now());
        TransactionPage page = new TransactionPage(List.of(jan15View), 0, 20, 1L, 1);
        when(persistencePort.findByFilter(filter, 0, 20)).thenReturn(page);

        TransactionPage result = service.getTransactions(filter, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).transactionDate()).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    // ── getTransactions — delegates correctly ─────────────────────────────────

    @Test
    void should_delegateToPersistencePort_with_correctPageAndSizeArgs() {
        TransactionFilter filter = new TransactionFilter(USER, null, null, null, null, null, null, null);
        TransactionPage emptyPage = new TransactionPage(List.of(), 2, 10, 0L, 0);
        when(persistencePort.findByFilter(filter, 2, 10)).thenReturn(emptyPage);

        service.getTransactions(filter, 2, 10);

        verify(persistencePort).findByFilter(filter, 2, 10);
    }
}
