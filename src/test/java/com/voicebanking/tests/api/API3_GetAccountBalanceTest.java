package com.voicebanking.tests.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.DataText.Constants;
import com.voicebanking.pages.BaseApiPage;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class API3_GetAccountBalanceTest extends BaseApiPage {

        @Test(description = "Should retrieve account balance")
        public void testGetAccountBalance() throws Exception {

                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);
                requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);
                requestBody.put("accountType", "SAVINGS");

                JsonNode response = apiClient.post(
                                Endpoints.ACCOUNT_BALANCE,
                                requestBody);

                Assert.assertEquals(response.get("status").asText(), Constants.SUCCESS_STATUS);
                Assert.assertEquals(response.get("statusCode").asInt(), Constants.SUCCESS_STATUS_CODE);
                Assert.assertTrue(
                                response.has("message"),
                                Constants.MESSAGE_EXIST);

                Assert.assertEquals(
                                response.get("message").asText(),
                                Constants.ACCOUNT_BALANCE_SUCCESS_MESSAGE);
                Assert.assertTrue(response.has("data"));
        }

        @Test(description = "Should contain balance details")
        public void testBalanceDetails() throws Exception {

                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);
                requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);
                requestBody.put("accountType", "SAVINGS");

                JsonNode response = apiClient.post(
                                Endpoints.ACCOUNT_BALANCE,
                                requestBody);

                JsonNode data = response.get("data");

                Assert.assertTrue(data.has("accountId"));
                Assert.assertTrue(data.has("balance"));
                Assert.assertTrue(data.has("status"));
                Assert.assertEquals(data.get("status").asText(), "ACTIVE");
        }

        @Test(description = "Should have masked account number")
        public void testMaskedAccountNumber() throws Exception {

                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);
                requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);
                requestBody.put("accountType", "SAVINGS");

                JsonNode response = apiClient.post(
                                Endpoints.ACCOUNT_BALANCE,
                                requestBody);

                JsonNode data = response.get("data");

                Assert.assertTrue(data.has("accountNumberMasked"));

                String masked = data.get("accountNumberMasked").asText();

                Assert.assertTrue(
                                masked.matches(".*\\d{4}$"),
                                "Masked account number should end with 4 digits. Actual: " + masked);
        }

        @Test(description = "Balance should be numeric and non-negative")
        public void testBalanceNumeric() throws Exception {

                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);
                requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);
                requestBody.put("accountType", "SAVINGS");

                JsonNode response = apiClient.post(
                                Endpoints.ACCOUNT_BALANCE,
                                requestBody);

                JsonNode data = response.get("data");

                Assert.assertTrue(
                                data.get("balance").isNumber(),
                                "Balance should be numeric");

                Assert.assertTrue(
                                data.get("balance").asDouble() >= 0,
                                "Balance should not be negative");
        }

        @Test(description = "Should validate account balance data structure")
        public void testAccountBalanceDataStructure() throws Exception {

                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);
                requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);
                requestBody.put("accountType", Constants.SAVINGS_ACCOUNT_TYPE);

                JsonNode response = apiClient.post(
                                Endpoints.ACCOUNT_BALANCE,
                                requestBody);

                JsonNode data = response.get("data");

                Assert.assertTrue(data.has("accountId"), "accountId is missing");
                Assert.assertTrue(data.has("accountNumberMasked"), "accountNumberMasked is missing");
                Assert.assertTrue(data.has("accountType"), "accountType is missing");
                Assert.assertTrue(data.has("balance"), "balance is missing");
                Assert.assertTrue(data.has("status"), "status is missing");

                Assert.assertFalse(
                                data.get("accountId").asText().isBlank(),
                                "accountId should not be empty");

                Assert.assertFalse(
                                data.get("accountNumberMasked").asText().isBlank(),
                                "accountNumberMasked should not be empty");

                Assert.assertTrue(
                                data.get("balance").isNumber(),
                                "balance should be numeric");

                Assert.assertTrue(
                                data.get("balance").asDouble() >= 0,
                                "balance should not be negative");
        }

        @Test(description = "Should verify account balance data values")
        public void testAccountBalanceDataValues() throws Exception {

                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("accountId", Constants.EXPECTED_ACCOUNT_ID);
                requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);
                requestBody.put("accountType", Constants.SAVINGS_ACCOUNT_TYPE);

                JsonNode response = apiClient.post(
                                Endpoints.ACCOUNT_BALANCE,
                                requestBody);

                JsonNode data = response.get("data");

                Assert.assertEquals(
                                data.get("accountId").asText(),
                                Constants.EXPECTED_ACCOUNT_ID,
                                "Account ID mismatch");

                Assert.assertEquals(
                                data.get("accountNumberMasked").asText(),
                                Constants.EXPECTED_MASKED_ACCOUNT,
                                "Masked account number mismatch");

                Assert.assertEquals(
                                data.get("accountType").asText(),
                                Constants.SAVINGS_ACCOUNT_TYPE,
                                "Account type mismatch");

                Assert.assertEquals(
                                data.get("status").asText(),
                                Constants.ACTIVE_STATUS,
                                "Account status mismatch");

                Assert.assertEquals(
                                data.get("balance").asDouble(),
                                Constants.EXPECTED_ACCOUNT_BALANCE,
                                0.01,
                                "Account balance mismatch");
        }
}