package com.voicebanking.tests.api;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.voicebanking.DataText.Constants;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.pages.BaseApiPage;

public class API8_GetLoanStatementTest extends BaseApiPage {

    private JsonNode getLoanStatementResponse() throws Exception {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.LOAN_ACCOUNT_ID);
        requestBody.put("fromDate", Constants.LOAN_STATEMENT_FROM_DATE);
        requestBody.put("toDate", Constants.LOAN_STATEMENT_TO_DATE);
        requestBody.put("page", Constants.LOAN_STATEMENT_PAGE);
        requestBody.put("size", Constants.LOAN_STATEMENT_SIZE);

        return apiClient.post(
                Endpoints.LOAN_STATEMENT,
                requestBody);
    }

    @Test(groups = {"smoke", "regression", "api"}, description = "Should validate loan statement response structure")
    public void testLoanStatementResponse() throws Exception {

        JsonNode response = getLoanStatementResponse();

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.SUCCESS_STATUS);

        Assert.assertEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE);

        Assert.assertTrue(
                response.has("message"),
                Constants.MESSAGE_EXIST);

        Assert.assertEquals(
                response.get("message").asText(),
                Constants.LOAN_STATEMENT_SUCCESS_MESSAGE);

        JsonNode data = response.get("data");

        Assert.assertTrue(data.has("accountId"));
        Assert.assertTrue(data.has("loanTransactionList"));
        Assert.assertTrue(data.get("loanTransactionList").isArray());

        Assert.assertTrue(data.has("page"));
        Assert.assertTrue(data.has("size"));
        Assert.assertTrue(data.has("totalElements"));
        Assert.assertTrue(data.has("totalPages"));
    }

    @Test(groups = {"regression", "api"}, description = "Should validate loan statement data and transactions")
    public void testLoanStatementDataValidation() throws Exception {

        JsonNode response = getLoanStatementResponse();
        JsonNode data = response.get("data");

        // Account & Pagination Validation
        Assert.assertEquals(
                data.get("accountId").asText(),
                Constants.LOAN_ACCOUNT_ID);

        Assert.assertEquals(
                data.get("page").asInt(),
                Constants.LOAN_STATEMENT_PAGE);

        Assert.assertEquals(
                data.get("size").asInt(),
                Constants.LOAN_STATEMENT_SIZE);

        Assert.assertEquals(
                data.get("totalElements").asInt(),
                Constants.LOAN_STATEMENT_TOTAL_ELEMENTS);

        Assert.assertEquals(
                data.get("totalPages").asInt(),
                Constants.LOAN_STATEMENT_TOTAL_PAGES);

        JsonNode transactions = data.get("loanTransactionList");

        Assert.assertTrue(
                transactions.size() > 0,
                "Loan transaction list should not be empty");

        for (JsonNode transaction : transactions) {

            Assert.assertTrue(transaction.has("amount"));
            Assert.assertTrue(transaction.has("date"));
            Assert.assertTrue(transaction.has("description"));

            Assert.assertTrue(
                    transaction.get("amount").isNumber(),
                    "Amount should be numeric");

            Assert.assertTrue(
                    transaction.get("amount").asDouble() > 0,
                    "Amount should be greater than zero");

            Assert.assertFalse(
                    transaction.get("date").asText().isBlank(),
                    "Date should not be empty");

            Assert.assertFalse(
                    transaction.get("description").asText().isBlank(),
                    "Description should not be empty");
        }

        // Validate first transaction
        JsonNode firstTransaction = transactions.get(0);

        Assert.assertEquals(
                firstTransaction.get("amount").asDouble(),
                Constants.EMI_AMOUNT);

        Assert.assertEquals(
                firstTransaction.get("description").asText(),
                Constants.EMI_PAYMENT_DESCRIPTION);
    }

    // --- Date Range Tests ---

    @Test(groups = {"regression", "api"}, description = "Should fetch loan statement for the last 1 month")
    public void testLoanStatementLastOneMonth() throws Exception {
        String fmt = Constants.DATE_FORMAT_PATTERN;
        String fromDate = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern(fmt));
        String toDate = LocalDate.now().format(DateTimeFormatter.ofPattern(fmt));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.LOAN_ACCOUNT_ID);
        requestBody.put("fromDate", fromDate);
        requestBody.put("toDate", toDate);
        requestBody.put("page", Constants.LOAN_STATEMENT_PAGE);
        requestBody.put("size", Constants.LOAN_STATEMENT_SIZE);

        JsonNode response = apiClient.post(Endpoints.LOAN_STATEMENT, requestBody);

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.SUCCESS_STATUS);

        Assert.assertEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE);

        Assert.assertEquals(
                response.get("message").asText(),
                Constants.LOAN_STATEMENT_SUCCESS_MESSAGE);

        JsonNode data = response.get("data");
        Assert.assertTrue(data.has("loanTransactionList"), "loanTransactionList should be present");
        Assert.assertTrue(data.get("loanTransactionList").isArray(), "loanTransactionList should be an array");
        Assert.assertTrue(data.get("totalElements").asInt() >= 0, "totalElements should be non-negative");
    }

    @Test(groups = {"regression", "api"}, description = "Should fetch loan statement for the last 2 months")
    public void testLoanStatementLastTwoMonths() throws Exception {
        String fmt = Constants.DATE_FORMAT_PATTERN;
        String fromDate = LocalDate.now().minusMonths(2).format(DateTimeFormatter.ofPattern(fmt));
        String toDate = LocalDate.now().format(DateTimeFormatter.ofPattern(fmt));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.LOAN_ACCOUNT_ID);
        requestBody.put("fromDate", fromDate);
        requestBody.put("toDate", toDate);
        requestBody.put("page", Constants.LOAN_STATEMENT_PAGE);
        requestBody.put("size", Constants.LOAN_STATEMENT_SIZE);

        JsonNode response = apiClient.post(Endpoints.LOAN_STATEMENT, requestBody);

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.SUCCESS_STATUS);

        Assert.assertEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE);

        Assert.assertEquals(
                response.get("message").asText(),
                Constants.LOAN_STATEMENT_SUCCESS_MESSAGE);

        JsonNode data = response.get("data");
        Assert.assertTrue(data.has("loanTransactionList"), "loanTransactionList should be present");
        Assert.assertTrue(data.get("loanTransactionList").isArray(), "loanTransactionList should be an array");
        Assert.assertTrue(data.get("totalElements").asInt() >= 0, "totalElements should be non-negative");
    }

    @Test(groups = {"regression", "api"}, description = "Should fetch loan statement for the last 6 months")
    public void testLoanStatementLastSixMonths() throws Exception {
        String fmt = Constants.DATE_FORMAT_PATTERN;
        String fromDate = LocalDate.now().minusMonths(6).format(DateTimeFormatter.ofPattern(fmt));
        String toDate = LocalDate.now().format(DateTimeFormatter.ofPattern(fmt));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.LOAN_ACCOUNT_ID);
        requestBody.put("fromDate", fromDate);
        requestBody.put("toDate", toDate);
        requestBody.put("page", Constants.LOAN_STATEMENT_PAGE);
        requestBody.put("size", Constants.LOAN_STATEMENT_SIZE);

        JsonNode response = apiClient.post(Endpoints.LOAN_STATEMENT, requestBody);

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.SUCCESS_STATUS);

        Assert.assertEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE);

        Assert.assertEquals(
                response.get("message").asText(),
                Constants.LOAN_STATEMENT_SUCCESS_MESSAGE);

        JsonNode data = response.get("data");
        Assert.assertTrue(data.has("loanTransactionList"), "loanTransactionList should be present");
        Assert.assertTrue(data.get("loanTransactionList").isArray(), "loanTransactionList should be an array");
        Assert.assertTrue(data.get("totalElements").asInt() >= 0, "totalElements should be non-negative");
    }

    // --- Negative Tests ---

    @Test(groups = {"negative", "regression", "api"},
            description = "Loan statement API should reject non-existent loan account ID")
    public void testLoanStatementWithInvalidAccountId() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.INVALID_LOAN_ACCOUNT_ID);
        requestBody.put("fromDate", Constants.LOAN_STATEMENT_FROM_DATE);
        requestBody.put("toDate", Constants.LOAN_STATEMENT_TO_DATE);
        requestBody.put("page", Constants.LOAN_STATEMENT_PAGE);
        requestBody.put("size", Constants.LOAN_STATEMENT_SIZE);

        JsonNode response = apiClient.post(Endpoints.LOAN_STATEMENT, requestBody);

        Assert.assertNotEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API should reject non-existent loan accountId");

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.ERROR_STATUS,
                "Response status should be error");

        Assert.assertEquals(
                response.get("error").get("message").asText(),
                Constants.ERR_LOAN_NOT_FOUND,
                "Error message should indicate loan details not found");
    }

    @Test(groups = {"negative", "regression", "api"},
            description = "Loan statement API should reject reversed date range (toDate before fromDate)")
    public void testLoanStatementWithReversedDateRange() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.LOAN_ACCOUNT_ID);
        requestBody.put("fromDate", Constants.REVERSED_FROM_DATE);
        requestBody.put("toDate", Constants.REVERSED_TO_DATE);
        requestBody.put("page", Constants.LOAN_STATEMENT_PAGE);
        requestBody.put("size", Constants.LOAN_STATEMENT_SIZE);

        JsonNode response = apiClient.post(Endpoints.LOAN_STATEMENT, requestBody);

        Assert.assertNotEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API should reject toDate earlier than fromDate");

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.ERROR_STATUS,
                "Response status should be error");

        Assert.assertEquals(
                response.get("error").get("message").asText(),
                Constants.ERR_DATE_RANGE_REVERSED,
                "Error message should indicate invalid date range");
    }

    @Test(groups = {"negative", "regression", "api"},
            description = "Loan statement API should return error for invalid date format")
    public void testLoanStatementWithInvalidDateFormat() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.LOAN_ACCOUNT_ID);
        requestBody.put("fromDate", Constants.INVALID_DATE_FORMAT);
        requestBody.put("toDate", Constants.INVALID_DATE_FORMAT);
        requestBody.put("page", Constants.LOAN_STATEMENT_PAGE);
        requestBody.put("size", Constants.LOAN_STATEMENT_SIZE);

        JsonNode response = apiClient.post(Endpoints.LOAN_STATEMENT, requestBody);

        Assert.assertNotEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API should return error for invalid date format");

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.ERROR_STATUS,
                "Response status should be error");

        Assert.assertEquals(
                response.get("error").get("code").asText(),
                Constants.ERR_CODE_INTERNAL_ERROR,
                "Error code should indicate internal error for unparseable date");
    }

    @Test(groups = {"negative", "regression", "api"},
            description = "Loan statement API should reject request with missing fields")
    public void testLoanStatementWithMissingFields() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();

        JsonNode response = apiClient.post(Endpoints.LOAN_STATEMENT, requestBody);

        Assert.assertNotEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API should reject request with missing fields");

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.ERROR_STATUS,
                "Response status should be error");

        Assert.assertEquals(
                response.get("error").get("message").asText(),
                Constants.ERR_LOAN_ACCOUNT_REQUIRED,
                "Error message should indicate account is required");
    }
}
