package com.shan.cyber.tech.financetracker.acceptance;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Flow 1 — Authentication acceptance tests.
 *
 * Covers: register, login (valid + wrong password), logout.
 * Each test uses its own credentials to avoid ordering dependencies.
 */
@Tag("acceptance")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Flow 1 — Auth")
class AuthFlowTest extends AbstractAcceptanceTest {

    private String sharedToken;
    private final String suffix = shortId();

    @BeforeAll
    void registerSharedUser() {
        sharedToken = registerAndLogin(suffix);
    }

    // ------------------------------------------------------------------
    // POST /api/v1/auth/register
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should_return_201_and_user_profile_when_registration_succeeds")
    void should_return_201_and_user_profile_when_registration_succeeds() {
        String s = shortId();

        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "username", "newuser_" + s,
                "email", "newuser_" + s + "@acceptance.test",
                "password", "Str0ng!Pass",
                "firstName", "Jane",
                "lastName", "Doe"
            ))
        .when()
            .post("/api/v1/auth/register")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("username", not(emptyOrNullString()))
            .body("email", not(emptyOrNullString()));
    }

    @Test
    @DisplayName("should_return_422_when_username_is_already_taken")
    void should_return_422_when_username_is_already_taken() {
        // The shared user was already registered in @BeforeAll; re-registering must fail.
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "username", "testuser_" + suffix,
                "email", "different_" + suffix + "@acceptance.test",
                "password", "Str0ng!Pass",
                "firstName", "Dup",
                "lastName", "User"
            ))
        .when()
            .post("/api/v1/auth/register")
        .then()
            .statusCode(422);
    }

    @Test
    @DisplayName("should_return_400_when_registration_payload_is_missing_required_fields")
    void should_return_400_when_registration_payload_is_missing_required_fields() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "username", "nopass_" + shortId()
                // password, firstName, lastName, email intentionally omitted
            ))
        .when()
            .post("/api/v1/auth/register")
        .then()
            .statusCode(400);
    }

    // ------------------------------------------------------------------
    // POST /api/v1/auth/login
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should_return_200_and_session_token_when_credentials_are_valid")
    void should_return_200_and_session_token_when_credentials_are_valid() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "username", "testuser_" + suffix,
                "password", "Str0ng!Pass"
            ))
        .when()
            .post("/api/v1/auth/login")
        .then()
            .statusCode(200)
            .body("token", not(emptyOrNullString()))
            .body("expiresAt", notNullValue());
    }

    @Test
    @DisplayName("should_return_401_when_password_is_incorrect")
    void should_return_401_when_password_is_incorrect() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "username", "testuser_" + suffix,
                "password", "WrongPassword99!"
            ))
        .when()
            .post("/api/v1/auth/login")
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("should_return_401_when_username_does_not_exist")
    void should_return_401_when_username_does_not_exist() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "username", "ghost_user_" + shortId(),
                "password", "Str0ng!Pass"
            ))
        .when()
            .post("/api/v1/auth/login")
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("should_return_400_when_login_payload_is_empty")
    void should_return_400_when_login_payload_is_empty() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post("/api/v1/auth/login")
        .then()
            .statusCode(400);
    }

    // ------------------------------------------------------------------
    // POST /api/v1/auth/logout
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should_return_204_when_valid_token_is_provided_on_logout")
    void should_return_204_when_valid_token_is_provided_on_logout() {
        // Obtain a dedicated token for this test so the shared token stays valid.
        String logoutSuffix = shortId();
        String tokenToInvalidate = registerAndLogin(logoutSuffix);

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + tokenToInvalidate)
        .when()
            .post("/api/v1/auth/logout")
        .then()
            .statusCode(204);
    }

    @Test
    @DisplayName("should_return_401_when_accessing_protected_endpoint_after_logout")
    void should_return_401_when_accessing_protected_endpoint_after_logout() {
        // Register + login a fresh user, then log out and verify protected endpoint is locked.
        String s = shortId();
        String token = registerAndLogin(s);

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
        .when()
            .post("/api/v1/auth/logout")
        .then()
            .statusCode(204);

        // After logout, any protected call must return 401.
        authed(token)
        .when()
            .get("/api/v1/accounts")
        .then()
            .statusCode(401);
    }

    // ------------------------------------------------------------------
    // GET /api/v1/auth/me
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should_return_200_and_profile_when_valid_session_exists")
    void should_return_200_and_profile_when_valid_session_exists() {
        authed(sharedToken)
        .when()
            .get("/api/v1/auth/me")
        .then()
            .statusCode(200)
            .body("username", not(emptyOrNullString()))
            .body("email", not(emptyOrNullString()));
    }

    @Test
    @DisplayName("should_return_401_when_no_authorization_header_is_sent")
    void should_return_401_when_no_authorization_header_is_sent() {
        given()
        .when()
            .get("/api/v1/auth/me")
        .then()
            .statusCode(401);
    }
}
