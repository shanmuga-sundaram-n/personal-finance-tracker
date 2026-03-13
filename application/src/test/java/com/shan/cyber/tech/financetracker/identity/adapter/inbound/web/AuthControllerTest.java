package com.shan.cyber.tech.financetracker.identity.adapter.inbound.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shan.cyber.tech.financetracker.identity.domain.exception.DuplicateUsernameException;
import com.shan.cyber.tech.financetracker.identity.domain.exception.InvalidCredentialsException;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.AuthenticateUserUseCase;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.GetCurrentUserQuery;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.LoginResult;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.LogoutUseCase;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.RegisterUserUseCase;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.UserProfile;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.GlobalExceptionHandler;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.SecurityContextHolder;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private RegisterUserUseCase registerUserUseCase;
    @MockBean private AuthenticateUserUseCase authenticateUserUseCase;
    @MockBean private LogoutUseCase logoutUseCase;
    @MockBean private GetCurrentUserQuery getCurrentUserQuery;

    private static final Long TEST_USER_ID = 1L;
    private static final UserProfile TEST_PROFILE = new UserProfile(
            TEST_USER_ID, "johndoe", "john@example.com", "John", "Doe", "USD", OffsetDateTime.now());

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setCurrentUserId(TEST_USER_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    void register_validRequest_returns201WithProfile() throws Exception {
        when(registerUserUseCase.registerUser(any())).thenReturn(new UserId(TEST_USER_ID));
        when(getCurrentUserQuery.getCurrentUser(any())).thenReturn(TEST_PROFILE);

        String body = objectMapper.writeValueAsString(new RegisterRequestDto(
                "johndoe", "john@example.com", "password123", "John", "Doe"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void register_blankUsername_returns422() throws Exception {
        String body = objectMapper.writeValueAsString(new RegisterRequestDto(
                "", "john@example.com", "password123", "John", "Doe"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void register_shortPassword_returns422() throws Exception {
        String body = objectMapper.writeValueAsString(new RegisterRequestDto(
                "johndoe", "john@example.com", "short", "John", "Doe"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void register_duplicateUsername_returns422() throws Exception {
        when(registerUserUseCase.registerUser(any())).thenThrow(new DuplicateUsernameException("johndoe"));

        String body = objectMapper.writeValueAsString(new RegisterRequestDto(
                "johndoe", "john@example.com", "password123", "John", "Doe"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("DUPLICATE_USERNAME"));
    }

    @Test
    void login_validCredentials_returnsToken() throws Exception {
        LoginResult loginResult = new LoginResult("test-token", OffsetDateTime.now().plusDays(7));
        when(authenticateUserUseCase.authenticate(any())).thenReturn(loginResult);

        String body = objectMapper.writeValueAsString(new LoginRequestDto("johndoe", "password123"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-token"));
    }

    @Test
    void login_invalidCredentials_returns422() throws Exception {
        when(authenticateUserUseCase.authenticate(any()))
                .thenThrow(new InvalidCredentialsException());

        String body = objectMapper.writeValueAsString(new LoginRequestDto("johndoe", "wrong"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void logout_validToken_returns204() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer some-token"))
                .andExpect(status().isNoContent());

        verify(logoutUseCase).logout("some-token");
    }

    @Test
    void logout_noToken_returns204WithoutCallingLogout() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent());

        verify(logoutUseCase, never()).logout(any());
    }

    @Test
    void me_authenticatedUser_returns200WithProfile() throws Exception {
        when(getCurrentUserQuery.getCurrentUser(any())).thenReturn(TEST_PROFILE);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_USER_ID))
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.preferredCurrency").value("USD"));
    }
}
