package com.voicebanking.tests.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.DataText.Constants;
import com.voicebanking.pages.BaseApiPage;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class API5_GetTransactionsListTest extends BaseApiPage {

    @Test(description = "Should retrieve transactions list")
    public void testGetTransactionsList() throws Exception {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);
        requestBody.put("toDate", "2026-05-26");
        requestBody.put("page", 0);
        requestBody.put("size", 100);

        JsonNode response = apiClient.post(
                Endpoints.TRANSACTIONS_LIST,
                requestBody);

        Assert.assertEquals(response.get("status").asText(), Constants.SUCCESS_STATUS);
        Assert.assertEquals(response.get("statusCode").asInt(), Constants.SUCCESS_STATUS_CODE);
        Assert.assertTrue(
                response.has("message"),
                Constants.MESSAGE_EXIST);

        Assert.assertEquals(
                response.get("message").asText(),
                Constants.TRANSACTIONS_LIST_SUCCESS_MESSAGE);
        Assert.assertTrue(response.has("data"));
    }

    @Test(description = "Should contain transaction list with pagination")
    public void testTransactionsListPagination() throws Exception {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);
        requestBody.put("toDate", "2026-05-26");
        requestBody.put("page", 0);
        requestBody.put("size", 100);

        JsonNode response = apiClient.post(
                Endpoints.TRANSACTIONS_LIST,
                requestBody);

        JsonNode data = response.get("data");

        Assert.assertTrue(data.has("transactionList"));
        Assert.assertTrue(data.get("transactionList").isArray());
        Assert.assertTrue(data.has("page"));
        Assert.assertTrue(data.has("size"));
        Assert.assertTrue(data.has("totalElements"));
        Assert.assertTrue(data.has("totalPages"));
    }

    @Test(description = "Transaction should have required fields")
    public void testTransactionFields() throws Exception {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);
        requestBody.put("toDate", "2026-05-26");
        requestBody.put("page", 0);
        requestBody.put("size", 10);

        JsonNode response = apiClient.post(
                Endpoints.TRANSACTIONS_LIST,
                requestBody);

        JsonNode transactions = response.get("data").get("transactionList");

        if (transactions.size() > 0) {
            JsonNode transaction = transactions.get(0);

            Assert.assertTrue(transaction.has("transactionId"));
            Assert.assertTrue(transaction.has("amount"));
            Assert.assertTrue(transaction.has("type"));
            Assert.assertTrue(transaction.has("description"));
        }
    }

    @Test(description = "Amount should be numeric")
    public void testTransactionAmount() throws Exception {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);
        requestBody.put("toDate", "2026-05-26");
        requestBody.put("page", 0);
        requestBody.put("size", 10);

        JsonNode response = apiClient.post(
                Endpoints.TRANSACTIONS_LIST,
                requestBody);

        JsonNode transactions = response.get("data").get("transactionList");

        if (transactions.size() > 0) {
            JsonNode transaction = transactions.get(0);

            Assert.assertTrue(
                    transaction.get("amount").isNumber(),
                    "Amount should be numeric");

            Assert.assertTrue(
                    transaction.get("amount").asDouble() >= 0,
                    "Amount should not be negative");
        }
    }
}