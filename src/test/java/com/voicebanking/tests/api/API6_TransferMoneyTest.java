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

    @Test(groups = {"smoke", "sanity", "regression"}, description
            = "Verify transfer, balance deduction, credit and cleanup")
    public void testTransferBalanceChaining() throws Exception {

        double amount = 10.00;

        // Given
        double senderBefore = getBalance(
                Constants.EXISTING_ACCOUNT_ID_1,
                Constants.EXISTING_CUSTOMER_ID_1);

        double receiverBefore = getBalance(
                Constants.RECEIVER_ACCOUNT_ID_1,
                Constants.RECEIVER_CUSTOMER_ID_1);

        // When
        JsonNode transferResponse = transferMoney(
                Constants.EXISTING_CUSTOMER_ID_1,
                Constants.EXISTING_ACCOUNT_ID_1,
                Constants.RECEIVER_BENEFICIARY_ID_1,
                amount,
                "Transfer Validation Test");

        // Then
        Assert.assertEquals(
                transferResponse.get("status").asText(),
                Constants.SUCCESS_STATUS);

        double senderAfter = getBalance(
                Constants.EXISTING_ACCOUNT_ID,
                Constants.EXISTING_CUSTOMER_ID_1);

        double receiverAfter = getBalance(
                Constants.RECEIVER_ACCOUNT_ID_1,
                Constants.RECEIVER_CUSTOMER_ID_1);

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
                Constants.ORIGINAL_SENDER_BENEFICIARY_ID_1,
                amount,
                "Cleanup Refund");

        // Verify cleanup
        Assert.assertEquals(
                getBalance(
                        Constants.EXISTING_ACCOUNT_ID_1,
                        Constants.EXISTING_CUSTOMER_ID_1),
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
