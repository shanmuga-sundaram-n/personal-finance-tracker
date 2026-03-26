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
 * Flow 4 — Budget CRUD acceptance tests.
 *
 * Preconditions established in {@code @BeforeAll}:
 *   - One registered user with a valid session token
 *   - One EXPENSE category (required by CreateBudgetRequestDto)
 *
 * Note: Each test that creates a budget uses a distinct category so it does not
 * collide with the one-active-budget-per-category/period invariant.
 */
@Tag("acceptance")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Flow 4 — Budget CRUD")
class BudgetFlowTest extends AbstractAcceptanceTest {

    private String token;
    private final String suffix = shortId();

    @BeforeAll
    void setup() {
        token = registerAndLogin("bgt_" + suffix);
    }

    // ------------------------------------------------------------------
    // POST /api/v1/budgets  —  Create
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should_return_201_and_budget_body_when_monthly_expense_budget_is_created")
    void should_return_201_and_budget_body_when_monthly_expense_budget_is_created() {
        long categoryId = createExpenseCategory(token, "create_" + shortId());

        authed(token)
            .body(Map.of(
                "categoryId", categoryId,
                "periodType", "MONTHLY",
                "amount", "500.00",
                "currency", "USD",
                "startDate", "2026-03-01",
                "rolloverEnabled", false,
                "alertThresholdPct", 80
            ))
        .when()
            .post("/api/v1/budgets")
        .then()
            .statusCode(201)
            .header("Location", not(emptyOrNullString()))
            .body("id", notNullValue())
            .body("periodType", equalTo("MONTHLY"))
            .body("currency", equalTo("USD"))
            .body("isActive", equalTo(true));
    }

    @Test
    @DisplayName("should_return_201_when_budget_uses_custom_period_with_end_date")
    void should_return_201_when_budget_uses_custom_period_with_end_date() {
        long categoryId = createExpenseCategory(token, "custom_" + shortId());

        authed(token)
            .body(Map.of(
                "categoryId", categoryId,
                "periodType", "CUSTOM",
                "amount", "1200.00",
                "currency", "USD",
                "startDate", "2026-01-01",
                "endDate", "2026-12-31",
                "rolloverEnabled", false,
                "alertThresholdPct", 90
            ))
        .when()
            .post("/api/v1/budgets")
        .then()
            .statusCode(201)
            .body("id", notNullValue());
    }

