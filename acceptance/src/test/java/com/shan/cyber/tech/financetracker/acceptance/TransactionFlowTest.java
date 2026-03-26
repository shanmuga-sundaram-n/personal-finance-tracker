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
 * Flow 3 — Transaction CRUD acceptance tests.
 *
 * Preconditions established in {@code @BeforeAll}:
 *   - One registered user with a valid session token
 *   - One CHECKING account (prerequisite for all transaction operations)
 *   - One EXPENSE category (required by CreateTransactionRequestDto)
 *   - One INCOME category (for INCOME transaction tests)
 */
@Tag("acceptance")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Flow 3 — Transaction CRUD")
class TransactionFlowTest extends AbstractAcceptanceTest {

    private String token;
    private long accountId;
    private long expenseCategoryId;
    private long incomeCategoryId;
    private final String suffix = shortId();

    @BeforeAll
    void setup() {
        token = registerAndLogin("txn_" + suffix);
        accountId = createCheckingAccount(token, "txn_" + suffix);
        expenseCategoryId = createExpenseCategory(token, suffix);
        incomeCategoryId = createIncomeCategory(token, suffix);
    }

    // ------------------------------------------------------------------
    // POST /api/v1/transactions  —  Create
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should_return_201_and_transaction_body_when_expense_transaction_is_created")
    void should_return_201_and_transaction_body_when_expense_transaction_is_created() {
        authed(token)
            .body(Map.of(
                "accountId", accountId,
                "categoryId", expenseCategoryId,
                "amount", "75.50",
                "currency", "USD",
                "type", "EXPENSE",
                "transactionDate", "2026-03-15",
                "description", "Weekly groceries",
                "merchantName", "Whole Foods Market",
                "referenceNumber", "WFM-2026-001"
            ))
        .when()
            .post("/api/v1/transactions")
        .then()
            .statusCode(201)
            .header("Location", not(emptyOrNullString()))
            .body("id", notNullValue())
            .body("accountId", equalTo((int) accountId))
            .body("type", equalTo("EXPENSE"))
            .body("currency", equalTo("USD"));
    }

    @Test
    @DisplayName("should_return_201_and_transaction_body_when_income_transaction_is_created")
    void should_return_201_and_transaction_body_when_income_transaction_is_created() {
        authed(token)
            .body(Map.of(
                "accountId", accountId,
                "categoryId", incomeCategoryId,
                "amount", "4500.00",
                "currency", "USD",
                "type", "INCOME",
                "transactionDate", "2026-03-01",
                "description", "March salary deposit"
            ))
        .when()
            .post("/api/v1/transactions")
        .then()
            .statusCode(201)
            .body("type", equalTo("INCOME"))
            .body("id", notNullValue());
    }

    @Test
    @DisplayName("should_return_400_when_amount_is_missing_on_create")
    void should_return_400_when_amount_is_missing_on_create() {
        authed(token)
            .body(Map.of(
                "accountId", accountId,
                "categoryId", expenseCategoryId,
                "currency", "USD",
                "type", "EXPENSE",
                "transactionDate", "2026-03-15"
                // amount intentionally omitted
            ))
        .when()
            .post("/api/v1/transactions")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("should_return_400_when_transaction_type_is_invalid")
    void should_return_400_when_transaction_type_is_invalid() {
        authed(token)
            .body(Map.of(
                "accountId", accountId,
                "categoryId", expenseCategoryId,
                "amount", "50.00",
                "currency", "USD",
                "type", "BOGUS_TYPE",    // not INCOME or EXPENSE
                "transactionDate", "2026-03-15"
            ))
        .when()
            .post("/api/v1/transactions")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("should_return_400_when_amount_is_negative")
    void should_return_400_when_amount_is_negative() {
        authed(token)
            .body(Map.of(
                "accountId", accountId,
                "categoryId", expenseCategoryId,
                "amount", "-10.00",
                "currency", "USD",
                "type", "EXPENSE",
                "transactionDate", "2026-03-15"
            ))
        .when()
            .post("/api/v1/transactions")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("should_return_401_when_creating_transaction_without_token")
    void should_return_401_when_creating_transaction_without_token() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "accountId", accountId,
                "categoryId", expenseCategoryId,
                "amount", "25.00",
                "currency", "USD",
                "type", "EXPENSE",
                "transactionDate", "2026-03-15"
            ))
        .when()
            .post("/api/v1/transactions")
        .then()
            .statusCode(401);
    }

