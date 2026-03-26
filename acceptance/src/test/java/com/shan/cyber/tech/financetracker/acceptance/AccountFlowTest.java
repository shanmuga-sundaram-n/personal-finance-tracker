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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Flow 2 — Account CRUD acceptance tests.
 *
 * Covers: create, list, get by ID, update, soft-delete.
 * All tests share one registered user and session token.
 */
@Tag("acceptance")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Flow 2 — Account CRUD")
class AccountFlowTest extends AbstractAcceptanceTest {

    private String token;
    private final String suffix = shortId();

    @BeforeAll
    void setup() {
        token = registerAndLogin("acct_" + suffix);
    }

    // ------------------------------------------------------------------
    // POST /api/v1/accounts  —  Create
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should_return_201_and_account_body_when_account_is_created")
    void should_return_201_and_account_body_when_account_is_created() {
        authed(token)
            .body(Map.of(
                "name", "Primary Checking " + suffix,
                "accountTypeCode", "CHECKING",
                "initialBalance", "2500.00",
                "currency", "USD",
                "institutionName", "First National",
                "accountNumberLast4", "4321"
            ))
        .when()
            .post("/api/v1/accounts")
        .then()
            .statusCode(201)
            .header("Location", not(emptyOrNullString()))
            .body("id", notNullValue())
            .body("name", equalTo("Primary Checking " + suffix))
            .body("accountTypeCode", equalTo("CHECKING"))
            .body("currentBalance", equalTo("2500.0000"))
            .body("currency", equalTo("USD"))
            .body("isActive", equalTo(true));
    }

