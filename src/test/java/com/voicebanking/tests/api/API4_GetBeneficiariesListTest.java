package com.voicebanking.tests.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.DataText.Constants;
import com.voicebanking.pages.BaseApiPage;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class API4_GetBeneficiariesListTest extends BaseApiPage {

    @Test(description = "Should retrieve beneficiaries list")
    public void testGetBeneficiariesList() throws Exception {

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);
        requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);

        JsonNode response = apiClient.post(
                Endpoints.BENEFICIARIES_LIST,
                requestBody);

        Assert.assertEquals(response.get("status").asText(), Constants.SUCCESS_STATUS);
        Assert.assertEquals(response.get("statusCode").asInt(), Constants.SUCCESS_STATUS_CODE);
        Assert.assertTrue(
                response.has("message"),
                Constants.MESSAGE_EXIST);

        Assert.assertEquals(
                response.get("message").asText(),
                Constants.BENEFICIARIES_LIST_SUCCESS_MESSAGE);
        Assert.assertTrue(response.has("data"));
    }

    @Test(description = "Should contain beneficiary list")
    public void testBeneficiariesList() throws Exception {

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);
        requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);

        JsonNode response = apiClient.post(
                Endpoints.BENEFICIARIES_LIST,
                requestBody);

        JsonNode data = response.get("data");

        Assert.assertTrue(data.has("beneficiaries"));
        Assert.assertTrue(data.get("beneficiaries").isArray());
    }

    @Test(description = "Beneficiary should have required fields")
    public void testBeneficiaryFields() throws Exception {

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);
        requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);

        JsonNode response = apiClient.post(
                Endpoints.BENEFICIARIES_LIST,
                requestBody);

        JsonNode beneficiaries = response.get("data").get("beneficiaries");

        if (beneficiaries.size() > 0) {
            JsonNode beneficiary = beneficiaries.get(0);

            Assert.assertTrue(beneficiary.has("beneficiaryId"));
            Assert.assertTrue(beneficiary.has("name"));
            Assert.assertTrue(beneficiary.has("status"));
        }
    }

    
    @Test(description = "All beneficiaries should contain mandatory fields")
    public void testAllBeneficiariesContainRequiredFields() throws Exception {

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);
        requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);

        JsonNode response = apiClient.post(
                Endpoints.BENEFICIARIES_LIST,
                requestBody);

        JsonNode beneficiaries = response.get("data").get("beneficiaries");

        Assert.assertTrue(
                beneficiaries.size() > 0,
                Constants.BENEFICIARY_LIST_NOT_EMPTY);

        for (JsonNode beneficiary : beneficiaries) {

            Assert.assertTrue(
                    beneficiary.has("beneficiaryId"),
                    "beneficiaryId is missing");

            Assert.assertTrue(
                    beneficiary.has("name"),
                    "name is missing");

            Assert.assertTrue(
                    beneficiary.has("bankName"),
                    "bankName is missing");

            Assert.assertTrue(
                    beneficiary.has("ifsc"),
                    "ifsc is missing");

            Assert.assertTrue(
                    beneficiary.has("status"),
                    "status is missing");

            Assert.assertTrue(
                    beneficiary.has("transferType"),
                    "transferType is missing");
        }
    }

    @Test(description = "Beneficiary data should contain valid values")
    public void testBeneficiaryDataValidation() throws Exception {

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);
        requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);

        JsonNode response = apiClient.post(
                Endpoints.BENEFICIARIES_LIST,
                requestBody);

        JsonNode beneficiaries = response.get("data").get("beneficiaries");

        Assert.assertTrue(
                beneficiaries.size() > 0,
                Constants.BENEFICIARY_LIST_NOT_EMPTY);

        for (JsonNode beneficiary : beneficiaries) {

            Assert.assertFalse(
                    beneficiary.get("beneficiaryId").asText().isBlank(),
                    Constants.BENEFICIARY_ID_EMPTY);

            Assert.assertFalse(
                    beneficiary.get("name").asText().isBlank(),
                    Constants.BENEFICIARY_NAME_EMPTY);

            Assert.assertFalse(
                    beneficiary.get("bankName").asText().isBlank(),
                    Constants.BANK_NAME_EMPTY);

            Assert.assertFalse(
                    beneficiary.get("ifsc").asText().isBlank(),
                    Constants.IFSC_EMPTY);

            String status = beneficiary.get("status").asText();

            Assert.assertTrue(
                    Constants.VALID_BENEFICIARY_STATUSES.contains(status),
                    "Invalid beneficiary status: " + status);

            String transferType = beneficiary.get("transferType").asText();

            Assert.assertTrue(
                    Constants.VALID_TRANSFER_TYPES.contains(transferType),
                    "Invalid transfer type: " + transferType);
        }
    }

    @Test(description = "Should validate beneficiary data")
    public void testBeneficiaryData() throws Exception {

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);
        requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);

        JsonNode response = apiClient.post(
                Endpoints.BENEFICIARIES_LIST,
                requestBody);

        JsonNode beneficiaries = response.get("data").get("beneficiaries");

        Assert.assertEquals(
                beneficiaries.size(),
                Constants.EXPECTED_BENEFICIARY_COUNT,
                "Unexpected beneficiary count");

        boolean rohitFound = false;
        boolean neerajFound = false;

        for (JsonNode beneficiary : beneficiaries) {

            String name = beneficiary.get("name").asText();

            if (name.equals(Constants.BENEFICIARY_1_NAME)) {

                Assert.assertEquals(
                        beneficiary.get("bankName").asText(),
                        Constants.BENEFICIARY_1_BANK);

                Assert.assertEquals(
                        beneficiary.get("ifsc").asText(),
                        Constants.BENEFICIARY_1_IFSC);

                Assert.assertEquals(
                        beneficiary.get("transferType").asText(),
                        Constants.BENEFICIARY_1_TRANSFER_TYPE);

                rohitFound = true;
            }

            if (name.equals(Constants.BENEFICIARY_2_NAME)) {

                Assert.assertEquals(
                        beneficiary.get("bankName").asText(),
                        Constants.BENEFICIARY_2_BANK);

                Assert.assertEquals(
                        beneficiary.get("ifsc").asText(),
                        Constants.BENEFICIARY_2_IFSC);

                Assert.assertEquals(
                        beneficiary.get("transferType").asText(),
                        Constants.BENEFICIARY_2_TRANSFER_TYPE);

                neerajFound = true;
            }
        }

        Assert.assertTrue(
                rohitFound,
                "Beneficiary " + Constants.BENEFICIARY_1_NAME + " not found");

        Assert.assertTrue(
                neerajFound,
                "Beneficiary " + Constants.BENEFICIARY_2_NAME + " not found");
    }
}