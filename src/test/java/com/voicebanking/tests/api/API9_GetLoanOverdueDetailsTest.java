package com.voicebanking.tests.api;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.voicebanking.DataText.Constants;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.pages.BaseApiPage;

public class API9_GetLoanOverdueDetailsTest extends BaseApiPage {

    private JsonNode getLoanOverdueResponse() throws Exception {

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.LOAN_ACCOUNT_ID);

        return apiClient.post(
                Endpoints.LOAN_OVERDUE,
                requestBody);
    }

    @Test(groups = {"smoke", "regression", "api"}, description = "Should validate loan overdue response structure")
    public void testLoanOverdueResponse() throws Exception {

        JsonNode response = getLoanOverdueResponse();

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
                Constants.LOAN_OVERDUE_SUCCESS_MESSAGE);

        JsonNode data = response.get("data");

        Assert.assertTrue(data.has("loanAccountId"));
        Assert.assertTrue(data.has("customerShortName"));
        Assert.assertTrue(data.has("loanAmount"));
        Assert.assertTrue(data.has("principalBalance"));
        Assert.assertTrue(data.has("totalOutstandings"));
        Assert.assertTrue(data.has("totalOverdueAmount"));
        Assert.assertTrue(data.has("availableBalance"));
        Assert.assertTrue(data.has("minAmountDue"));
        Assert.assertTrue(data.has("nextInstallmentAmount"));
        Assert.assertTrue(data.has("amountPaidToday"));
        Assert.assertTrue(data.has("nextDueDate"));
        Assert.assertTrue(data.has("maturityDate"));
    }

    @Test(groups = {"regression", "api"}, description = "Should validate overdue loan business data")
    public void testLoanOverdueDataValidation() throws Exception {

        JsonNode response = getLoanOverdueResponse();
        JsonNode data = response.get("data");

        // Exact value validation
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
                data.get("totalOverdueAmount").asDouble(),
                Constants.TOTAL_OVERDUE_AMOUNT);

        Assert.assertEquals(
                data.get("nextDueDate").asText(),
                Constants.NEXT_DUE_DATE);

        Assert.assertEquals(
                data.get("maturityDate").asText(),
                Constants.MATURITY_DATE);

        // Numeric validations
        Assert.assertTrue(data.get("loanAmount").asDouble() > 0);
        Assert.assertTrue(data.get("principalBalance").asDouble() >= 0);
        Assert.assertTrue(data.get("totalOutstandings").asDouble() >= 0);
        Assert.assertTrue(data.get("availableBalance").asDouble() >= 0);
        Assert.assertTrue(data.get("minAmountDue").asDouble() >= 0);
        Assert.assertTrue(data.get("nextInstallmentAmount").asDouble() >= 0);
        Assert.assertTrue(data.get("amountPaidToday").asDouble() >= 0);
        Assert.assertTrue(data.get("totalOverdueAmount").asDouble() >= 0);
    }

    // --- Negative Tests ---

    @Test(groups = {"negative", "regression", "api"},
            description = "Loan overdue API should reject non-existent loan account ID")
    public void testLoanOverdueWithInvalidAccountId() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("accountId", Constants.INVALID_LOAN_ACCOUNT_ID);

        JsonNode response = apiClient.post(Endpoints.LOAN_OVERDUE, requestBody);

        Assert.assertNotEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API should reject non-existent loan accountId");

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.ERROR_STATUS,
                "Response status should be error");

        Assert.assertEquals(
                response.get("error").get("message").asText(),
                Constants.ERR_LOAN_NOT_FOUND,
                "Error message should indicate loan details not found");
    }

    @Test(groups = {"negative", "regression", "api"},
            description = "Loan overdue API should reject empty account ID")
    public void testLoanOverdueWithEmptyAccountId() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("accountId", "");

        JsonNode response = apiClient.post(Endpoints.LOAN_OVERDUE, requestBody);

        Assert.assertNotEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API should reject empty accountId");

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.ERROR_STATUS,
                "Response status should be error");

        Assert.assertEquals(
                response.get("error").get("message").asText(),
                Constants.ERR_CUSTOMER_ID_REQUIRED,
                "Error message should indicate customerId is required");
    }

    @Test(groups = {"negative", "regression", "api"},
            description = "Loan overdue API should reject request with missing account ID field")
    public void testLoanOverdueWithMissingAccountId() throws Exception {
        Map<String, String> requestBody = new HashMap<>();

        JsonNode response = apiClient.post(Endpoints.LOAN_OVERDUE, requestBody);

        Assert.assertNotEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API should reject request missing accountId");

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.ERROR_STATUS,
                "Response status should be error");

        Assert.assertEquals(
                response.get("error").get("message").asText(),
                Constants.ERR_CUSTOMER_ID_REQUIRED,
                "Error message should indicate customerId is required");
    }
}
