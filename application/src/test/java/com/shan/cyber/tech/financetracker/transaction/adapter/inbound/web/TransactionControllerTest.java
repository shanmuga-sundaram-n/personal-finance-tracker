package com.shan.cyber.tech.financetracker.transaction.adapter.inbound.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.GlobalExceptionHandler;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.SecurityContextHolder;
import com.shan.cyber.tech.financetracker.shared.domain.model.TransactionId;
import com.shan.cyber.tech.financetracker.transaction.domain.exception.TransactionNotFoundException;
import com.shan.cyber.tech.financetracker.transaction.domain.exception.TransferSameAccountException;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.CreateTransactionUseCase;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.CreateTransferUseCase;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.DeleteTransactionUseCase;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.GetTransactionsQuery;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.ReconcileTransactionUseCase;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.TransactionView;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.TransferResult;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.UpdateTransactionUseCase;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionPage;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@Import({GlobalExceptionHandler.class, TransactionRequestMapper.class})
class TransactionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private CreateTransactionUseCase createTransactionUseCase;
    @MockBean private CreateTransferUseCase createTransferUseCase;
    @MockBean private UpdateTransactionUseCase updateTransactionUseCase;
    @MockBean private DeleteTransactionUseCase deleteTransactionUseCase;
    @MockBean private ReconcileTransactionUseCase reconcileTransactionUseCase;
    @MockBean private GetTransactionsQuery getTransactionsQuery;

    private static final Long TEST_USER_ID = 1L;
    private static final TransactionView SAMPLE_VIEW = new TransactionView(
            1L, 10L, "My Savings", 5L, "Groceries",
            "100.0000", "USD", "EXPENSE", LocalDate.now(),
            "Weekly shopping", null, null, null, false, false, null);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setCurrentUserId(TEST_USER_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    void list_returnsPagedTransactions() throws Exception {
        TransactionPage page = new TransactionPage(List.of(SAMPLE_VIEW), 0, 30, 1L, 1);
        when(getTransactionsQuery.getTransactions(any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].type").value("EXPENSE"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void create_validExpense_returns201() throws Exception {
        when(createTransactionUseCase.createTransaction(any())).thenReturn(new TransactionId(1L));
        when(getTransactionsQuery.getById(any(), any())).thenReturn(SAMPLE_VIEW);

        String body = objectMapper.writeValueAsString(new CreateTransactionRequestDto(
                10L, 5L, new BigDecimal("100.00"), "USD", "EXPENSE",
                LocalDate.now(), "Weekly shopping", null, null));

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(header().string("Location", "/api/v1/transactions/1"));
    }

    @Test
    void create_nullAccountId_returns422() throws Exception {
        String body = "{\"accountId\":null,\"categoryId\":5,\"amount\":100,\"currency\":\"USD\",\"type\":\"EXPENSE\",\"transactionDate\":\"2025-01-01\"}";

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createTransfer_success_returns201() throws Exception {
        TransferResult result = new TransferResult(new TransactionId(10L), new TransactionId(11L));
        when(createTransferUseCase.createTransfer(any())).thenReturn(result);

        String body = objectMapper.writeValueAsString(new CreateTransferRequestDto(
                10L, 20L, 5L, new BigDecimal("500.00"), "USD", LocalDate.now(), "Monthly transfer"));

        mockMvc.perform(post("/api/v1/transactions/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outboundId").value(10L))
                .andExpect(jsonPath("$.inboundId").value(11L));
    }

    @Test
    void createTransfer_sameAccount_returns422() throws Exception {
        when(createTransferUseCase.createTransfer(any()))
                .thenThrow(new TransferSameAccountException());

        String body = objectMapper.writeValueAsString(new CreateTransferRequestDto(
                10L, 10L, 5L, new BigDecimal("500.00"), "USD", LocalDate.now(), "Same account"));

        mockMvc.perform(post("/api/v1/transactions/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void getById_found_returns200() throws Exception {
        when(getTransactionsQuery.getById(any(), any())).thenReturn(SAMPLE_VIEW);

        mockMvc.perform(get("/api/v1/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(getTransactionsQuery.getById(any(), any()))
                .thenThrow(new TransactionNotFoundException(999L));

        mockMvc.perform(get("/api/v1/transactions/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_success_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/transactions/1"))
                .andExpect(status().isNoContent());

        verify(deleteTransactionUseCase).deleteTransaction(any(), any());
    }
}
