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
        transferMoney(
                Constants.RECEIVER_CUSTOMER_ID_1,
                Constants.RECEIVER_ACCOUNT_ID_1,
                Constants.SENDER_BENEFICIARY_ID_ORIGINAL,
                amount,
                "Cleanup Refund");

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

}
