package com.voicebanking.tests.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.voicebanking.DataText.Constants;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.pages.BaseApiPage;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class API1_GetAccountListTest extends BaseApiPage {

    @Test(description = "Should retrieve account list for a customer")
    public void testGetAccountList() throws Exception {

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);

        JsonNode response = apiClient.post(Endpoints.ACCOUNT_LIST, requestBody);

        // Verify response structure
        Assert.assertEquals(response.get("status").asText(), Constants.SUCCESS_STATUS);
        Assert.assertEquals(response.get("statusCode").asInt(), Constants.SUCCESS_STATUS_CODE);

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

    @Test(description = "Should contain account details in list")
    public void testAccountListContainsDetails() throws Exception {

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);

        JsonNode response = apiClient.post(Endpoints.ACCOUNT_LIST, requestBody);
        JsonNode accountList = response.get("data").get("accountList");
        Assert.assertTrue(
                accountList.size() > 0,
                "Account list should not be empty");

        if (accountList.size() > 0) {
            JsonNode account = accountList.get(0);

            Assert.assertTrue(account.has("accountId"));
            Assert.assertTrue(account.has("accountType"));
            Assert.assertTrue(account.has("status"));
        }
    }

    @Test(description = "Should validate all account details dynamically")
public void testValidateAccountDetails() throws Exception {

    Map<String, String> requestBody = new HashMap<>();
    requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);

    JsonNode response = apiClient.post(Endpoints.ACCOUNT_LIST, requestBody);
    JsonNode accountList = response.get("data").get("accountList");

    Assert.assertTrue(
            accountList.size() > 0,
            "Account list should not be empty");

    boolean savingsAccountFound = false;
    boolean currentAccountFound = false;

    for (JsonNode account : accountList) {

        // Verify mandatory fields
        Assert.assertTrue(
                account.has("accountId"),
                "accountId field is missing");

        Assert.assertTrue(
                account.has("accountType"),
                "accountType field is missing");

        Assert.assertTrue(
                account.has("status"),
                "status field is missing");

        Assert.assertTrue(
                account.has("createdAt"),
                "createdAt field is missing");

        Assert.assertTrue(
                account.has("updatedAt"),
                "updatedAt field is missing");

        // Verify field values
        Assert.assertFalse(
                account.get("accountId").asText().isBlank(),
                "accountId should not be empty");

        Assert.assertEquals(
                account.get("status").asText(),
                Constants.ACTIVE_STATUS,
                "Invalid account status");

        String accountType = account.get("accountType").asText();

        if (Constants.SAVINGS_ACCOUNT_TYPE.equals(accountType)) {
            savingsAccountFound = true;
        }

        if (Constants.CURRENT_ACCOUNT_TYPE.equals(accountType)) {
            currentAccountFound = true;
        }
    }

    Assert.assertTrue(
            savingsAccountFound,
            "SAVINGS account type not found");

    Assert.assertTrue(
            currentAccountFound,
            "CURRENT account type not found");
}
}