package com.voicebanking.tests.api;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.voicebanking.DataText.Constants;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.pages.BaseApiPage;

public class API4_GetBeneficiariesListTest extends BaseApiPage {

    private JsonNode getBeneficiariesResponse() throws Exception {

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);
        requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);

        return apiClient.post(
                Endpoints.BENEFICIARIES_LIST,
                requestBody);
    }

    @Test(groups = {"smoke", "regression", "api"}, description = "Should validate beneficiaries API response structure")
    public void testBeneficiariesResponse() throws Exception {

        JsonNode response = getBeneficiariesResponse();

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
                Constants.BENEFICIARIES_LIST_SUCCESS_MESSAGE);

        Assert.assertTrue(
                response.has("data"));

        Assert.assertTrue(
                response.get("data").has("beneficiaries"));

        Assert.assertTrue(
                response.get("data").get("beneficiaries").isArray());
    }

    @Test(groups = {"regression", "api"}, description = "Should validate beneficiary schema and field values")
    public void testBeneficiarySchemaValidation() throws Exception {

        JsonNode beneficiaries
                = getBeneficiariesResponse()
                        .get("data")
                        .get("beneficiaries");

        Assert.assertTrue(
                beneficiaries.size() > 0,
                Constants.BENEFICIARY_LIST_NOT_EMPTY);

        for (JsonNode beneficiary : beneficiaries) {

            // Mandatory fields
            Assert.assertTrue(beneficiary.has("beneficiaryId"));
            Assert.assertTrue(beneficiary.has("name"));
            Assert.assertTrue(beneficiary.has("bankName"));
            Assert.assertTrue(beneficiary.has("ifsc"));
            Assert.assertTrue(beneficiary.has("status"));
            Assert.assertTrue(beneficiary.has("transferType"));

            // Value validation
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

            Assert.assertTrue(
                    Constants.VALID_BENEFICIARY_STATUSES.contains(
                            beneficiary.get("status").asText()),
                    "Invalid beneficiary status");

            Assert.assertTrue(
                    Constants.VALID_TRANSFER_TYPES.contains(
                            beneficiary.get("transferType").asText()),
                    "Invalid transfer type");
        }
    }

    @Test(groups = {"regression", "api"}, description = "Should validate expected beneficiary data")
    public void testBeneficiaryDataValidation() throws Exception {

        JsonNode beneficiaries
                = getBeneficiariesResponse()
                        .get("data")
                        .get("beneficiaries");

        Assert.assertEquals(
                beneficiaries.size(),
                Constants.EXPECTED_BENEFICIARY_COUNT);

        boolean beneficiary1Found = false;

        for (JsonNode beneficiary : beneficiaries) {

            String name = beneficiary.get("name").asText();

            if (Constants.BENEFICIARY_1_NAME.equals(name)) {

                Assert.assertEquals(
                        beneficiary.get("bankName").asText(),
                        Constants.BENEFICIARY_1_BANK);

                Assert.assertEquals(
                        beneficiary.get("ifsc").asText(),
                        Constants.BENEFICIARY_1_IFSC);

                Assert.assertEquals(
                        beneficiary.get("transferType").asText(),
                        Constants.BENEFICIARY_1_TRANSFER_TYPE);

                beneficiary1Found = true;
            }
        }

        Assert.assertTrue(
                beneficiary1Found,
                Constants.BENEFICIARY_1_NAME + " not found");
    }

    // --- Negative Tests ---

    @Test(groups = {"negative", "regression", "api"},
            description = "Beneficiaries API should reject non-existent customer ID")
    public void testBeneficiariesWithInvalidCustomerId() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("customerId", Constants.INVALID_CUSTOMER_ID);
        requestBody.put("accountId", Constants.EXISTING_ACCOUNT_ID);

        JsonNode response = apiClient.post(Endpoints.BENEFICIARIES_LIST, requestBody);

        Assert.assertNotEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API should reject non-existent customerId");

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
            description = "Beneficiaries API should reject non-existent account ID")
    public void testBeneficiariesWithInvalidAccountId() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);
        requestBody.put("accountId", Constants.INVALID_ACCOUNT_ID);

        JsonNode response = apiClient.post(Endpoints.BENEFICIARIES_LIST, requestBody);

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
            description = "Beneficiaries API should reject request with missing fields")
    public void testBeneficiariesWithMissingFields() throws Exception {
        Map<String, String> requestBody = new HashMap<>();

        JsonNode response = apiClient.post(Endpoints.BENEFICIARIES_LIST, requestBody);

        Assert.assertNotEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API should reject request with missing fields");

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.ERROR_STATUS,
                "Response status should be error");

        String errorMessage = response.get("error").get("message").asText();
        Assert.assertTrue(
                errorMessage.contains(Constants.ERR_CUSTOMER_ID_REQUIRED),
                "Error message should indicate customerId is required");
        Assert.assertTrue(
                errorMessage.contains(Constants.ERR_ACCOUNT_ID_REQUIRED),
                "Error message should indicate accountId is required");
    }
}
