package com.voicebanking.tests.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.DataText.Constants;
import com.voicebanking.pages.BaseApiPage;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class API8_GetLoanStatementTest extends BaseApiPage {

    @Test(description = "Should retrieve loan statement")
    public void testGetLoanStatement() throws Exception {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", "LN10001");
        requestBody.put("fromDate", "2022-03-01");
        requestBody.put("toDate", "2026-09-01");
        requestBody.put("page", 0);
        requestBody.put("size", 10);

        JsonNode response = apiClient.post(
                Endpoints.LOAN_STATEMENT,
                requestBody);

        Assert.assertEquals(response.get("status").asText(), Constants.SUCCESS_STATUS);
        Assert.assertEquals(response.get("statusCode").asInt(), Constants.SUCCESS_STATUS_CODE);
        Assert.assertTrue(
                response.has("message"),
                Constants.MESSAGE_EXIST);

        Assert.assertEquals(
                response.get("message").asText(),
                Constants.LOAN_STATEMENT_SUCCESS_MESSAGE);
        Assert.assertTrue(response.has("data"));
    }

    @Test(description = "Should contain loan transaction list")
    public void testLoanStatement() throws Exception {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", "LN10001");
        requestBody.put("fromDate", "2022-03-01");
        requestBody.put("toDate", "2026-09-01");
        requestBody.put("page", 0);
        requestBody.put("size", 10);

        JsonNode response = apiClient.post(
                Endpoints.LOAN_STATEMENT,
                requestBody);

        JsonNode data = response.get("data");

        Assert.assertTrue(data.has("accountId"));
        Assert.assertTrue(data.has("loanTransactionList"));
        Assert.assertTrue(data.get("loanTransactionList").isArray());
    }

    @Test(description = "Loan transaction should have required fields")
    public void testLoanTransactionFields() throws Exception {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", "LN10001");
        requestBody.put("fromDate", "2022-03-01");
        requestBody.put("toDate", "2026-09-01");
        requestBody.put("page", 0);
        requestBody.put("size", 10);

        JsonNode response = apiClient.post(
                Endpoints.LOAN_STATEMENT,
                requestBody);

        JsonNode transactions = response.get("data").get("loanTransactionList");

        if (transactions.size() > 0) {
            JsonNode transaction = transactions.get(0);

            Assert.assertTrue(transaction.has("amount"));
            Assert.assertTrue(transaction.has("date"));
            Assert.assertTrue(transaction.has("description"));
        }
    }

    @Test(description = "Should contain pagination details")
    public void testPagination() throws Exception {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", "LN10001");
        requestBody.put("fromDate", "2022-03-01");
        requestBody.put("toDate", "2026-09-01");
        requestBody.put("page", 0);
        requestBody.put("size", 10);

        JsonNode response = apiClient.post(
                Endpoints.LOAN_STATEMENT,
                requestBody);

        JsonNode data = response.get("data");

        Assert.assertTrue(data.has("page"));
        Assert.assertTrue(data.has("size"));
        Assert.assertTrue(data.has("totalElements"));
        Assert.assertTrue(data.has("totalPages"));
    }

    @Test(description = "Should validate all loan transactions")
    public void testLoanTransactionData() throws Exception {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.LOAN_ACCOUNT_ID);
        requestBody.put("fromDate", "2022-03-01");
        requestBody.put("toDate", "2026-09-01");
        requestBody.put("page", 0);
        requestBody.put("size", 10);

        JsonNode response = apiClient.post(
                Endpoints.LOAN_STATEMENT,
                requestBody);

        JsonNode transactions = response.get("data").get("loanTransactionList");

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
                    "Transaction date should not be empty");

            Assert.assertFalse(
                    transaction.get("description").asText().isBlank(),
                    "Description should not be empty");
        }
    }

    @Test(description = "Should validate loan statement data values")
    public void testLoanStatementDataValues() throws Exception {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.LOAN_ACCOUNT_ID);
        requestBody.put("fromDate", "2022-03-01");
        requestBody.put("toDate", "2026-09-01");
        requestBody.put("page", 0);
        requestBody.put("size", 10);

        JsonNode response = apiClient.post(
                Endpoints.LOAN_STATEMENT,
                requestBody);

        JsonNode data = response.get("data");

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

        JsonNode firstTransaction = data.get("loanTransactionList").get(0);

        Assert.assertEquals(
                firstTransaction.get("amount").asDouble(),
                Constants.EMI_AMOUNT);

        Assert.assertEquals(
                firstTransaction.get("description").asText(),
                Constants.EMI_PAYMENT_DESCRIPTION);
    }

    @Test(description = "Should validate pagination values")
    public void testPaginationValues() throws Exception {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.LOAN_ACCOUNT_ID);
        requestBody.put("fromDate", "2022-03-01");
        requestBody.put("toDate", "2026-09-01");
        requestBody.put("page", 0);
        requestBody.put("size", 10);

        JsonNode response = apiClient.post(
                Endpoints.LOAN_STATEMENT,
                requestBody);

        JsonNode data = response.get("data");

        Assert.assertTrue(data.get("page").asInt() >= 0);
        Assert.assertTrue(data.get("size").asInt() > 0);
        Assert.assertTrue(data.get("totalElements").asInt() >= 0);
        Assert.assertTrue(data.get("totalPages").asInt() > 0);
    }
}