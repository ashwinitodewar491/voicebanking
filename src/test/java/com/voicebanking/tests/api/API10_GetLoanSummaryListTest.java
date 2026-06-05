package com.voicebanking.tests.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.voicebanking.DataText.Constants;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.pages.BaseApiPage;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class API10_GetLoanSummaryListTest extends BaseApiPage {

        private JsonNode getResponse() throws Exception {

                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("customerId", Constants.EXISTING_CUSTOMER_ID);

                return apiClient.post(
                                Endpoints.LOAN_SUMMARY,
                                requestBody);
        }

        private JsonNode getLoanDetails() throws Exception {
                return getResponse()
                                .get("data")
                                .get("loanDetails");
        }

        @Test(description = "Should retrieve loan summary list")
        public void testGetLoanSummary() throws Exception {

                JsonNode response = getResponse();

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

                Assert.assertTrue(
                                response.has("data"),
                                "Response should contain data object");
        }

        @Test(description = "Should contain loan details list")
        public void testLoanSummaryContainsLoanList() throws Exception {

                JsonNode response = getResponse();

                Assert.assertTrue(
                                response.get("data").has("loanDetails"),
                                "loanDetails array is missing");

                Assert.assertTrue(
                                response.get("data").get("loanDetails").isArray(),
                                "loanDetails should be an array");
        }

        @Test(description = "Each loan should contain mandatory fields")
        public void testLoanDetailFields() throws Exception {

                JsonNode loans = getLoanDetails();

                Assert.assertTrue(
                                loans.size() > 0,
                                "Loan list should not be empty");

                for (JsonNode loan : loans) {

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
                }
        }

        @Test(description = "Loan amounts should be numeric and positive")
        public void testLoanAmounts() throws Exception {

                JsonNode loans = getLoanDetails();

                for (JsonNode loan : loans) {

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
                }
        }

        @Test(description = "Should return one or more loans")
        public void testMultipleLoans() throws Exception {

                JsonNode loans = getLoanDetails();

                Assert.assertTrue(
                                loans.isArray(),
                                "loanDetails should be an array");

                Assert.assertTrue(
                                loans.size() > 0,
                                "At least one loan should be returned");
        }

        @Test(description = "Should validate loan summary business data")
        public void testLoanSummaryDataValues() throws Exception {

                JsonNode loans = getLoanDetails();

                for (JsonNode loan : loans) {

                        Assert.assertEquals(
                                        loan.get("customerNumber").asText(),
                                        Constants.EXISTING_CUSTOMER_ID);

                        Assert.assertEquals(
                                        loan.get("customerName").asText(),
                                        Constants.CUSTOMER_NAME);

                        Assert.assertEquals(
                                        loan.get("accountStatus").asText(),
                                        Constants.ACTIVE_STATUS);

                        Assert.assertEquals(
                                        loan.get("accountCurrency").asText(),
                                        Constants.INR_CURRENCY);

                        Assert.assertEquals(
                                        loan.get("termUnits").asText(),
                                        Constants.MONTH_TERM_UNIT);
                }
        }

        @Test(description = "Communication address should contain mandatory fields")
        public void testCommunicationAddress() throws Exception {

                JsonNode loans = getLoanDetails();

                for (JsonNode loan : loans) {

                        JsonNode addresses = loan.get("communicationAddress");

                        Assert.assertTrue(
                                        addresses.isArray(),
                                        "communicationAddress should be an array");

                        Assert.assertTrue(
                                        addresses.size() > 0,
                                        "At least one communication address should exist");

                        for (JsonNode address : addresses) {

                                Assert.assertTrue(address.has("AddressLine1"));
                                Assert.assertTrue(address.has("City"));
                                Assert.assertTrue(address.has("State"));
                                Assert.assertTrue(address.has("Country"));
                                Assert.assertTrue(address.has("Zip"));
                        }
                }
        }
}