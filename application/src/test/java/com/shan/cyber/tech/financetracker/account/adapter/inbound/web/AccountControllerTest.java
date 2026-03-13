package com.shan.cyber.tech.financetracker.account.adapter.inbound.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shan.cyber.tech.financetracker.account.domain.exception.AccountNotFoundException;
import com.shan.cyber.tech.financetracker.account.domain.exception.MaxAccountsExceededException;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.AccountView;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.CreateAccountUseCase;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.DeactivateAccountUseCase;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.GetAccountsQuery;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.NetWorthView;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.UpdateAccountUseCase;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.GlobalExceptionHandler;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.SecurityContextHolder;
import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@Import(GlobalExceptionHandler.class)
class AccountControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private CreateAccountUseCase createAccountUseCase;
    @MockBean private UpdateAccountUseCase updateAccountUseCase;
    @MockBean private DeactivateAccountUseCase deactivateAccountUseCase;
    @MockBean private GetAccountsQuery getAccountsQuery;

    private static final Long TEST_USER_ID = 1L;
    private static final AccountView SAMPLE_VIEW = new AccountView(
            1L, "My Savings", "SAVINGS", "Savings Account",
            Money.of("1000.00", "USD"), Money.of("1000.00", "USD"),
            "USD", null, null, true, true, false, null);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setCurrentUserId(TEST_USER_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    void list_returnsAccountList() throws Exception {
        when(getAccountsQuery.getAccountsByOwner(any())).thenReturn(List.of(SAMPLE_VIEW));

        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("My Savings"))
                .andExpect(jsonPath("$[0].accountTypeCode").value("SAVINGS"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(createAccountUseCase.createAccount(any())).thenReturn(new AccountId(1L));
        when(getAccountsQuery.getAccountById(any(), any())).thenReturn(SAMPLE_VIEW);

        String body = objectMapper.writeValueAsString(new CreateAccountRequestDto(
                "My Savings", "SAVINGS", new BigDecimal("1000.00"), "USD", null, null));

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My Savings"))
                .andExpect(header().string("Location", "/api/v1/accounts/1"));
    }

    @Test
    void create_blankName_returns422() throws Exception {
        String body = objectMapper.writeValueAsString(new CreateAccountRequestDto(
                "", "SAVINGS", new BigDecimal("0"), "USD", null, null));

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void create_invalidCurrencyCode_returns422() throws Exception {
        String body = objectMapper.writeValueAsString(new CreateAccountRequestDto(
                "Savings", "SAVINGS", new BigDecimal("0"), "us", null, null));

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void create_maxAccountsExceeded_returns422() throws Exception {
        when(createAccountUseCase.createAccount(any()))
                .thenThrow(new MaxAccountsExceededException(20));

        String body = objectMapper.writeValueAsString(new CreateAccountRequestDto(
                "Account", "SAVINGS", new BigDecimal("0"), "USD", null, null));

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void getById_found_returns200() throws Exception {
        when(getAccountsQuery.getAccountById(any(), any())).thenReturn(SAMPLE_VIEW);

        mockMvc.perform(get("/api/v1/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getById_notFound_returns422() throws Exception {
        when(getAccountsQuery.getAccountById(any(), any()))
                .thenThrow(new AccountNotFoundException(999L));

        mockMvc.perform(get("/api/v1/accounts/999"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void update_validRequest_returns200() throws Exception {
        when(updateAccountUseCase.updateAccount(any())).thenReturn(SAMPLE_VIEW);

        String body = objectMapper.writeValueAsString(new UpdateAccountRequestDto("My Savings Renamed", null));

        mockMvc.perform(put("/api/v1/accounts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void deactivate_success_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/accounts/1"))
                .andExpect(status().isNoContent());

        verify(deactivateAccountUseCase).deactivateAccount(any(), any());
    }

    @Test
    void netWorth_returns200WithSummary() throws Exception {
        NetWorthView netWorthView = new NetWorthView(
                Money.of("2000.00", "USD"),
                Money.of("500.00", "USD"),
                Money.of("1500.00", "USD"));
        when(getAccountsQuery.getNetWorth(any())).thenReturn(netWorthView);

        mockMvc.perform(get("/api/v1/accounts/net-worth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAssets").value("2000.0000"))
                .andExpect(jsonPath("$.totalLiabilities").value("500.0000"))
                .andExpect(jsonPath("$.netWorth").value("1500.0000"));
    }
}
