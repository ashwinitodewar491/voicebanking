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
                maskedAccount.matches(Constants.MASKED_ACCOUNT_REGEX),
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

    // --- Negative Tests ---

    @Test(groups = {"negative", "regression", "api"},
            description = "Account balance API should reject non-existent account ID")
    public void testAccountBalanceWithInvalidAccountId() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.INVALID_ACCOUNT_ID);
        requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);
        requestBody.put("accountType", Constants.SAVINGS_ACCOUNT_TYPE);

        JsonNode response = apiClient.post(Endpoints.ACCOUNT_BALANCE, requestBody);

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
            description = "Account balance API looks up by accountId only — customerId is not validated")
    public void testAccountBalanceWithInvalidCustomerId() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);
        requestBody.put("customerId", Constants.INVALID_CUSTOMER_ID);
        requestBody.put("accountType", Constants.SAVINGS_ACCOUNT_TYPE);

        JsonNode response = apiClient.post(Endpoints.ACCOUNT_BALANCE, requestBody);

        Assert.assertEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API returns success — customerId is not strictly validated");

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.SUCCESS_STATUS,
                "Response status should be success");
    }

    @Test(groups = {"negative", "regression", "api"},
            description = "Account balance API ignores invalid accountType and returns data by accountId")
    public void testAccountBalanceWithInvalidAccountType() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);
        requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);
        requestBody.put("accountType", Constants.INVALID_ACCOUNT_TYPE);

        JsonNode response = apiClient.post(Endpoints.ACCOUNT_BALANCE, requestBody);

        Assert.assertEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API returns success — accountType parameter is not validated");

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.SUCCESS_STATUS,
                "Response status should be success");
    }

    @Test(groups = {"negative", "regression", "api"},
            description = "Account balance API should reject request with all empty fields")
    public void testAccountBalanceWithEmptyFields() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("accountId", "");
        requestBody.put("customerId", "");
        requestBody.put("accountType", "");

        JsonNode response = apiClient.post(Endpoints.ACCOUNT_BALANCE, requestBody);

        Assert.assertNotEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API should reject empty fields");

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