    @Test
    @DisplayName("should_return_400_when_period_type_is_invalid")
    void should_return_400_when_period_type_is_invalid() {
        long categoryId = createExpenseCategory(token, "badperiod_" + shortId());

        authed(token)
            .body(Map.of(
                "categoryId", categoryId,
                "periodType", "FORTNIGHTLY",    // not in the allowed set
                "amount", "300.00",
                "currency", "USD",
                "startDate", "2026-03-01",
                "rolloverEnabled", false
            ))
        .when()
            .post("/api/v1/budgets")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("should_return_400_when_amount_is_missing_on_create")
    void should_return_400_when_amount_is_missing_on_create() {
        long categoryId = createExpenseCategory(token, "noamt_" + shortId());

        authed(token)
            .body(Map.of(
                "categoryId", categoryId,
                "periodType", "MONTHLY",
                "currency", "USD",
                "startDate", "2026-03-01",
                "rolloverEnabled", false
                // amount intentionally omitted
            ))
        .when()
            .post("/api/v1/budgets")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("should_return_422_when_duplicate_budget_is_created_for_same_category_and_period")
    void should_return_422_when_duplicate_budget_is_created_for_same_category_and_period() {
        long categoryId = createExpenseCategory(token, "dup_" + shortId());

        Map<Object, Object> body = Map.of(
            "categoryId", categoryId,
            "periodType", "MONTHLY",
            "amount", "200.00",
            "currency", "USD",
            "startDate", "2026-03-01",
            "rolloverEnabled", false,
            "alertThresholdPct", 75
        );

        // First creation succeeds.
        authed(token).body(body).when().post("/api/v1/budgets").then().statusCode(201);

        // Second creation with the same category/period must fail.
        authed(token)
            .body(body)
        .when()
            .post("/api/v1/budgets")
        .then()
            .statusCode(422);
    }

    @Test
    @DisplayName("should_return_401_when_creating_budget_without_token")
    void should_return_401_when_creating_budget_without_token() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "categoryId", 1,
                "periodType", "MONTHLY",
                "amount", "100.00",
                "currency", "USD",
                "startDate", "2026-03-01",
                "rolloverEnabled", false
            ))
        .when()
            .post("/api/v1/budgets")
        .then()
            .statusCode(401);
    }

    // ------------------------------------------------------------------
    // GET /api/v1/budgets  —  List
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should_return_200_and_paginated_list_when_user_has_active_budgets")
    void should_return_200_and_paginated_list_when_user_has_active_budgets() {
        long categoryId = createExpenseCategory(token, "list_" + shortId());
        authed(token)
            .body(Map.of(
                "categoryId", categoryId,
                "periodType", "MONTHLY",
                "amount", "350.00",
                "currency", "USD",
                "startDate", "2026-03-01",
                "rolloverEnabled", false
            ))
        .when()
            .post("/api/v1/budgets")
        .then()
            .statusCode(201);

        authed(token)
        .when()
            .get("/api/v1/budgets")
        .then()
            .statusCode(200)
            .body("totalElements", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("should_return_401_when_listing_budgets_without_token")
    void should_return_401_when_listing_budgets_without_token() {
        given()
        .when()
            .get("/api/v1/budgets")
        .then()
            .statusCode(401);
    }

    // ------------------------------------------------------------------
    // GET /api/v1/budgets/{id}  —  Get by ID
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should_return_200_and_correct_budget_when_fetching_by_id")
    void should_return_200_and_correct_budget_when_fetching_by_id() {
        long categoryId = createExpenseCategory(token, "getid_" + shortId());
        long budgetId = authed(token)
            .body(Map.of(
                "categoryId", categoryId,
                "periodType", "MONTHLY",
                "amount", "450.00",
                "currency", "USD",
                "startDate", "2026-03-01",
                "rolloverEnabled", false,
                "alertThresholdPct", 80
            ))
        .when()
            .post("/api/v1/budgets")
        .then()
            .statusCode(201)
            .extract()
            .path("id");

        authed(token)
        .when()
            .get("/api/v1/budgets/" + budgetId)
        .then()
            .statusCode(200)
            .body("id", equalTo((int) budgetId))
            .body("periodType", equalTo("MONTHLY"))
            .body("isActive", equalTo(true))
            .body("spentAmount", notNullValue())
            .body("remainingAmount", notNullValue());
    }

    @Test
    @DisplayName("should_return_404_when_budget_id_does_not_exist")
    void should_return_404_when_budget_id_does_not_exist() {
        authed(token)
        .when()
            .get("/api/v1/budgets/999999999")
        .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("should_return_404_when_budget_belongs_to_a_different_user")
    void should_return_404_when_budget_belongs_to_a_different_user() {
        long categoryId = createExpenseCategory(token, "iso_" + shortId());
        long budgetId = authed(token)
            .body(Map.of(
                "categoryId", categoryId,
                "periodType", "MONTHLY",
                "amount", "300.00",
                "currency", "USD",
                "startDate", "2026-03-01",
                "rolloverEnabled", false
            ))
        .when()
            .post("/api/v1/budgets")
        .then()
            .statusCode(201)
            .extract()
            .path("id");

        String otherToken = registerAndLogin("bgt_other_" + shortId());

        authed(otherToken)
        .when()
            .get("/api/v1/budgets/" + budgetId)
        .then()
            .statusCode(404);
    }

    // ------------------------------------------------------------------
    // PUT /api/v1/budgets/{id}  —  Update
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should_return_200_and_updated_budget_when_amount_and_threshold_are_changed")
    void should_return_200_and_updated_budget_when_amount_and_threshold_are_changed() {
        long categoryId = createExpenseCategory(token, "upd_" + shortId());
        long budgetId = authed(token)
            .body(Map.of(
                "categoryId", categoryId,
                "periodType", "MONTHLY",
                "amount", "600.00",
                "currency", "USD",
                "startDate", "2026-03-01",
                "rolloverEnabled", false,
                "alertThresholdPct", 70
            ))
        .when()
            .post("/api/v1/budgets")
        .then()
            .statusCode(201)
            .extract()
            .path("id");

        authed(token)
            .body(Map.of(
                "amount", "750.00",
                "currency", "USD",
                "rolloverEnabled", true,
                "alertThresholdPct", 90
            ))
        .when()
            .put("/api/v1/budgets/" + budgetId)
        .then()
            .statusCode(200)
            .body("alertThresholdPct", equalTo(90));
    }

    @Test
    @DisplayName("should_return_400_when_update_amount_is_zero")
    void should_return_400_when_update_amount_is_zero() {
        long categoryId = createExpenseCategory(token, "zeroamt_" + shortId());
        long budgetId = authed(token)
            .body(Map.of(
                "categoryId", categoryId,
                "periodType", "MONTHLY",
                "amount", "400.00",
                "currency", "USD",
                "startDate", "2026-03-01",
                "rolloverEnabled", false
            ))
        .when()
            .post("/api/v1/budgets")
        .then()
            .statusCode(201)
            .extract()
            .path("id");

        authed(token)
            .body(Map.of(
                "amount", "0",          // fails @Positive
                "currency", "USD",
                "rolloverEnabled", false
            ))
        .when()
            .put("/api/v1/budgets/" + budgetId)
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("should_return_404_when_updating_a_budget_that_does_not_exist")
    void should_return_404_when_updating_a_budget_that_does_not_exist() {
        authed(token)
            .body(Map.of(
                "amount", "200.00",
                "currency", "USD",
                "rolloverEnabled", false
            ))
        .when()
            .put("/api/v1/budgets/999999998")
        .then()
            .statusCode(404);
    }

    // ------------------------------------------------------------------
    // DELETE /api/v1/budgets/{id}  —  Soft-delete
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should_return_204_when_budget_is_deactivated")
    void should_return_204_when_budget_is_deactivated() {
        long categoryId = createExpenseCategory(token, "softdel_" + shortId());
        long budgetId = authed(token)
            .body(Map.of(
                "categoryId", categoryId,
                "periodType", "MONTHLY",
                "amount", "200.00",
                "currency", "USD",
                "startDate", "2026-03-01",
                "rolloverEnabled", false
            ))
        .when()
            .post("/api/v1/budgets")
        .then()
            .statusCode(201)
            .extract()
            .path("id");

        authed(token)
        .when()
            .delete("/api/v1/budgets/" + budgetId)
        .then()
            .statusCode(204);

        // Deactivated budget should no longer appear via GET
        authed(token)
        .when()
            .get("/api/v1/budgets/" + budgetId)
        .then()
            .statusCode(404);
    }
}
