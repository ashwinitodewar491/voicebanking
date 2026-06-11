package com.voicebanking.tests.api;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.voicebanking.DataText.Constants;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.pages.BaseApiPage;

public class API3_GetAccountBalanceTest extends BaseApiPage {

    private JsonNode getAccountBalanceResponse() throws Exception {

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);
        requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);
        requestBody.put("accountType", Constants.SAVINGS_ACCOUNT_TYPE);

        return apiClient.post(
                Endpoints.ACCOUNT_BALANCE,
                requestBody);
    }

    @Test(groups = {"smoke", "regression", "api"}, description = "Should validate account balance API response")
    public void testAccountBalanceResponse() throws Exception {

        JsonNode response = getAccountBalanceResponse();

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
                Constants.ACCOUNT_BALANCE_SUCCESS_MESSAGE);

        Assert.assertTrue(
                response.has("data"),
                "Data node is missing");
    }

    @Test(groups = {"smoke", "regression", "api"}, description = "Should validate account balance data and business rules")
    public void testAccountBalanceDataValidation() throws Exception {

        JsonNode data = getAccountBalanceResponse().get("data");

        // Mandatory Fields
        String[] requiredFields = {
            "accountId",
            "accountNumberMasked",
            "accountType",
            "balance",
            "status"
        };

        for (String field : requiredFields) {
            Assert.assertTrue(
                    data.has(field),
                    field + " field is missing");
        }

        // Account ID
        Assert.assertEquals(
                data.get("accountId").asText(),
                Constants.EXPECTED_ACCOUNT_ID,
                "Account ID mismatch");

        // Masked Account Number
        String maskedAccount = data.get("accountNumberMasked").asText();

        Assert.assertEquals(
                maskedAccount,
                Constants.EXPECTED_MASKED_ACCOUNT,
                "Masked account number mismatch");

        Assert.assertTrue(
                maskedAccount.matches(".*\\d{4}$"),
                "Masked account should end with 4 digits");

        // Account Type
        Assert.assertEquals(
                data.get("accountType").asText(),
                Constants.SAVINGS_ACCOUNT_TYPE,
                "Account type mismatch");

        // Status
        Assert.assertEquals(
                data.get("status").asText(),
                Constants.ACTIVE_STATUS,
                "Account status mismatch");

        // Balance
        Assert.assertTrue(
                data.get("balance").isNumber(),
                "Balance should be numeric");

        Assert.assertTrue(
                data.get("balance").asDouble() >= 0,
                "Balance should not be negative");

        Assert.assertEquals(
                data.get("balance").asDouble(),
                Constants.EXPECTED_ACCOUNT_BALANCE,
                0.01,
                "Balance mismatch");

        // Non-empty validations
        Assert.assertFalse(
                data.get("accountId").asText().isBlank(),
                "accountId should not be empty");

        Assert.assertFalse(
                maskedAccount.isBlank(),
                "accountNumberMasked should not be empty");
    }
}
