package com.voicebanking.tests.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.DataText.Constants;
import com.voicebanking.pages.BaseApiPage;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class API6_TransferMoneyTest extends BaseApiPage {

    @Test(description = "Should transfer money successfully")
    public void testTransferMoney() throws Exception {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);
        requestBody.put("fromAccountId", Constants.EXISTING_ACCOUNT_ID);
        requestBody.put("beneficiaryId", "a3c7e2b5-6f44-4a01-8c45-3f8c2e7b4d04");
        requestBody.put("amount", 1000.00);
        requestBody.put("description", "IMPS to Shailesh Kumar");

        JsonNode response = apiClient.post(
                Endpoints.TRANSFER_MONEY,
                requestBody);

        Assert.assertEquals(response.get("status").asText(), Constants.SUCCESS_STATUS);
        Assert.assertEquals(response.get("statusCode").asInt(), Constants.SUCCESS_STATUS_CODE);
        Assert.assertTrue(
                response.has("message"),
                Constants.MESSAGE_EXIST);

        Assert.assertEquals(
                response.get("message").asText(),
                Constants.TRANSFER_MONEY_SUCCESS_MESSAGE);
        Assert.assertTrue(response.has("data"));
    }

    @Test(description = "Transfer response should contain transaction details")
    public void testTransferResponse() throws Exception {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);
        requestBody.put("fromAccountId", Constants.EXISTING_ACCOUNT_ID);
        requestBody.put("beneficiaryId", "a3c7e2b5-6f44-4a01-8c45-3f8c2e7b4d04");
        requestBody.put("amount", 1000.00);
        requestBody.put("description", "IMPS to Shailesh Kumar");

        JsonNode response = apiClient.post(
                Endpoints.TRANSFER_MONEY,
                requestBody);

        JsonNode data = response.get("data");

        Assert.assertTrue(data.has("transactionId"));
        Assert.assertTrue(data.has("status"));
        Assert.assertTrue(data.has("balanceAfterTxn"));
        Assert.assertTrue(data.has("transactionDate"));
    }

    @Test(description = "Transfer status should be SUCCESS")
    public void testTransferStatus() throws Exception {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);
        requestBody.put("fromAccountId", Constants.EXISTING_ACCOUNT_ID);
        requestBody.put("beneficiaryId", "a3c7e2b5-6f44-4a01-8c45-3f8c2e7b4d04");
        requestBody.put("amount", 500.00);
        requestBody.put("description", "IMPS Transfer");

        JsonNode response = apiClient.post(
                Endpoints.TRANSFER_MONEY,
                requestBody);

        JsonNode data = response.get("data");

        Assert.assertEquals(
                data.get("status").asText(),
                "SUCCESS",
                "Transfer status should be SUCCESS");
    }

    @Test(description = "Balance after transaction should be numeric")
    public void testBalanceAfterTransfer() throws Exception {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);
        requestBody.put("fromAccountId", Constants.EXISTING_ACCOUNT_ID);
        requestBody.put("beneficiaryId", "a3c7e2b5-6f44-4a01-8c45-3f8c2e7b4d04");
        requestBody.put("amount", 500.00);
        requestBody.put("description", "IMPS Transfer");

        JsonNode response = apiClient.post(
                Endpoints.TRANSFER_MONEY,
                requestBody);

        JsonNode data = response.get("data");

        Assert.assertTrue(
                data.get("balanceAfterTxn").isNumber(),
                "Balance after transaction should be numeric");

        Assert.assertTrue(
                data.get("balanceAfterTxn").asDouble() >= 0,
                "Balance after transaction should not be negative");
    }
}