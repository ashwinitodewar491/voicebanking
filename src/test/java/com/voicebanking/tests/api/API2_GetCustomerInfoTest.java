package com.voicebanking.tests.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.DataText.Constants;
import com.voicebanking.pages.BaseApiPage;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class API2_GetCustomerInfoTest extends BaseApiPage {

        @Test(description = "Should retrieve customer information")
        public void testGetCustomerInfo() throws Exception {

                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);

                JsonNode response = apiClient.post(
                                Endpoints.CUSTOMER_INFO,
                                requestBody);

                Assert.assertEquals(response.get("status").asText(), Constants.SUCCESS_STATUS);
                Assert.assertEquals(response.get("statusCode").asInt(), Constants.SUCCESS_STATUS_CODE);
                Assert.assertTrue(
                                response.has("message"),
                                Constants.MESSAGE_EXIST);

                Assert.assertEquals(
                                response.get("message").asText(),
                                Constants.CUSTOMER_INFO_SUCCESS_MESSAGE);
                Assert.assertTrue(response.has("data"));
        }

        @Test(description = "Should contain required customer fields")
        public void testCustomerInfoFields() throws Exception {

                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);

                JsonNode response = apiClient.post(
                                Endpoints.CUSTOMER_INFO,
                                requestBody);

                JsonNode data = response.get("data");

                Assert.assertEquals(
                                data.get("customerId").asText(),
                                Constants.EXISTING_CUSTOMER_ID);

                Assert.assertTrue(data.has("name"));
                Assert.assertTrue(data.has("email"));
                Assert.assertTrue(data.has("mobileNumber"));
                Assert.assertTrue(data.has("status"));
        }

        @Test(description = "Should have KYC status in response")
        public void testKYCStatus() throws Exception {

                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);

                JsonNode response = apiClient.post(
                                Endpoints.CUSTOMER_INFO,
                                requestBody);

                JsonNode data = response.get("data");

                Assert.assertTrue(data.has("kycStatus"));

                String kycStatus = data.get("kycStatus").asText();

                Assert.assertTrue(
                                kycStatus.isEmpty()
                                                || kycStatus.equals("VERIFIED")
                                                || kycStatus.equals("PENDING")
                                                || kycStatus.equals("FAILED"),
                                "Unexpected KYC Status: " + kycStatus);
        }

        @Test(description = "Should validate customer data details")
        public void testCustomerDataValidation() throws Exception {

                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);

                JsonNode response = apiClient.post(
                                Endpoints.CUSTOMER_INFO,
                                requestBody);

                JsonNode data = response.get("data");

                // Mandatory fields
                Assert.assertTrue(data.has("customerId"), "customerId is missing");
                Assert.assertTrue(data.has("name"), "name is missing");
                Assert.assertTrue(data.has("email"), "email is missing");
                Assert.assertTrue(data.has("mobileNumber"), "mobileNumber is missing");
                Assert.assertTrue(data.has("dateOfBirth"), "dateOfBirth is missing");
                Assert.assertTrue(data.has("status"), "status is missing");
                Assert.assertTrue(data.has("kycStatus"), "kycStatus is missing");
                Assert.assertTrue(data.has("createdDate"), "createdDate is missing");
                Assert.assertTrue(data.has("updatedDate"), "updatedDate is missing");

                // Customer Id
                Assert.assertEquals(
                                data.get("customerId").asText(),
                                Constants.EXISTING_CUSTOMER_ID,
                                "Incorrect customerId");

                // Name
                Assert.assertFalse(
                                data.get("name").asText().isBlank(),
                                "Customer name should not be empty");

                // Email
                String email = data.get("email").asText();
                Assert.assertTrue(
                                email.matches(Constants.EMAIL_REGEX),
                                "Invalid email format: " + email);

                // Mobile Number
                String mobileNumber = data.get("mobileNumber").asText();
                Assert.assertTrue(
                                mobileNumber.matches(Constants.MOBILE_REGEX),
                                "Invalid mobile number: " + mobileNumber);

                // Status
                Assert.assertEquals(
                                data.get("status").asText(),
                                Constants.ACTIVE_STATUS,
                                "Customer status is incorrect");

                // KYC Status
                Assert.assertEquals(
                                data.get("kycStatus").asText(),
                                Constants.VERIFIED_KYC_STATUS,
                                "KYC status is incorrect");

                // Date fields should not be empty
                Assert.assertFalse(
                                data.get("createdDate").asText().isBlank(),
                                "createdDate should not be empty");

                Assert.assertFalse(
                                data.get("updatedDate").asText().isBlank(),
                                "updatedDate should not be empty");
        }

        @Test(description = "Should verify customer details against expected data")
        public void testCustomerDataValues() throws Exception {

                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("customerId", Constants.EXPECTED_CUSTOMER_ID);

                JsonNode response = apiClient.post(
                                Endpoints.CUSTOMER_INFO,
                                requestBody);

                JsonNode data = response.get("data");

                Assert.assertEquals(
                                data.get("customerId").asText(),
                                Constants.EXPECTED_CUSTOMER_ID,
                                "Customer ID mismatch");

                Assert.assertEquals(
                                data.get("name").asText(),
                                Constants.EXPECTED_CUSTOMER_NAME,
                                "Customer name mismatch");

                Assert.assertEquals(
                                data.get("email").asText(),
                                Constants.EXPECTED_CUSTOMER_EMAIL,
                                "Customer email mismatch");

                Assert.assertEquals(
                                data.get("mobileNumber").asText(),
                                Constants.EXPECTED_CUSTOMER_MOBILE,
                                "Customer mobile number mismatch");

                Assert.assertEquals(
                                data.get("dateOfBirth").asText(),
                                Constants.EXPECTED_CUSTOMER_DOB,
                                "Customer DOB mismatch");

                Assert.assertEquals(
                                data.get("kycStatus").asText(),
                                Constants.VERIFIED_KYC_STATUS,
                                "KYC status mismatch");

                Assert.assertEquals(
                                data.get("status").asText(),
                                Constants.ACTIVE_STATUS,
                                "Customer status mismatch");
        }
}