package com.shan.cyber.tech.financetracker.account.adapter.inbound.web;

import com.shan.cyber.tech.financetracker.account.domain.exception.AccountNotFoundException;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.CreateAccountUseCase;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.DeactivateAccountUseCase;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.GetAccountsQuery;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.UpdateAccountUseCase;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.GlobalExceptionHandler;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.SecurityContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * H-2 Regression — Soft-deleted accounts must return 404, not 200 with stale data.
 *
 * <p>The persistence layer uses {@code findByIdAndUserIdAndIsActiveTrue}, which returns
 * {@link java.util.Optional#empty()} for inactive accounts. The domain service then throws
 * {@link AccountNotFoundException}. For 404 to be returned, AccountNotFoundException must
 * extend {@link com.shan.cyber.tech.financetracker.shared.domain.exception.ResourceNotFoundException}
 * so that GlobalExceptionHandler maps it to HTTP 404 — not to 422 (which happens when it
 * extends DomainException directly).</p>
 *
 * <p>These tests verify the full controller → exception handler chain is correct.</p>
 */
@WebMvcTest(AccountController.class)
@Import(GlobalExceptionHandler.class)
class AccountSoftDeleteControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private CreateAccountUseCase createAccountUseCase;
    @MockBean private UpdateAccountUseCase updateAccountUseCase;
    @MockBean private DeactivateAccountUseCase deactivateAccountUseCase;
    @MockBean private GetAccountsQuery getAccountsQuery;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setCurrentUserId(1L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    /**
     * Simulate GET /api/v1/accounts/{id} where the account exists in the DB but
     * is_active = false (soft-deleted). The persistence adapter returns Optional.empty()
     * because findByIdAndUserIdAndIsActiveTrue filters it out. The domain service throws
     * AccountNotFoundException. The controller must respond with 404 — not 200, not 422.
     */
    @Test
    void should_return404_when_accountIsSoftDeleted() throws Exception {
        // Arrange — persistence finds nothing because is_active = false; service throws
        when(getAccountsQuery.getAccountById(any(), any()))
                .thenThrow(new AccountNotFoundException(7L));

        // Act + Assert — 404 because AccountNotFoundException extends ResourceNotFoundException
        mockMvc.perform(get("/api/v1/accounts/7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    /**
     * Verify that GET /api/v1/accounts/{id} where the account belongs to another user
     * also returns 404 — not 403. The persistence adapter returns empty because the
     * userId filter mismatches, driving the same AccountNotFoundException path.
     */
    @Test
    void should_return404_when_accountBelongsToAnotherUser() throws Exception {
        // Arrange — account id 99 exists but belongs to user 2, not user 1
        when(getAccountsQuery.getAccountById(any(), any()))
                .thenThrow(new AccountNotFoundException(99L));

        // Act + Assert — 404, never 403, for a resource belonging to another user
        mockMvc.perform(get("/api/v1/accounts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    /**
     * Deactivate endpoint (DELETE) must return 204 on success. This verifies the
     * soft-delete contract — a 204 signals the account is now inactive, subsequent
     * GET calls must return 404.
     */
    @Test
    void should_return204_when_deactivateAccount_succeeds() throws Exception {
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/accounts/5"))
                .andExpect(status().isNoContent());
    }
}
