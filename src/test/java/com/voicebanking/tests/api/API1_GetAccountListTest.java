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

    @Test(groups = {"smoke", "sanity", "regression"}, description = "Verify account list API response structure")
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

    @Test(groups = {"smoke", "sanity", "regression"}, description = "Verify account details and account types")
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
}
