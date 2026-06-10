package com.voicebanking.tests.api;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.voicebanking.DataText.Constants;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.pages.BaseApiPage;

public class API2_GetCustomerInfoTest extends BaseApiPage {

    private JsonNode getCustomerInfoResponse() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);

        return apiClient.post(
                Endpoints.CUSTOMER_INFO,
                requestBody);
    }

    @Test(groups = {"smoke", "regression"}, description = "Should validate customer info API response structure")
    public void testCustomerInfoResponse() throws Exception {

        JsonNode response = getCustomerInfoResponse();

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
                Constants.CUSTOMER_INFO_SUCCESS_MESSAGE);

        Assert.assertTrue(
                response.has("data"),
                "Data node is missing");
    }

    @Test(groups = {"smoke", "regression"}, description = "Should validate customer information and business data")
    public void testCustomerDataValidation() throws Exception {

        JsonNode data = getCustomerInfoResponse().get("data");

        // Mandatory fields
        String[] requiredFields = {
            "customerId",
            "name",
            "email",
            "mobileNumber",
            "dateOfBirth",
            "status",
            "kycStatus",
            "createdDate",
            "updatedDate"
        };

        for (String field : requiredFields) {
            Assert.assertTrue(
                    data.has(field),
                    field + " field is missing");
        }

        // Customer Id
        Assert.assertEquals(
                data.get("customerId").asText(),
                Constants.EXPECTED_CUSTOMER_ID);

        // Customer Name
        Assert.assertEquals(
                data.get("name").asText(),
                Constants.EXPECTED_CUSTOMER_NAME);

        // Email
        String email = data.get("email").asText();

        Assert.assertEquals(
                email,
                Constants.EXPECTED_CUSTOMER_EMAIL);

        Assert.assertTrue(
                email.matches(Constants.EMAIL_REGEX),
                "Invalid email format");

        // Mobile Number
        String mobileNumber = data.get("mobileNumber").asText();

        Assert.assertEquals(
                mobileNumber,
                Constants.EXPECTED_CUSTOMER_MOBILE);

        Assert.assertTrue(
                mobileNumber.matches(Constants.MOBILE_REGEX),
                "Invalid mobile number");

        // DOB
        Assert.assertEquals(
                data.get("dateOfBirth").asText(),
                Constants.EXPECTED_CUSTOMER_DOB);

        // Status
        Assert.assertEquals(
                data.get("status").asText(),
                Constants.ACTIVE_STATUS);

        // KYC
        Assert.assertEquals(
                data.get("kycStatus").asText(),
                Constants.VERIFIED_KYC_STATUS);

        // Audit Fields
        Assert.assertFalse(
                data.get("createdDate").asText().isBlank(),
                "createdDate should not be empty");

        Assert.assertFalse(
                data.get("updatedDate").asText().isBlank(),
                "updatedDate should not be empty");
    }
}
