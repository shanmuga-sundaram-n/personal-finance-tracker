package com.shan.cyber.tech.financetracker.budget.adapter.inbound.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shan.cyber.tech.financetracker.budget.domain.exception.BudgetNotFoundException;
import com.shan.cyber.tech.financetracker.budget.domain.exception.DuplicateActiveBudgetException;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.BudgetPlanCategoryGroup;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.BudgetPlanCategoryRow;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.BudgetPlanTotals;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.BudgetPlanView;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.BudgetView;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.CreateBudgetUseCase;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.DeactivateBudgetUseCase;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.GetBudgetPlanQuery;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.GetBudgetsQuery;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.UpdateBudgetUseCase;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.UpsertBudgetByCategoryUseCase;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BudgetController.class)
@Import({GlobalExceptionHandler.class, BudgetRequestMapper.class})
class BudgetControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private CreateBudgetUseCase createBudgetUseCase;
    @MockBean private UpdateBudgetUseCase updateBudgetUseCase;
    @MockBean private DeactivateBudgetUseCase deactivateBudgetUseCase;
    @MockBean private GetBudgetsQuery getBudgetsQuery;
    @MockBean private GetBudgetPlanQuery getBudgetPlanQuery;
    @MockBean private UpsertBudgetByCategoryUseCase upsertBudgetByCategoryUseCase;

    private static final Long TEST_USER_ID = 1L;
    private static final BudgetView SAMPLE_VIEW = new BudgetView(
            1L, 5L, "Groceries", "MONTHLY", "500.0000", "USD",
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
            false, 80, true, "200.0000", "300.0000", 40.0, false, null, null, null);

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
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].categoryName").value("Groceries"))
                .andExpect(jsonPath("$.content[0].periodType").value("MONTHLY"));
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

    // -------------------------------------------------------------------------
    // GET /api/v1/budgets/plan
    // -------------------------------------------------------------------------

    @Test
    void getBudgetPlan_validDates_returns200WithCorrectStructure() throws Exception {
        BudgetPlanView planView = buildSamplePlanView();
        when(getBudgetPlanQuery.getBudgetPlan(any(), eq(LocalDate.of(2025, 3, 1)), eq(LocalDate.of(2025, 3, 31))))
                .thenReturn(planView);

        mockMvc.perform(get("/api/v1/budgets/plan")
                        .param("startDate", "2025-03-01")
                        .param("endDate", "2025-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.startDate").value("2025-03-01"))
                .andExpect(jsonPath("$.endDate").value("2025-03-31"))
                .andExpect(jsonPath("$.incomeRows").isArray())
                .andExpect(jsonPath("$.expenseGroups").isArray())
                .andExpect(jsonPath("$.incomeTotals").exists())
                .andExpect(jsonPath("$.expenseTotals").exists());
    }

    @Test
    void getBudgetPlan_validDates_incomeRowFieldsSerializedCorrectly() throws Exception {
        BudgetPlanView planView = buildSamplePlanView();
        when(getBudgetPlanQuery.getBudgetPlan(any(), any(), any())).thenReturn(planView);

        mockMvc.perform(get("/api/v1/budgets/plan")
                        .param("startDate", "2025-03-01")
                        .param("endDate", "2025-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incomeRows[0].categoryId").value(1))
                .andExpect(jsonPath("$.incomeRows[0].categoryName").value("Salary"))
                .andExpect(jsonPath("$.incomeRows[0].budgetId").value(10))
                .andExpect(jsonPath("$.incomeRows[0].budgetedAmount").value("3000.0000"))
                .andExpect(jsonPath("$.incomeRows[0].actualAmount").value("2500.00"))
                .andExpect(jsonPath("$.incomeRows[0].varianceAmount").value("500.0000"))
                .andExpect(jsonPath("$.incomeRows[0].percentUsed").value(83.33))
                .andExpect(jsonPath("$.incomeRows[0].hasBudget").value(true));
    }

    @Test
    void getBudgetPlan_validDates_expenseTotalsSerializedCorrectly() throws Exception {
        BudgetPlanView planView = buildSamplePlanView();
        when(getBudgetPlanQuery.getBudgetPlan(any(), any(), any())).thenReturn(planView);

        mockMvc.perform(get("/api/v1/budgets/plan")
                        .param("startDate", "2025-03-01")
                        .param("endDate", "2025-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expenseTotals.totalBudgeted").value("600.0000"))
                .andExpect(jsonPath("$.expenseTotals.totalActual").value("480.00"))
                .andExpect(jsonPath("$.expenseTotals.totalVariance").value("120.0000"))
                .andExpect(jsonPath("$.expenseTotals.totalPercentUsed").value(80.0));
    }

    @Test
    void getBudgetPlan_missingStartDate_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/budgets/plan")
                        .param("endDate", "2025-03-31"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBudgetPlan_missingEndDate_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/budgets/plan")
                        .param("startDate", "2025-03-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBudgetPlan_invalidDateFormat_returns400() throws Exception {
        // Spring cannot bind "March 2025" to a LocalDate with ISO format — expects 400
        mockMvc.perform(get("/api/v1/budgets/plan")
                        .param("startDate", "March 2025")
                        .param("endDate", "2025-03-31"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBudgetPlan_noAuthUser_returns500() throws Exception {
        // When no user is in the ThreadLocal SecurityContextHolder, getCurrentUserId() throws
        // IllegalStateException, which the generic handler maps to 500. This test documents
        // the current behavior and guards against accidental change in that contract.
        SecurityContextHolder.clear();

        mockMvc.perform(get("/api/v1/budgets/plan")
                        .param("startDate", "2025-03-01")
                        .param("endDate", "2025-03-31"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getBudgetPlan_emptyPlan_returns200WithEmptyArrays() throws Exception {
        // A user with no categories or transactions should receive a valid 200 response with
        // empty row arrays and zero totals — not a 500 from a null/missing structure.
        BudgetPlanTotals zeroTotals = new BudgetPlanTotals(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0.0,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
        BudgetPlanView emptyPlan = new BudgetPlanView(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                "USD",
                List.of(),
                List.of(),
                zeroTotals,
                zeroTotals
        );
        when(getBudgetPlanQuery.getBudgetPlan(any(), any(), any())).thenReturn(emptyPlan);

        mockMvc.perform(get("/api/v1/budgets/plan")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incomeRows").isArray())
                .andExpect(jsonPath("$.incomeRows").isEmpty())
                .andExpect(jsonPath("$.expenseGroups").isArray())
                .andExpect(jsonPath("$.expenseGroups").isEmpty());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/budgets/upsert-by-category
    // -------------------------------------------------------------------------

    @Test
    void upsertByCategory_validRequest_returns200() throws Exception {
        BudgetId budgetId = new BudgetId(42L);
        when(upsertBudgetByCategoryUseCase.upsertBudget(any())).thenReturn(budgetId);
        when(getBudgetsQuery.getById(any(), any())).thenReturn(SAMPLE_VIEW);

        String body = objectMapper.writeValueAsString(new UpsertBudgetByCategoryRequestDto(
                5L, "MONTHLY", new BigDecimal("600.00"), "USD",
                LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31)));

        mockMvc.perform(post("/api/v1/budgets/upsert-by-category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(upsertBudgetByCategoryUseCase).upsertBudget(any());
    }

    @Test
    void upsertByCategory_missingCategoryId_returns422() throws Exception {
        // categoryId is @NotNull — omitting it triggers MethodArgumentNotValidException → 422
        String body = "{\"periodType\":\"MONTHLY\",\"amount\":600,\"currency\":\"USD\","
                + "\"startDate\":\"2025-03-01\",\"endDate\":\"2025-03-31\"}";

        mockMvc.perform(post("/api/v1/budgets/upsert-by-category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void upsertByCategory_invalidPeriodType_returns422() throws Exception {
        String body = "{\"categoryId\":5,\"periodType\":\"FORTNIGHT\",\"amount\":600,"
                + "\"currency\":\"USD\",\"startDate\":\"2025-03-01\",\"endDate\":\"2025-03-31\"}";

        mockMvc.perform(post("/api/v1/budgets/upsert-by-category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a realistic BudgetPlanView for use in controller tests.
     * Income: Salary — budgeted 3000, actual 2500, variance 500, 83.33%
     * Expense: Groceries — budgeted 600, actual 480, variance 120, 80.0%
     */
    private static BudgetPlanView buildSamplePlanView() {
        BudgetPlanCategoryRow salaryRow = new BudgetPlanCategoryRow(
                1L, "Salary", 10L,
                new BigDecimal("3000.0000"),
                new BigDecimal("2500.00"),
                new BigDecimal("500.0000"),
                83.33,
                true,
                "MONTHLY",
                new BigDecimal("3000.0000"),
                new BigDecimal("36000.0000")
        );
        BudgetPlanCategoryRow groceriesRow = new BudgetPlanCategoryRow(
                10L, "Groceries", 20L,
                new BigDecimal("600.0000"),
                new BigDecimal("480.00"),
                new BigDecimal("120.0000"),
                80.0,
                true,
                "MONTHLY",
                new BigDecimal("600.0000"),
                new BigDecimal("7200.0000")
        );
        BudgetPlanTotals incomeTotals = new BudgetPlanTotals(
                new BigDecimal("3000.0000"),
                new BigDecimal("2500.00"),
                new BigDecimal("500.0000"),
                83.33,
                new BigDecimal("3000.0000"),
                new BigDecimal("36000.0000")
        );
        BudgetPlanTotals expenseTotals = new BudgetPlanTotals(
                new BigDecimal("600.0000"),
                new BigDecimal("480.00"),
                new BigDecimal("120.0000"),
                80.0,
                new BigDecimal("600.0000"),
                new BigDecimal("7200.0000")
        );
        // Groceries is an ungrouped leaf → solo group with null parentCategoryId
        BudgetPlanCategoryGroup groceriesGroup = new BudgetPlanCategoryGroup(
                null, "Groceries",
                List.of(groceriesRow),
                new BigDecimal("600.0000"),
                new BigDecimal("7200.0000"),
                new BigDecimal("0.0000"),
                false
        );
        return new BudgetPlanView(
                LocalDate.of(2025, 3, 1),
                LocalDate.of(2025, 3, 31),
                "USD",
                List.of(salaryRow),
                List.of(groceriesGroup),
                incomeTotals,
                expenseTotals
        );
    }
}
