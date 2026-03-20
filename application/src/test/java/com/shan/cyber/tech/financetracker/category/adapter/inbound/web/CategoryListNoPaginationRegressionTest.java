package com.shan.cyber.tech.financetracker.category.adapter.inbound.web;

import com.shan.cyber.tech.financetracker.category.domain.port.inbound.CategoryView;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.CreateCategoryUseCase;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.DeactivateCategoryUseCase;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.GetCategoriesQuery;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.UpdateCategoryUseCase;
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

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression test for BUG: GET /api/v1/categories silently truncated results
 * when the user had more than 50 total categories (system + custom combined).
 *
 * Root cause: the old implementation used @PageableDefault(size=50) with
 * in-memory slicing, so Page 0 always contained at most 50 entries.
 * A user with 49 system categories + 10 custom categories received only
 * 50 items — 9 custom categories were invisible in the UI.
 *
 * Fix: CategoryController.list() now returns List<CategoryResponseDto> directly
 * with no pagination wrapper. The response body is a JSON array (not an object
 * with a "content" field) and all items are returned in a single call.
 *
 * This test verifies both aspects of the fix:
 * 1. The response body is a plain JSON array — NOT a paginated envelope.
 * 2. When the service returns more than 50 categories, ALL of them appear
 *    in the single response — none are truncated.
 */
@WebMvcTest(CategoryController.class)
@Import(GlobalExceptionHandler.class)
class CategoryListNoPaginationRegressionTest {

    // 49 = exact number of system categories seeded by 007_seed_system_categories.yml
    // (16 EXPENSE parents + 7 INCOME parents + 1 TRANSFER parent + 25 EXPENSE children)
    private static final int SYSTEM_CATEGORY_COUNT = 49;

    // 10 user-created categories push the total to 59, which exceeds the old page size of 50.
    // Before the fix, the 9 categories numbered 51–59 were silently dropped.
    private static final int USER_CATEGORY_COUNT = 10;

    private static final int TOTAL_CATEGORIES = SYSTEM_CATEGORY_COUNT + USER_CATEGORY_COUNT;

    @Autowired private MockMvc mockMvc;

    @MockBean private CreateCategoryUseCase createCategoryUseCase;
    @MockBean private UpdateCategoryUseCase updateCategoryUseCase;
    @MockBean private DeactivateCategoryUseCase deactivateCategoryUseCase;
    @MockBean private GetCategoriesQuery getCategoriesQuery;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setCurrentUserId(1L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    void should_return_all_categories_when_total_exceeds_50() throws Exception {
        // Arrange — build a list whose size exceeds the old @PageableDefault(size=50) limit
        List<CategoryView> allCategories = buildCategoryViews(TOTAL_CATEGORIES);
        when(getCategoriesQuery.getByOwner(any())).thenReturn(allCategories);

        // Act & Assert
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                // The root element must be a JSON array, not a paginated envelope.
                // If $.content existed the top-level size() check below would fail
                // (it would report 0 items in the array because the array IS the
                // wrapper object), which is the exact symptom of the original bug.
                .andExpect(jsonPath("$", hasSize(TOTAL_CATEGORIES)))
                // Spot-check that the 51st item (index 50) is present — this is the
                // first item that the old page-0 response silently omitted.
                .andExpect(jsonPath("$[50].id").value(51L))
                .andExpect(jsonPath("$[50].name").value("Category-51"))
                // Spot-check the last item to confirm no trailing truncation.
                .andExpect(jsonPath("$[" + (TOTAL_CATEGORIES - 1) + "].id").value((long) TOTAL_CATEGORIES))
                .andExpect(jsonPath("$[" + (TOTAL_CATEGORIES - 1) + "].name").value("Category-" + TOTAL_CATEGORIES));
    }

    @Test
    void should_return_plain_array_not_paginated_envelope() throws Exception {
        // Arrange — even a small result set must not be wrapped in { "content": [...] }
        List<CategoryView> oneCategory = buildCategoryViews(1);
        when(getCategoriesQuery.getByOwner(any())).thenReturn(oneCategory);

        // Act & Assert
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                // A paginated envelope would produce a JSON object here, causing
                // jsonPath("$[0]") to fail and jsonPath("$.content[0]") to succeed.
                // Both checks together pin the exact contract.
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Category-1"))
                // $.content must not exist — presence of this field would mean the
                // frontend's .then(d => d.content) path is still required.
                .andExpect(jsonPath("$.content").doesNotExist());
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private static List<CategoryView> buildCategoryViews(int count) {
        List<CategoryView> views = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            views.add(new CategoryView(
                    (long) i,
                    "Category-" + i,
                    "EXPENSE",
                    "Expense",
                    null,
                    null,
                    null,
                    null,
                    i <= SYSTEM_CATEGORY_COUNT,   // first 49 are system categories
                    true,
                    null));
        }
        return views;
    }
}
