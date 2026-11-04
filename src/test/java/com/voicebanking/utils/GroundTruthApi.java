package com.voicebanking.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.voicebanking.DataText.Endpoints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Typed wrapper around {@link APIClient} for the ground-truth values UI tests cross-check against. */
public class GroundTruthApi {

    private final APIClient apiClient;

    public GroundTruthApi(String baseUrl) {
        this.apiClient = new APIClient(baseUrl);
    }

    /** One loan from the Loan Summary API. */
    public static class LoanRecord {
        public final String accountId;
        public final String loanType;
        public final double loanAmount;
        public final double interestRate;
        public final int loanTenure;
        public final int pendingTenure;
        public final double outstandingAmount;
        public final String accountStatus;

        private LoanRecord(JsonNode node) {
            this.accountId = node.get("accountId").asText();
            this.loanType = node.get("loanType").asText();
            this.loanAmount = node.get("loanAmount").asDouble();
            this.interestRate = node.get("interestRate").asDouble();
            this.loanTenure = node.get("loanTenure").asInt();
            this.pendingTenure = node.get("pendingTenure").asInt();
            this.outstandingAmount = node.get("outstandingAmount").asDouble();
            this.accountStatus = node.get("accountStatus").asText();
        }
    }

    /** Loan Overdue API response for one loan. */
    public static class OverdueRecord {
        public final double nextInstallmentAmount;
        public final String nextDueDate;
        public final double totalOutstandings;
        public final double minAmountDue;

        private OverdueRecord(JsonNode data) {
            this.nextInstallmentAmount = data.get("nextInstallmentAmount").asDouble();
            this.nextDueDate = data.get("nextDueDate").asText();
            this.totalOutstandings = data.get("totalOutstandings").asDouble();
            this.minAmountDue = data.get("minAmountDue").asDouble();
        }
    }

    /** One page of the Transactions List API. */
    public static class TransactionsPage {
        public final int totalElements;
        public final List<Double> amounts;

        private TransactionsPage(JsonNode data) {
            this.totalElements = data.get("totalElements").asInt();
            this.amounts = new ArrayList<>();
            if (data.has("transactionList")) {
                for (JsonNode txn : data.get("transactionList")) {
                    this.amounts.add(txn.get("amount").asDouble());
                }
            }
        }
    }

    /** All loans for {@code customerId}; empty if there are none. */
    public List<LoanRecord> loanSummary(String customerId) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("customerId", customerId);

        JsonNode response = apiClient.post(Endpoints.LOAN_SUMMARY, body);
        List<LoanRecord> loans = new ArrayList<>();
        if (!response.has("data") || !response.get("data").has("loanDetails")) {
            return loans;
        }
        for (JsonNode loan : response.get("data").get("loanDetails")) {
            loans.add(new LoanRecord(loan));
        }
        return loans;
    }

    /** Overdue/EMI details for a loan account id, e.g. "LN10005". */
    public OverdueRecord loanOverdue(String loanAccountId) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("accountId", loanAccountId);

        JsonNode response = apiClient.post(Endpoints.LOAN_OVERDUE, body);
        return new OverdueRecord(response.get("data"));
    }

    /** Current balance for one account. */
    public double accountBalance(String accountId, String customerId, String accountType) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("accountId", accountId);
        body.put("customerId", customerId);
        body.put("accountType", accountType);

        JsonNode response = apiClient.post(Endpoints.ACCOUNT_BALANCE, body);
        return response.get("data").get("balance").asDouble();
    }

    /** Transactions for {@code accountId} up to {@code toDate} (yyyy-MM-dd). */
    public TransactionsPage transactions(String accountId, String toDate) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("accountId", accountId);
        body.put("toDate", toDate);
        body.put("page", 0);
        body.put("size", 100);

        JsonNode response = apiClient.post(Endpoints.TRANSACTIONS_LIST, body);
        return new TransactionsPage(response.get("data"));
    }
}
