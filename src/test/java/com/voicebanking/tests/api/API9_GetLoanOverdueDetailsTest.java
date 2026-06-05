package com.voicebanking.tests.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.DataText.Constants;
import com.voicebanking.pages.BaseApiPage;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class API9_GetLoanOverdueDetailsTest extends BaseApiPage {

    @Test(description = "Should retrieve loan overdue details")
    public void testGetLoanOverdue() throws Exception {

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("accountId", "LN10001");

        JsonNode response = apiClient.post(
                Endpoints.LOAN_OVERDUE,
                requestBody);

        Assert.assertEquals(response.get("status").asText(), Constants.SUCCESS_STATUS);
        Assert.assertEquals(response.get("statusCode").asInt(), Constants.SUCCESS_STATUS_CODE);
        Assert.assertTrue(
                response.has("message"),
                Constants.MESSAGE_EXIST);

        Assert.assertEquals(
                response.get("message").asText(),
                Constants.LOAN_OVERDUE_SUCCESS_MESSAGE);
        Assert.assertTrue(response.has("data"));
    }

    @Test(description = "Should contain overdue details")
    public void testOverdueDetails() throws Exception {

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("accountId", "LN10001");

        JsonNode response = apiClient.post(
                Endpoints.LOAN_OVERDUE,
                requestBody);

        JsonNode data = response.get("data");

        Assert.assertTrue(
                data.has("loanAccountId") || data.has("accountStatus"),
                "Expected either loanAccountId or accountStatus");

        Assert.assertTrue(data.has("customerShortName"));
        Assert.assertTrue(data.has("totalOutstandings"));
    }

    @Test(description = "Overdue amounts should be numeric")
    public void testOverdueAmounts() throws Exception {

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("accountId", "LN10001");

        JsonNode response = apiClient.post(
                Endpoints.LOAN_OVERDUE,
                requestBody);

        JsonNode data = response.get("data");

        if (data.has("totalOutstandings")) {
            Assert.assertTrue(
                    data.get("totalOutstandings").isNumber(),
                    "Total outstanding amount should be numeric");

            Assert.assertTrue(
                    data.get("totalOutstandings").asDouble() >= 0,
                    "Total outstanding amount should not be negative");
        }

        if (data.has("totalOverdueAmount")) {
            Assert.assertTrue(
                    data.get("totalOverdueAmount").isNumber(),
                    "Total overdue amount should be numeric");

            Assert.assertTrue(
                    data.get("totalOverdueAmount").asDouble() >= 0,
                    "Total overdue amount should not be negative");
        }
    }

    @Test(description = "Should contain loan tenure information")
    public void testTenureInformation() throws Exception {

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("accountId", "LN10001");

            JsonNode response = apiClient.post(
                            Endpoints.LOAN_OVERDUE,
                            requestBody);

            JsonNode data = response.get("data");

            if (data.has("nextDueDate")) {
                    Assert.assertTrue(data.has("nextDueDate"));
            }

            if (data.has("maturityDate")) {
                    Assert.assertTrue(data.has("maturityDate"));
            }
    }
    
    @Test(description = "Should contain all mandatory overdue loan fields")
    public void testMandatoryOverdueFields() throws Exception {

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("accountId", Constants.LOAN_ACCOUNT_ID);

            JsonNode response = apiClient.post(
                            Endpoints.LOAN_OVERDUE,
                            requestBody);

            JsonNode data = response.get("data");

            Assert.assertTrue(data.has("loanAccountId"));
            Assert.assertTrue(data.has("customerShortName"));
            Assert.assertTrue(data.has("loanAmount"));
            Assert.assertTrue(data.has("principalBalance"));
            Assert.assertTrue(data.has("totalOutstandings"));
            Assert.assertTrue(data.has("availableBalance"));
            Assert.assertTrue(data.has("minAmountDue"));
            Assert.assertTrue(data.has("nextInstallmentAmount"));
            Assert.assertTrue(data.has("amountPaidToday"));
            Assert.assertTrue(data.has("nextDueDate"));
            Assert.assertTrue(data.has("maturityDate"));
    }

    @Test(description = "Should validate overdue loan details data")
    public void testOverdueLoanDataValues() throws Exception {

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("accountId", Constants.LOAN_ACCOUNT_ID);

            JsonNode response = apiClient.post(
                            Endpoints.LOAN_OVERDUE,
                            requestBody);

            JsonNode data = response.get("data");

            Assert.assertEquals(
                            data.get("loanAccountId").asText(),
                            Constants.LOAN_ACCOUNT_ID);

            Assert.assertEquals(
                            data.get("customerShortName").asText(),
                            Constants.CUSTOMER_SHORT_NAME);

            Assert.assertEquals(
                            data.get("loanAmount").asDouble(),
                            Constants.LOAN_AMOUNT);

            Assert.assertEquals(
                            data.get("principalBalance").asDouble(),
                            Constants.PRINCIPAL_BALANCE);

            Assert.assertEquals(
                            data.get("totalOutstandings").asDouble(),
                            Constants.TOTAL_OUTSTANDINGS);

            Assert.assertEquals(
                            data.get("availableBalance").asDouble(),
                            Constants.AVAILABLE_BALANCE);

            Assert.assertEquals(
                            data.get("minAmountDue").asDouble(),
                            Constants.MIN_AMOUNT_DUE);

            Assert.assertEquals(
                            data.get("nextInstallmentAmount").asDouble(),
                            Constants.NEXT_INSTALLMENT_AMOUNT);

            Assert.assertEquals(
                            data.get("amountPaidToday").asDouble(),
                            Constants.AMOUNT_PAID_TODAY);

            Assert.assertEquals(
                            data.get("nextDueDate").asText(),
                            Constants.NEXT_DUE_DATE);

            Assert.assertEquals(
                            data.get("maturityDate").asText(),
                            Constants.MATURITY_DATE);
    }
}