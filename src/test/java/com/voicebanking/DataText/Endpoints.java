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

    public static final String UI_BASE_URL_PROD
            = "https://voicebank.joshsoftware.com";

    // UI-only environment — no API base URL given for dev yet, so getBaseUrl() still falls back
    // to prod for -Denv=dev. Fine for now since this was added specifically for UI testing.
    public static final String UI_BASE_URL_DEV
            = "https://voicebank-dev.joshsoftware.com";

    private static String resolveEnv() {
        return System.getProperty("env", System.getenv("ENV") != null ? System.getenv("ENV") : "prod");
    }

    public static String getBaseUrl() {
        String env = resolveEnv();
        return "stage".equalsIgnoreCase(env) ? BASE_URL_STAGE : BASE_URL_PROD;
    }

    public static String getUiBaseUrl() {
        String env = resolveEnv();
        if ("stage".equalsIgnoreCase(env)) return UI_BASE_URL_STAGE;
        if ("dev".equalsIgnoreCase(env)) return UI_BASE_URL_DEV;
        return UI_BASE_URL_PROD;
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
