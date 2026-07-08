package com.voicebanking.tests.api;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.voicebanking.DataText.Constants;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.pages.BaseApiPage;

public class API6_TransferMoneyTest extends BaseApiPage {

    private JsonNode transferMoney(
            String customerId,
            String accountId,
            String beneficiaryId,
            double amount,
            String description) throws Exception {

        Map<String, Object> request = new HashMap<>();

        request.put("customerId", customerId);
        request.put("fromAccountId", accountId);
        request.put("beneficiaryId", beneficiaryId);
        request.put("amount", amount);
        request.put("description", description);

        return apiClient.post(
                Endpoints.TRANSFER_MONEY,
                request);
    }

    private double getBalance(
            String accountId,
            String customerId) throws Exception {

        Map<String, String> request = new HashMap<>();

        request.put("accountId", accountId);
        request.put("customerId", customerId);
        request.put("accountType", Constants.SAVINGS_ACCOUNT_TYPE);

        JsonNode response
                = apiClient.post(Endpoints.ACCOUNT_BALANCE, request);

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.SUCCESS_STATUS);

        return response.get("data")
                .get("balance")
                .asDouble();
    }

    @Test(groups = {"smoke", "regression", "api"}, description
            = "Verify transfer, balance deduction, credit and cleanup")
    public void testTransferBalanceChaining() throws Exception {

        double amount = 10.00;

        // Given
        double senderBefore = getBalance(
                Constants.SENDER_ACCOUNT_ID_ORIGINAL,
                Constants.SENDER_CUSTOMER_ID_ORIGINAL);
        System.out.println("Sender Before: " + senderBefore);

        double receiverBefore = getBalance(
                Constants.RECEIVER_ACCOUNT_ID_1,
                Constants.RECEIVER_CUSTOMER_ID_1);
        System.out.println("Receiver Before: " + receiverBefore);

        // When
        JsonNode transferResponse = transferMoney(
                Constants.SENDER_CUSTOMER_ID_ORIGINAL,
                Constants.SENDER_ACCOUNT_ID_ORIGINAL,
                Constants.RECEIVER_BENEFICIARY_ID_1,
                amount,
                "Transfer Validation Test");
        System.out.println("Transfer Response: " + transferResponse);

        // Then
        Assert.assertEquals(
                transferResponse.get("status").asText(),
                Constants.SUCCESS_STATUS);

        double senderAfter = getBalance(
                Constants.SENDER_ACCOUNT_ID_ORIGINAL,
                Constants.SENDER_CUSTOMER_ID_ORIGINAL);
        System.out.println("Sender After: " + senderAfter);

        double receiverAfter = getBalance(
                Constants.RECEIVER_ACCOUNT_ID_1,
                Constants.RECEIVER_CUSTOMER_ID_1);
        System.out.println("Receiver After: " + receiverAfter);

        Assert.assertEquals(
                senderAfter,
                senderBefore - amount,
                0.01);

        Assert.assertEquals(
                receiverAfter,
                receiverBefore + amount,
                0.01);

        Assert.assertEquals(
                senderAfter,
                transferResponse.get("data")
                        .get("balanceAfterTxn")
                        .asDouble(),
                0.01);

        // Cleanup
        JsonNode cleanupResponse = transferMoney(
                Constants.RECEIVER_CUSTOMER_ID_1,
                Constants.RECEIVER_ACCOUNT_ID_1,
                Constants.SENDER_BENEFICIARY_ID_ORIGINAL,
                amount,
                "Cleanup Refund");

        Assert.assertEquals(
                cleanupResponse.get("status").asText(),
                Constants.SUCCESS_STATUS,
                "Cleanup refund failed — sender balance will not be restored: " + cleanupResponse);

        // Verify cleanup
        Assert.assertEquals(
                getBalance(
                        Constants.SENDER_ACCOUNT_ID_ORIGINAL,
                        Constants.SENDER_CUSTOMER_ID_ORIGINAL),
                senderBefore,
                0.01);

        Assert.assertEquals(
                getBalance(
                        Constants.RECEIVER_ACCOUNT_ID_1,
                        Constants.RECEIVER_CUSTOMER_ID_1),
                receiverBefore,
                0.01);
    }

    // --- Negative Tests ---

    @Test(groups = {"negative", "regression", "api"},
            description = "Transfer API should reject invalid beneficiary ID")
    public void testTransferWithInvalidBeneficiaryId() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("customerId", Constants.SENDER_CUSTOMER_ID_ORIGINAL);
        request.put("fromAccountId", Constants.SENDER_ACCOUNT_ID_ORIGINAL);
        request.put("beneficiaryId", Constants.INVALID_BENEFICIARY_ID);
        request.put("amount", 10.0);
        request.put("description", "Negative test");

        JsonNode response = apiClient.post(Endpoints.TRANSFER_MONEY, request);

        Assert.assertNotEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API should reject invalid beneficiaryId");

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.ERROR_STATUS,
                "Response status should be error");

        Assert.assertEquals(
                response.get("error").get("message").asText(),
                Constants.ERR_BENEFICIARY_NOT_FOUND,
                "Error message should indicate beneficiary not found");
    }

    @Test(groups = {"negative", "regression", "api"},
            description = "Transfer API should reject zero amount")
    public void testTransferWithZeroAmount() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("customerId", Constants.SENDER_CUSTOMER_ID_ORIGINAL);
        request.put("fromAccountId", Constants.SENDER_ACCOUNT_ID_ORIGINAL);
        request.put("beneficiaryId", Constants.RECEIVER_BENEFICIARY_ID_1);
        request.put("amount", Constants.ZERO_TRANSFER_AMOUNT);
        request.put("description", "Negative test");

        JsonNode response = apiClient.post(Endpoints.TRANSFER_MONEY, request);

        Assert.assertNotEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API should reject zero transfer amount");

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.ERROR_STATUS,
                "Response status should be error");

        Assert.assertEquals(
                response.get("error").get("message").asText(),
                Constants.ERR_AMOUNT_POSITIVE,
                "Error message should indicate amount must be greater than 0");
    }

    @Test(groups = {"negative", "regression", "api"},
            description = "Transfer API should reject negative amount")
    public void testTransferWithNegativeAmount() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("customerId", Constants.SENDER_CUSTOMER_ID_ORIGINAL);
        request.put("fromAccountId", Constants.SENDER_ACCOUNT_ID_ORIGINAL);
        request.put("beneficiaryId", Constants.RECEIVER_BENEFICIARY_ID_1);
        request.put("amount", Constants.NEGATIVE_TRANSFER_AMOUNT);
        request.put("description", "Negative test");

        JsonNode response = apiClient.post(Endpoints.TRANSFER_MONEY, request);

        Assert.assertNotEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API should reject negative transfer amount");

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.ERROR_STATUS,
                "Response status should be error");

        Assert.assertEquals(
                response.get("error").get("message").asText(),
                Constants.ERR_AMOUNT_POSITIVE,
                "Error message should indicate amount must be greater than 0");
    }

    @Test(groups = {"negative", "regression", "api"},
            description = "Transfer API should reject amount exceeding account balance")
    public void testTransferWithInsufficientFunds() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("customerId", Constants.SENDER_CUSTOMER_ID_ORIGINAL);
        request.put("fromAccountId", Constants.SENDER_ACCOUNT_ID_ORIGINAL);
        request.put("beneficiaryId", Constants.RECEIVER_BENEFICIARY_ID_1);
        request.put("amount", Constants.EXCESSIVE_TRANSFER_AMOUNT);
        request.put("description", "Negative test");

        JsonNode response = apiClient.post(Endpoints.TRANSFER_MONEY, request);

        Assert.assertNotEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API should reject transfer amount exceeding balance");

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.ERROR_STATUS,
                "Response status should be error");

        Assert.assertEquals(
                response.get("error").get("message").asText(),
                Constants.ERR_INSUFFICIENT_BALANCE,
                "Error message should indicate insufficient balance");
    }

    @Test(groups = {"negative", "regression", "api"},
            description = "Transfer API should reject non-existent source account ID")
    public void testTransferWithInvalidFromAccount() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("customerId", Constants.SENDER_CUSTOMER_ID_ORIGINAL);
        request.put("fromAccountId", Constants.INVALID_ACCOUNT_ID);
        request.put("beneficiaryId", Constants.RECEIVER_BENEFICIARY_ID_1);
        request.put("amount", 10.0);
        request.put("description", "Negative test");

        JsonNode response = apiClient.post(Endpoints.TRANSFER_MONEY, request);

        Assert.assertNotEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API should reject non-existent fromAccountId");

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
            description = "Transfer API should reject request with missing fields")
    public void testTransferWithMissingFields() throws Exception {
        Map<String, Object> request = new HashMap<>();

        JsonNode response = apiClient.post(Endpoints.TRANSFER_MONEY, request);

        Assert.assertNotEquals(
                response.get("statusCode").asInt(),
                Constants.SUCCESS_STATUS_CODE,
                "API should reject request with missing fields");

        Assert.assertEquals(
                response.get("status").asText(),
                Constants.ERROR_STATUS,
                "Response status should be error");

        Assert.assertEquals(
                response.get("error").get("message").asText(),
                Constants.ERR_NULL_ID,
                "Error message should indicate null ID error");
    }
}
