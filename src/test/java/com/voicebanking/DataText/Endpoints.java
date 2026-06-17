package com.voicebanking.DataText;

public final class Endpoints {

    private Endpoints() {
    }

    public static final String BASE_URL_PROD
            = "http://98.93.75.232:9090";

    public static final String BASE_URL_STAGE
            = "http://3.111.41.3:9090";

    public static final String UI_BASE_URL_STAGE
            = "https://voicebank-stage.joshsoftware.com";

    public static String getBaseUrl() {
        String env = System.getProperty("env", System.getenv("ENV") != null ? System.getenv("ENV") : "prod");
        return "stage".equalsIgnoreCase(env) ? BASE_URL_STAGE : BASE_URL_PROD;
    }

    public static String getUiBaseUrl() {
        return UI_BASE_URL_STAGE;
    }

    public static final String ACCOUNT_LIST
            = "/api/v1/accounts/list";

    public static final String CUSTOMER_INFO
            = "/api/v1/customers/info";

    public static final String ACCOUNT_BALANCE
            = "/api/v1/accounts/balance";

    public static final String BENEFICIARIES_LIST
            = "/api/v1/beneficiaries/list";

    public static final String TRANSACTIONS_LIST
            = "/api/v1/transactions/list";

    public static final String TRANSFER_MONEY
            = "/api/v1/transactions/transfer";

    public static final String LOAN_STATEMENT
            = "/api/v1/loans/statement";

    public static final String LOAN_OVERDUE
            = "/api/v1/loans/overdue";

    public static final String LOAN_SUMMARY
            = "/api/v1/loans/summary";

}
