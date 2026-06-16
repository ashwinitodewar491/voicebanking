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

public class API5_GetTransactionsListTest extends BaseApiPage {

    private JsonNode getTransactionsResponse() throws Exception {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);
        requestBody.put("toDate", Constants.TRANSACTION_TO_DATE);
        requestBody.put("page", Constants.DEFAULT_PAGE);
        requestBody.put("size", Constants.DEFAULT_PAGE_SIZE);

        return apiClient.post(
                Endpoints.TRANSACTIONS_LIST,
                requestBody);
    }

    @Test(groups = {"regression", "api"}, description = "Should validate transactions API response structure")
    public void testTransactionsResponse() throws Exception {

        JsonNode response = getTransactionsResponse();

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
                Constants.TRANSACTIONS_LIST_SUCCESS_MESSAGE);

        Assert.assertTrue(response.has("data"));
    }

    @Test(groups = {"regression", "api"}, description = "Should validate transaction list and pagination details")
    public void testTransactionListPagination() throws Exception {

        JsonNode data = getTransactionsResponse().get("data");

        Assert.assertTrue(data.has("transactionList"));
        Assert.assertTrue(data.get("transactionList").isArray());

        Assert.assertTrue(data.has("page"));
        Assert.assertTrue(data.has("size"));
        Assert.assertTrue(data.has("totalElements"));
        Assert.assertTrue(data.has("totalPages"));

        Assert.assertTrue(
                data.get("page").asInt() >= 0,
                "Invalid page number");

        Assert.assertTrue(
                data.get("size").asInt() > 0,
                "Invalid page size");
    }

    @Test(groups = {"regression", "api"}, description = "Should validate transaction schema and values")
    public void testTransactionDataValidation() throws Exception {

        JsonNode transactions
                = getTransactionsResponse()
                        .get("data")
                        .get("transactionList");

        for (JsonNode transaction : transactions) {

            // Mandatory fields
            Assert.assertTrue(transaction.has("transactionId"));
            Assert.assertTrue(transaction.has("amount"));
            Assert.assertTrue(transaction.has("type"));
            Assert.assertTrue(transaction.has("description"));

            // Non-empty validations
            Assert.assertFalse(
                    transaction.get("transactionId").asText().isBlank(),
                    "transactionId is empty");

            Assert.assertFalse(
                    transaction.get("type").asText().isBlank(),
                    "transaction type is empty");

            Assert.assertFalse(
                    transaction.get("description").asText().isBlank(),
                    "description is empty");

            // Amount validations
            Assert.assertTrue(
                    transaction.get("amount").isNumber(),
                    "Amount should be numeric");

            Assert.assertTrue(
                    transaction.get("amount").asDouble() >= 0,
                    "Amount should not be negative");
        }
    }

    // --- Date Range Tests ---

    @Test(groups = {"regression", "api"}, description = "Should fetch transactions for the last 1 month")
    public void testTransactionsLastOneMonth() throws Exception {
        String fmt = "yyyy-MM-dd";
        String fromDate = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern(fmt));
        String toDate = LocalDate.now().format(DateTimeFormatter.ofPattern(fmt));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);
        requestBody.put("fromDate", fromDate);
        requestBody.put("toDate", toDate);
        requestBody.put("page", Constants.DEFAULT_PAGE);
        requestBody.put("size", Constants.DEFAULT_PAGE_SIZE);

        JsonNode response = apiClient.post(Endpoints.TRANSACTIONS_LIST, requestBody);

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.SUCCESS_STATUS);

        Assert.assertEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE);

        Assert.assertEquals(
                response.get("message").asText(),
                Constants.TRANSACTIONS_LIST_SUCCESS_MESSAGE);

        JsonNode data = response.get("data");
        Assert.assertTrue(data.has("transactionList"), "transactionList should be present");
        Assert.assertTrue(data.get("transactionList").isArray(), "transactionList should be an array");
        Assert.assertTrue(data.get("totalElements").asInt() >= 0, "totalElements should be non-negative");
    }

    @Test(groups = {"regression", "api"}, description = "Should fetch transactions for the last 2 months")
    public void testTransactionsLastTwoMonths() throws Exception {
        String fmt = "yyyy-MM-dd";
        String fromDate = LocalDate.now().minusMonths(2).format(DateTimeFormatter.ofPattern(fmt));
        String toDate = LocalDate.now().format(DateTimeFormatter.ofPattern(fmt));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);
        requestBody.put("fromDate", fromDate);
        requestBody.put("toDate", toDate);
        requestBody.put("page", Constants.DEFAULT_PAGE);
        requestBody.put("size", Constants.DEFAULT_PAGE_SIZE);

        JsonNode response = apiClient.post(Endpoints.TRANSACTIONS_LIST, requestBody);

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.SUCCESS_STATUS);

        Assert.assertEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE);

        Assert.assertEquals(
                response.get("message").asText(),
                Constants.TRANSACTIONS_LIST_SUCCESS_MESSAGE);

        JsonNode data = response.get("data");
        Assert.assertTrue(data.has("transactionList"), "transactionList should be present");
        Assert.assertTrue(data.get("transactionList").isArray(), "transactionList should be an array");
        Assert.assertTrue(data.get("totalElements").asInt() >= 0, "totalElements should be non-negative");
    }

    @Test(groups = {"regression", "api"}, description = "Should fetch transactions for the last 6 months")
    public void testTransactionsLastSixMonths() throws Exception {
        String fmt = "yyyy-MM-dd";
        String fromDate = LocalDate.now().minusMonths(6).format(DateTimeFormatter.ofPattern(fmt));
        String toDate = LocalDate.now().format(DateTimeFormatter.ofPattern(fmt));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);
        requestBody.put("fromDate", fromDate);
        requestBody.put("toDate", toDate);
        requestBody.put("page", Constants.DEFAULT_PAGE);
        requestBody.put("size", Constants.DEFAULT_PAGE_SIZE);

        JsonNode response = apiClient.post(Endpoints.TRANSACTIONS_LIST, requestBody);

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.SUCCESS_STATUS);

        Assert.assertEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE);

        Assert.assertEquals(
                response.get("message").asText(),
                Constants.TRANSACTIONS_LIST_SUCCESS_MESSAGE);

        JsonNode data = response.get("data");
        Assert.assertTrue(data.has("transactionList"), "transactionList should be present");
        Assert.assertTrue(data.get("transactionList").isArray(), "transactionList should be an array");
        Assert.assertTrue(data.get("totalElements").asInt() >= 0, "totalElements should be non-negative");
    }

    // --- Negative Tests ---

    @Test(groups = {"negative", "regression", "api"},
            description = "Transactions API should reject non-existent account ID")
    public void testTransactionsWithInvalidAccountId() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.INVALID_ACCOUNT_ID);
        requestBody.put("toDate", Constants.TRANSACTION_TO_DATE);
        requestBody.put("page", Constants.DEFAULT_PAGE);
        requestBody.put("size", Constants.DEFAULT_PAGE_SIZE);

        JsonNode response = apiClient.post(Endpoints.TRANSACTIONS_LIST, requestBody);

        Assert.assertNotEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API should reject non-existent accountId");

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.ERROR_STATUS,
                "Response status should be error");

        Assert.assertEquals(
                response.get("error").get("message").asText(),
                Constants.ERR_ACCOUNT_NOT_FOUND,
                "Error message should indicate account not found");
    }

    @Test(groups = {"negative", "regression", "api"},
            description = "Transactions API should return error for invalid date format")
    public void testTransactionsWithInvalidDateFormat() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);
        requestBody.put("toDate", Constants.INVALID_DATE_FORMAT);
        requestBody.put("page", Constants.DEFAULT_PAGE);
        requestBody.put("size", Constants.DEFAULT_PAGE_SIZE);

        JsonNode response = apiClient.post(Endpoints.TRANSACTIONS_LIST, requestBody);

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
                "INTERNAL_ERROR",
                "Error code should indicate internal error for unparseable date");
    }

    @Test(groups = {"negative", "regression", "api"},
            description = "Transactions API should reject negative page size")
    public void testTransactionsWithNegativePageSize() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);
        requestBody.put("toDate", Constants.TRANSACTION_TO_DATE);
        requestBody.put("page", Constants.DEFAULT_PAGE);
        requestBody.put("size", -1);

        JsonNode response = apiClient.post(Endpoints.TRANSACTIONS_LIST, requestBody);

        Assert.assertNotEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API should reject negative page size");

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.ERROR_STATUS,
                "Response status should be error");

        Assert.assertEquals(
                response.get("error").get("message").asText(),
                Constants.ERR_PAGE_SIZE_MIN,
                "Error message should indicate minimum page size requirement");
    }

    @Test(groups = {"negative", "regression", "api"},
            description = "Transactions API should reject request with missing fields")
    public void testTransactionsWithMissingFields() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();

        JsonNode response = apiClient.post(Endpoints.TRANSACTIONS_LIST, requestBody);

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
                Constants.ERR_ACCOUNT_ID_REQUIRED,
                "Error message should indicate missing accountId");
    }
}
