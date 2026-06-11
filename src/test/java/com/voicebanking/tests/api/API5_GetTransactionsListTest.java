package com.voicebanking.tests.api;

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
}