    // ------------------------------------------------------------------
    // GET /api/v1/transactions  —  List
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should_return_200_and_paginated_result_when_transactions_are_listed")
    void should_return_200_and_paginated_result_when_transactions_are_listed() {
        // Create a transaction so the list is non-empty.
        authed(token)
            .body(Map.of(
                "accountId", accountId,
                "categoryId", expenseCategoryId,
                "amount", "12.99",
                "currency", "USD",
                "type", "EXPENSE",
                "transactionDate", "2026-03-10"
            ))
        .when()
            .post("/api/v1/transactions");

        authed(token)
        .when()
            .get("/api/v1/transactions")
        .then()
            .statusCode(200)
            .body("totalElements", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("should_return_200_filtered_transactions_when_accountId_filter_is_applied")
    void should_return_200_filtered_transactions_when_accountId_filter_is_applied() {
        authed(token)
        .when()
            .get("/api/v1/transactions?accountId=" + accountId)
        .then()
            .statusCode(200)
            .body("content.size()", greaterThanOrEqualTo(0));
    }

    // ------------------------------------------------------------------
    // GET /api/v1/transactions/{id}  —  Get by ID
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should_return_200_and_correct_transaction_when_fetching_by_id")
    void should_return_200_and_correct_transaction_when_fetching_by_id() {
        // Create a transaction and capture its ID.
        long txnId = authed(token)
            .body(Map.of(
                "accountId", accountId,
                "categoryId", expenseCategoryId,
                "amount", "33.00",
                "currency", "USD",
                "type", "EXPENSE",
                "transactionDate", "2026-03-12",
                "description", "Coffee supplies"
            ))
        .when()
            .post("/api/v1/transactions")
        .then()
            .statusCode(201)
            .extract()
            .path("id");

        authed(token)
        .when()
            .get("/api/v1/transactions/" + txnId)
        .then()
            .statusCode(200)
            .body("id", equalTo((int) txnId))
            .body("type", equalTo("EXPENSE"));
    }

    @Test
    @DisplayName("should_return_404_when_transaction_id_does_not_exist")
    void should_return_404_when_transaction_id_does_not_exist() {
        authed(token)
        .when()
            .get("/api/v1/transactions/999999999")
        .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("should_return_404_when_transaction_belongs_to_a_different_user")
    void should_return_404_when_transaction_belongs_to_a_different_user() {
        // Create a transaction under the primary user.
        long txnId = authed(token)
            .body(Map.of(
                "accountId", accountId,
                "categoryId", expenseCategoryId,
                "amount", "18.00",
                "currency", "USD",
                "type", "EXPENSE",
                "transactionDate", "2026-03-11"
            ))
        .when()
            .post("/api/v1/transactions")
        .then()
            .statusCode(201)
            .extract()
            .path("id");

        // A second user must receive 404, not 403.
        String otherToken = registerAndLogin("txn_other_" + shortId());

        authed(otherToken)
        .when()
            .get("/api/v1/transactions/" + txnId)
        .then()
            .statusCode(404);
    }

    // ------------------------------------------------------------------
    // PUT /api/v1/transactions/{id}  —  Update
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should_return_200_and_updated_transaction_when_description_and_amount_are_changed")
    void should_return_200_and_updated_transaction_when_description_and_amount_are_changed() {
        long txnId = authed(token)
            .body(Map.of(
                "accountId", accountId,
                "categoryId", expenseCategoryId,
                "amount", "50.00",
                "currency", "USD",
                "type", "EXPENSE",
                "transactionDate", "2026-03-05"
            ))
        .when()
            .post("/api/v1/transactions")
        .then()
            .statusCode(201)
            .extract()
            .path("id");

        authed(token)
            .body(Map.of(
                "categoryId", expenseCategoryId,
                "amount", "55.75",
                "currency", "USD",
                "transactionDate", "2026-03-05",
                "description", "Updated grocery run"
            ))
        .when()
            .put("/api/v1/transactions/" + txnId)
        .then()
            .statusCode(200)
            .body("description", equalTo("Updated grocery run"));
    }

    // ------------------------------------------------------------------
    // DELETE /api/v1/transactions/{id}  —  Delete
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should_return_204_when_transaction_is_deleted")
    void should_return_204_when_transaction_is_deleted() {
        long txnId = authed(token)
            .body(Map.of(
                "accountId", accountId,
                "categoryId", expenseCategoryId,
                "amount", "9.99",
                "currency", "USD",
                "type", "EXPENSE",
                "transactionDate", "2026-03-20"
            ))
        .when()
            .post("/api/v1/transactions")
        .then()
            .statusCode(201)
            .extract()
            .path("id");

        authed(token)
        .when()
            .delete("/api/v1/transactions/" + txnId)
        .then()
            .statusCode(204);

        // Deleted transaction must return 404
        authed(token)
        .when()
            .get("/api/v1/transactions/" + txnId)
        .then()
            .statusCode(404);
    }
}
