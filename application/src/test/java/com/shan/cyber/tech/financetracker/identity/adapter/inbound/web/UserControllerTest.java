package com.shan.cyber.tech.financetracker.identity.adapter.inbound.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shan.cyber.tech.financetracker.identity.domain.exception.UserNotFoundException;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.UpdateUserProfileUseCase;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.UserProfile;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.GlobalExceptionHandler;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.SecurityContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UpdateUserProfileUseCase updateUserProfileUseCase;

    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setCurrentUserId(TEST_USER_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    void updateMe_validRequest_returns200WithUpdatedProfile() throws Exception {
        UserProfile updated = new UserProfile(TEST_USER_ID, "johndoe", "john@example.com", "Jane", "Smith", "EUR", OffsetDateTime.now());
        when(updateUserProfileUseCase.updateProfile(any())).thenReturn(updated);

        String body = objectMapper.writeValueAsString(new UpdateProfileRequestDto("Jane", "Smith", "EUR"));

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.preferredCurrency").value("EUR"));
    }

    @Test
    void updateMe_blankFirstName_returns422() throws Exception {
        String body = objectMapper.writeValueAsString(new UpdateProfileRequestDto("", "Smith", "USD"));

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void updateMe_invalidCurrencyFormat_returns422() throws Exception {
        String body = objectMapper.writeValueAsString(new UpdateProfileRequestDto("Jane", "Smith", "us"));

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void updateMe_userNotFound_returns422() throws Exception {
        when(updateUserProfileUseCase.updateProfile(any()))
                .thenThrow(new UserNotFoundException("User not found"));

        String body = objectMapper.writeValueAsString(new UpdateProfileRequestDto("Jane", "Smith", "EUR"));

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"));
    }
}
