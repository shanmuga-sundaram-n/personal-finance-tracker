package com.shan.cyber.tech.financetracker.category.adapter.inbound.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shan.cyber.tech.financetracker.category.domain.exception.CategoryNotFoundException;
import com.shan.cyber.tech.financetracker.category.domain.exception.SystemCategoryModificationException;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.CategoryView;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.CreateCategoryUseCase;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.DeactivateCategoryUseCase;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.GetCategoriesQuery;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.UpdateCategoryUseCase;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.GlobalExceptionHandler;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.SecurityContextHolder;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@Import(GlobalExceptionHandler.class)
class CategoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private CreateCategoryUseCase createCategoryUseCase;
    @MockBean private UpdateCategoryUseCase updateCategoryUseCase;
    @MockBean private DeactivateCategoryUseCase deactivateCategoryUseCase;
    @MockBean private GetCategoriesQuery getCategoriesQuery;

    private static final Long TEST_USER_ID = 1L;
    private static final CategoryView SAMPLE_VIEW = new CategoryView(
            1L, "Groceries", "EXPENSE", "Expense", null, null, null, null, false, true, null);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setCurrentUserId(TEST_USER_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    void list_returnsAllCategories() throws Exception {
        when(getCategoriesQuery.getByOwner(any())).thenReturn(List.of(SAMPLE_VIEW));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Groceries"));
    }

    @Test
    void list_withTypeFilter_callsGetByType() throws Exception {
        when(getCategoriesQuery.getByType(any(), eq("EXPENSE"))).thenReturn(List.of(SAMPLE_VIEW));

        mockMvc.perform(get("/api/v1/categories").param("type", "EXPENSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryTypeCode").value("EXPENSE"));

        verify(getCategoriesQuery).getByType(any(), eq("EXPENSE"));
        verify(getCategoriesQuery, never()).getByOwner(any());
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(createCategoryUseCase.createCategory(any())).thenReturn(new CategoryId(1L));
        when(getCategoriesQuery.getById(any(), any())).thenReturn(SAMPLE_VIEW);

        String body = objectMapper.writeValueAsString(
                new CreateCategoryRequestDto("Groceries", "EXPENSE", null, null, null));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Groceries"))
                .andExpect(header().string("Location", "/api/v1/categories/1"));
    }

    @Test
    void create_blankName_returns422() throws Exception {
        String body = objectMapper.writeValueAsString(
                new CreateCategoryRequestDto("", "EXPENSE", null, null, null));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void getById_found_returns200() throws Exception {
        when(getCategoriesQuery.getById(any(), any())).thenReturn(SAMPLE_VIEW);

        mockMvc.perform(get("/api/v1/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(getCategoriesQuery.getById(any(), any()))
                .thenThrow(new CategoryNotFoundException(999L));

        mockMvc.perform(get("/api/v1/categories/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_systemCategory_returns422() throws Exception {
        when(updateCategoryUseCase.updateCategory(any()))
                .thenThrow(new SystemCategoryModificationException());

        String body = objectMapper.writeValueAsString(
                new UpdateCategoryRequestDto("Renamed", null, null));

        mockMvc.perform(put("/api/v1/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivate_success_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/1"))
                .andExpect(status().isNoContent());

        verify(deactivateCategoryUseCase).deactivateCategory(any(), any());
    }

    @Test
    void deactivate_systemCategory_returns403() throws Exception {
        doThrow(new SystemCategoryModificationException())
                .when(deactivateCategoryUseCase).deactivateCategory(any(), any());

        mockMvc.perform(delete("/api/v1/categories/1"))
                .andExpect(status().isForbidden());
    }
}
