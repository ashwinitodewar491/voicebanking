package com.voicebanking.tests.api;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.voicebanking.DataText.Constants;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.pages.BaseApiPage;

public class API1_GetAccountListTest extends BaseApiPage {

    private JsonNode getAccountListResponse() throws Exception {

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);

        return apiClient.post(
                Endpoints.ACCOUNT_LIST,
                requestBody);
    }

    @Test(groups = {"smoke", "regression", "api"}, description = "Verify account list API response structure")
    public void testGetAccountList() throws Exception {

        JsonNode response = getAccountListResponse();

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
                Constants.ACCOUNT_LIST_SUCCESS_MESSAGE);

        Assert.assertTrue(response.has("data"));
        Assert.assertTrue(response.get("data").has("accountList"));
        Assert.assertTrue(response.get("data").get("accountList").isArray());
    }

    @Test(groups = {"smoke", "regression"}, description = "Verify account details and account types")
    public void testValidateAccountDetails() throws Exception {

        JsonNode response = getAccountListResponse();
        JsonNode accountList = response.get("data").get("accountList");

        Assert.assertFalse(
                accountList.isEmpty(),
                "Account list should not be empty");

        boolean savingsAccountFound = false;
        boolean currentAccountFound = false;

        for (JsonNode account : accountList) {

            Assert.assertTrue(account.has("accountId"));
            Assert.assertTrue(account.has("accountType"));
            Assert.assertTrue(account.has("status"));
            Assert.assertTrue(account.has("createdAt"));
            Assert.assertTrue(account.has("updatedAt"));

            Assert.assertFalse(
                    account.get("accountId").asText().isBlank(),
                    "Account Id should not be empty");

            Assert.assertEquals(
                    account.get("status").asText(),
                    Constants.ACTIVE_STATUS);

            String accountType
                    = account.get("accountType").asText();

            switch (accountType) {

                case Constants.SAVINGS_ACCOUNT_TYPE:
                    savingsAccountFound = true;
                    break;

                case Constants.CURRENT_ACCOUNT_TYPE:
                    currentAccountFound = true;
                    break;

                default:
                    Assert.fail(
                            "Unexpected account type found : "
                            + accountType);
            }
        }

        Assert.assertTrue(
                savingsAccountFound,
                "Savings account not found");

        Assert.assertTrue(
                currentAccountFound,
                "Current account not found");
    }

    // --- Negative Tests ---

    @Test(groups = {"negative", "regression", "api"},
            description = "Account list API returns empty list for non-existent customer ID")
    public void testGetAccountListWithInvalidCustomerId() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("customerId", Constants.INVALID_CUSTOMER_ID);

        JsonNode response = apiClient.post(Endpoints.ACCOUNT_LIST, requestBody);

        Assert.assertEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API returns success for non-existent customerId");

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.SUCCESS_STATUS,
                "Response status should be success");

        Assert.assertTrue(
                response.get("data").get("accountList").isEmpty(),
                "Account list should be empty for non-existent customerId");
    }

    @Test(groups = {"negative", "regression", "api"},
            description = "Account list API should reject empty customer ID")
    public void testGetAccountListWithEmptyCustomerId() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("customerId", "");

        JsonNode response = apiClient.post(Endpoints.ACCOUNT_LIST, requestBody);

        Assert.assertNotEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API should reject empty customerId");

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.ERROR_STATUS,
                "Response status should be error");

        Assert.assertEquals(
                response.get("error").get("message").asText(),
                Constants.ERR_CUSTOMER_ID_REQUIRED,
                "Error message should indicate missing customerId");
    }

    @Test(groups = {"negative", "regression", "api"},
            description = "Account list API should reject request with missing customer ID field")
    public void testGetAccountListWithMissingCustomerId() throws Exception {
        Map<String, String> requestBody = new HashMap<>();

        JsonNode response = apiClient.post(Endpoints.ACCOUNT_LIST, requestBody);

        Assert.assertNotEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API should reject request missing customerId");

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.ERROR_STATUS,
                "Response status should be error");

        Assert.assertEquals(
                response.get("error").get("message").asText(),
                Constants.ERR_CUSTOMER_ID_REQUIRED,
                "Error message should indicate missing customerId");
    }
}
