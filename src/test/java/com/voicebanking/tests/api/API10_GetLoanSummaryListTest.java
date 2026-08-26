package com.voicebanking.tests.api;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.voicebanking.DataText.Constants;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.pages.BaseApiPage;

public class API10_GetLoanSummaryListTest extends BaseApiPage {

    private JsonNode getLoanSummaryResponse() throws Exception {

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("customerId", Constants.LOAN_SUMMARY_CUSTOMER_ID);

        return apiClient.post(
                Endpoints.LOAN_SUMMARY,
                requestBody);
    }

    private JsonNode getLoanDetails() throws Exception {
        return getLoanSummaryResponse()
                .get("data")
                .get("loanDetails");
    }

    @Test(groups = {"smoke", "regression", "api"}, description = "Should validate loan summary response structure")
    public void testLoanSummaryResponse() throws Exception {

        JsonNode response = getLoanSummaryResponse();

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
                Constants.LOAN_SUMMARY_SUCCESS_MESSAGE);

        Assert.assertTrue(response.has("data"));

        Assert.assertTrue(
                response.get("data").has("loanDetails"),
                "loanDetails array is missing");

        Assert.assertTrue(
                response.get("data").get("loanDetails").isArray(),
                "loanDetails should be an array");
    }

    @Test(groups = {"regression", "api"}, description = "Should validate all loan details")
    public void testLoanDetailsValidation() throws Exception {

        JsonNode loans = getLoanDetails();

        Assert.assertTrue(
                loans.size() > 0,
                "Loan list should not be empty");

        for (JsonNode loan : loans) {

            // Mandatory fields
            Assert.assertTrue(loan.has("accountId"));
            Assert.assertTrue(loan.has("accountStatus"));
            Assert.assertTrue(loan.has("customerName"));
            Assert.assertTrue(loan.has("customerNumber"));
            Assert.assertTrue(loan.has("loanType"));
            Assert.assertTrue(loan.has("loanAmount"));
            Assert.assertTrue(loan.has("interestRate"));
            Assert.assertTrue(loan.has("loanTenure"));
            Assert.assertTrue(loan.has("outstandingAmount"));
            Assert.assertTrue(loan.has("schemeName"));
            Assert.assertTrue(loan.has("communicationAddress"));

            // Numeric validations
            Assert.assertTrue(
                    loan.get("loanAmount").isNumber(),
                    "loanAmount should be numeric");

            Assert.assertTrue(
                    loan.get("loanAmount").asDouble() > 0,
                    "loanAmount should be greater than zero");

            Assert.assertTrue(
                    loan.get("interestRate").isNumber(),
                    "interestRate should be numeric");

            Assert.assertTrue(
                    loan.get("interestRate").asDouble() > 0,
                    "interestRate should be greater than zero");

            // Business validations
            Assert.assertEquals(
                    loan.get("customerNumber").asText(),
                    Constants.LOAN_SUMMARY_CUSTOMER_ID);

            Assert.assertEquals(
                    loan.get("customerName").asText(),
                    Constants.CUSTOMER_NAME);

            Assert.assertTrue(
                    Constants.VALID_LOAN_STATUSES.contains(loan.get("accountStatus").asText()),
                    "Invalid loan account status");

            Assert.assertEquals(
                    loan.get("accountCurrency").asText(),
                    Constants.INR_CURRENCY);

            Assert.assertEquals(
                    loan.get("termUnits").asText(),
                    Constants.MONTH_TERM_UNIT);
        }
    }

    @Test(groups = {"regression", "api"}, description = "Should validate communication addresses")
    public void testCommunicationAddressValidation() throws Exception {

        JsonNode loans = getLoanDetails();

        for (JsonNode loan : loans) {

            JsonNode addresses = loan.get("communicationAddress");

            Assert.assertTrue(
                    addresses.isArray(),
                    "communicationAddress should be an array");

            Assert.assertTrue(
                    addresses.size() > 0,
                    "Communication address should not be empty");

            for (JsonNode address : addresses) {

                Assert.assertTrue(address.has("AddressLine1"));
                Assert.assertTrue(address.has("City"));
                Assert.assertTrue(address.has("State"));
                Assert.assertTrue(address.has("Country"));
                Assert.assertTrue(address.has("Zip"));

                Assert.assertFalse(
                        address.get("AddressLine1").asText().isBlank());

                Assert.assertFalse(
                        address.get("City").asText().isBlank());

                Assert.assertFalse(
                        address.get("State").asText().isBlank());

                Assert.assertFalse(
                        address.get("Country").asText().isBlank());

                Assert.assertFalse(
                        address.get("Zip").asText().isBlank());
            }
        }
    }

    @Test(groups = {"regression", "api"}, description = "Should validate expected loan products")
    public void testLoanProductTypes() throws Exception {

        JsonNode loans = getLoanDetails();

        boolean homeLoanFound = false;
        boolean educationLoanFound = false;

        for (JsonNode loan : loans) {

            String loanType = loan.get("loanType").asText();

            if (Constants.HOME_LOAN_TYPE.equals(loanType)) {
                homeLoanFound = true;
            }

            if (Constants.EDUCATION_LOAN_TYPE.equals(loanType)) {
                educationLoanFound = true;
            }
        }

        Assert.assertTrue(
                homeLoanFound,
                "HOME_LOAN not found");

        Assert.assertTrue(
                educationLoanFound,
                "EDUCATION_LOAN not found");
    }

    // --- Negative Tests ---

    @Test(groups = {"negative", "regression", "api"},
            description = "Loan summary API returns success with no data for non-existent customer ID")
    public void testLoanSummaryWithInvalidCustomerId() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("customerId", Constants.INVALID_CUSTOMER_ID);

        JsonNode response = apiClient.post(Endpoints.LOAN_SUMMARY, requestBody);

        Assert.assertEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API returns success for non-existent customerId");

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.SUCCESS_STATUS,
                "Response status should be success");

        Assert.assertFalse(
                response.has("data"),
                "No data should be returned for non-existent customerId");
    }

    @Test(groups = {"negative", "regression", "api"},
            description = "Loan summary API should reject empty customer ID")
    public void testLoanSummaryWithEmptyCustomerId() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("customerId", "");

        JsonNode response = apiClient.post(Endpoints.LOAN_SUMMARY, requestBody);

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
            description = "Loan summary API should reject request with missing customer ID field")
    public void testLoanSummaryWithMissingCustomerId() throws Exception {
        Map<String, String> requestBody = new HashMap<>();

        JsonNode response = apiClient.post(Endpoints.LOAN_SUMMARY, requestBody);

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