    @Test
    @DisplayName("should_return_400_when_required_fields_are_missing_on_create")
    void should_return_400_when_required_fields_are_missing_on_create() {
        authed(token)
            // name and accountTypeCode are missing
            .body(Map.of(
                "initialBalance", "100.00",
                "currency", "USD"
            ))
        .when()
            .post("/api/v1/accounts")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("should_return_400_when_currency_code_is_not_3_uppercase_letters")
    void should_return_400_when_currency_code_is_not_3_uppercase_letters() {
        authed(token)
            .body(Map.of(
                "name", "Bad Currency Account",
                "accountTypeCode", "SAVINGS",
                "initialBalance", "500.00",
                "currency", "us"       // lowercase — invalid pattern
            ))
        .when()
            .post("/api/v1/accounts")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("should_return_401_when_authorization_header_is_absent_on_create")
    void should_return_401_when_authorization_header_is_absent_on_create() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", "Ghost Account",
                "accountTypeCode", "CHECKING",
                "initialBalance", "0.00",
                "currency", "USD"
            ))
        .when()
            .post("/api/v1/accounts")
        .then()
            .statusCode(401);
    }

    // ------------------------------------------------------------------
    // GET /api/v1/accounts  —  List
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should_return_200_and_list_containing_created_account_when_accounts_are_listed")
    void should_return_200_and_list_containing_created_account_when_accounts_are_listed() {
        // Create one account to guarantee the list is non-empty.
        createCheckingAccount(token, "list_" + shortId());

        authed(token)
        .when()
            .get("/api/v1/accounts")
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("should_return_401_when_listing_accounts_without_token")
    void should_return_401_when_listing_accounts_without_token() {
        given()
        .when()
            .get("/api/v1/accounts")
        .then()
            .statusCode(401);
    }

    // ------------------------------------------------------------------
    // GET /api/v1/accounts/{id}  —  Get by ID
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should_return_200_and_correct_account_when_fetching_by_id")
    void should_return_200_and_correct_account_when_fetching_by_id() {
        long accountId = createCheckingAccount(token, "get_" + shortId());

        authed(token)
        .when()
            .get("/api/v1/accounts/" + accountId)
        .then()
            .statusCode(200)
            .body("id", equalTo((int) accountId))
            .body("accountTypeCode", equalTo("CHECKING"));
    }

    @Test
    @DisplayName("should_return_404_when_account_id_does_not_exist")
    void should_return_404_when_account_id_does_not_exist() {
        authed(token)
        .when()
            .get("/api/v1/accounts/999999999")
        .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("should_return_404_when_account_belongs_to_a_different_user")
    void should_return_404_when_account_belongs_to_a_different_user() {
        // Create account under owner
        long ownedAccountId = createCheckingAccount(token, "owned_" + shortId());

        // Second user should NOT be able to see owner's account — returns 404, not 403
        String otherToken = registerAndLogin("acct_other_" + shortId());

        authed(otherToken)
        .when()
            .get("/api/v1/accounts/" + ownedAccountId)
        .then()
            .statusCode(404);
    }

    // ------------------------------------------------------------------
    // PUT /api/v1/accounts/{id}  —  Update
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should_return_200_and_updated_account_when_name_and_institution_are_changed")
    void should_return_200_and_updated_account_when_name_and_institution_are_changed() {
        long accountId = createCheckingAccount(token, "upd_" + shortId());
        String newName = "Renamed Checking " + shortId();

        authed(token)
            .body(Map.of(
                "name", newName,
                "institutionName", "New Bank"
            ))
        .when()
            .put("/api/v1/accounts/" + accountId)
        .then()
            .statusCode(200)
            .body("name", equalTo(newName))
            .body("institutionName", equalTo("New Bank"));
    }

    @Test
    @DisplayName("should_return_400_when_update_payload_has_blank_name")
    void should_return_400_when_update_payload_has_blank_name() {
        long accountId = createCheckingAccount(token, "bad_upd_" + shortId());

        authed(token)
            .body(Map.of("name", ""))   // blank name — fails @NotBlank
        .when()
            .put("/api/v1/accounts/" + accountId)
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("should_return_404_when_updating_an_account_that_does_not_exist")
    void should_return_404_when_updating_an_account_that_does_not_exist() {
        authed(token)
            .body(Map.of("name", "Ghost Update"))
        .when()
            .put("/api/v1/accounts/999999998")
        .then()
            .statusCode(404);
    }

    // ------------------------------------------------------------------
    // DELETE /api/v1/accounts/{id}  —  Soft-delete
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should_return_204_when_account_is_deactivated")
    void should_return_204_when_account_is_deactivated() {
        long accountId = createCheckingAccount(token, "del_" + shortId());

        authed(token)
        .when()
            .delete("/api/v1/accounts/" + accountId)
        .then()
            .statusCode(204);
    }

    @Test
    @DisplayName("should_return_404_when_fetching_a_deactivated_account")
    void should_return_404_when_fetching_a_deactivated_account() {
        long accountId = createCheckingAccount(token, "del_check_" + shortId());

        // Soft-delete
        authed(token)
        .when()
            .delete("/api/v1/accounts/" + accountId)
        .then()
            .statusCode(204);

        // Deactivated account must not be visible via GET
        authed(token)
        .when()
            .get("/api/v1/accounts/" + accountId)
        .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("should_return_404_when_deleting_an_account_that_does_not_exist")
    void should_return_404_when_deleting_an_account_that_does_not_exist() {
        authed(token)
        .when()
            .delete("/api/v1/accounts/999999997")
        .then()
            .statusCode(404);
    }

    // ------------------------------------------------------------------
    // GET /api/v1/accounts/net-worth
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should_return_200_and_net_worth_summary_when_user_has_accounts")
    void should_return_200_and_net_worth_summary_when_user_has_accounts() {
        // Ensure at least one account exists
        createCheckingAccount(token, "nw_" + shortId());

        authed(token)
        .when()
            .get("/api/v1/accounts/net-worth")
        .then()
            .statusCode(200)
            .body("totalAssets", notNullValue())
            .body("totalLiabilities", notNullValue())
            .body("netWorth", notNullValue());
    }
}
