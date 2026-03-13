package com.shan.cyber.tech.financetracker.budget.adapter.inbound.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shan.cyber.tech.financetracker.budget.domain.exception.BudgetNotFoundException;
import com.shan.cyber.tech.financetracker.budget.domain.exception.DuplicateActiveBudgetException;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.BudgetView;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.CreateBudgetUseCase;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.DeactivateBudgetUseCase;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.GetBudgetsQuery;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.UpdateBudgetUseCase;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.GlobalExceptionHandler;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.SecurityContextHolder;
import com.shan.cyber.tech.financetracker.shared.domain.model.BudgetId;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BudgetController.class)
@Import(GlobalExceptionHandler.class)
class BudgetControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private CreateBudgetUseCase createBudgetUseCase;
    @MockBean private UpdateBudgetUseCase updateBudgetUseCase;
    @MockBean private DeactivateBudgetUseCase deactivateBudgetUseCase;
    @MockBean private GetBudgetsQuery getBudgetsQuery;

    private static final Long TEST_USER_ID = 1L;
    private static final BudgetView SAMPLE_VIEW = new BudgetView(
            1L, 5L, "Groceries", "MONTHLY", "500.0000", "USD",
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
            false, 80, true, "200.0000", "300.0000", 40.0, false, null);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setCurrentUserId(TEST_USER_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    void list_returnsBudgetList() throws Exception {
        when(getBudgetsQuery.getActiveByUser(any())).thenReturn(List.of(SAMPLE_VIEW));

        mockMvc.perform(get("/api/v1/budgets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].categoryName").value("Groceries"))
                .andExpect(jsonPath("$[0].periodType").value("MONTHLY"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(createBudgetUseCase.createBudget(any())).thenReturn(new BudgetId(1L));
        when(getBudgetsQuery.getById(any(), any())).thenReturn(SAMPLE_VIEW);

        String body = objectMapper.writeValueAsString(new CreateBudgetRequestDto(
                5L, "MONTHLY", new BigDecimal("500.00"), "USD",
                LocalDate.of(2025, 1, 1), null, false, 80));

        mockMvc.perform(post("/api/v1/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryName").value("Groceries"))
                .andExpect(header().string("Location", "/api/v1/budgets/1"));
    }

    @Test
    void create_missingCategoryId_returns422() throws Exception {
        String body = "{\"periodType\":\"MONTHLY\",\"amount\":500,\"currency\":\"USD\",\"startDate\":\"2025-01-01\"}";

        mockMvc.perform(post("/api/v1/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void create_duplicateBudget_returns422() throws Exception {
        when(createBudgetUseCase.createBudget(any()))
                .thenThrow(new DuplicateActiveBudgetException());

        String body = objectMapper.writeValueAsString(new CreateBudgetRequestDto(
                5L, "MONTHLY", new BigDecimal("500.00"), "USD",
                LocalDate.of(2025, 1, 1), null, false, null));

        mockMvc.perform(post("/api/v1/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void getById_found_returns200() throws Exception {
        when(getBudgetsQuery.getById(any(), any())).thenReturn(SAMPLE_VIEW);

        mockMvc.perform(get("/api/v1/budgets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(getBudgetsQuery.getById(any(), any()))
                .thenThrow(new BudgetNotFoundException(999L));

        mockMvc.perform(get("/api/v1/budgets/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_validRequest_returns200() throws Exception {
        when(getBudgetsQuery.getById(any(), any())).thenReturn(SAMPLE_VIEW);

        String body = objectMapper.writeValueAsString(new UpdateBudgetRequestDto(
                new BigDecimal("800.00"), "USD", null, true, 90));

        mockMvc.perform(put("/api/v1/budgets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(updateBudgetUseCase).updateBudget(any());
    }

    @Test
    void delete_success_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/budgets/1"))
                .andExpect(status().isNoContent());

        verify(deactivateBudgetUseCase).deactivateBudget(any(), any());
    }
}
