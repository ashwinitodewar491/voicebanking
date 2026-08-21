package com.voicebanking.DataText;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class Constants {

    private Constants() {
    }

    // Success messages
    public static final String MESSAGE_EXIST = "Response does not contain message field";
    public static final String ACCOUNT_LIST_SUCCESS_MESSAGE = "Account list fetched successfully";
    public static final String CUSTOMER_INFO_SUCCESS_MESSAGE = "Customer info fetched successfully";
    public static final String ACCOUNT_BALANCE_SUCCESS_MESSAGE = "Account details fetched successfully";
    public static final String BENEFICIARIES_LIST_SUCCESS_MESSAGE = "Beneficiary list fetched successfully";
    public static final String TRANSACTIONS_LIST_SUCCESS_MESSAGE = "Transaction list fetched successfully";
    public static final String TRANSFER_MONEY_SUCCESS_MESSAGE = "Amount Transferred successfully";
    public static final String LOAN_STATEMENT_SUCCESS_MESSAGE = "Loan statement fetched successfully";
    public static final String LOAN_OVERDUE_SUCCESS_MESSAGE = "Overdue loan details fetched successfully";
    public static final String LOAN_SUMMARY_SUCCESS_MESSAGE = "Loan summary fetched successfully";

    // Fixed values — Customer A (dual account, two loans): CIF202602260005 / Rohit Mehta
    public static final String EXISTING_CUSTOMER_ID = "CIF202602260005";
    public static final String EXISTING_ACCOUNT_ID = "ACC202602260007";

    // Loan summary test reuses Customer A (CIF202602260005 / Rohit Mehta) — has two loan
    // product types (HOME_LOAN active, EDUCATION_LOAN closed). Suresh Patel (CIF202602260007) is
    // now only a beneficiary of Customer A's CURRENT account, not a standalone test customer.
    public static final String LOAN_SUMMARY_CUSTOMER_ID = "CIF202602260005";
    public static final String SUCCESS_STATUS = "success";
    public static final int SUCCESS_STATUS_CODE = 200;

    // Account types
    public static final String SAVINGS_ACCOUNT_TYPE = "SAVINGS";
    public static final String CURRENT_ACCOUNT_TYPE = "CURRENT";
    public static final String ACTIVE_STATUS = "ACTIVE";

    // Customer Info Field validation and actual value verification
    public static final String VERIFIED_KYC_STATUS = "VERIFIED";
    public static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    public static final String MOBILE_REGEX = "^\\d{10}$";
    public static final String EXPECTED_CUSTOMER_ID = "CIF202602260005";
    public static final String EXPECTED_CUSTOMER_NAME = "Rohit Mehta";
    public static final String EXPECTED_CUSTOMER_EMAIL = "rohit.mehta@gmail.com";
    public static final String EXPECTED_CUSTOMER_MOBILE = "9898989898";
    public static final String EXPECTED_CUSTOMER_DOB = "1987-01-19";

    // Account Balance
    public static final String EXPECTED_ACCOUNT_ID = "ACC202602260007";
    public static final String EXPECTED_MASKED_ACCOUNT = "XXXX0007";
    // Live balance, drifts by -1.00 with every real-money API6 transfer test run — refresh as
    // needed rather than treating as permanently fixed.
    public static final double EXPECTED_ACCOUNT_BALANCE = 359472.00;

    // Customer A (Rohit Mehta)'s CURRENT account — SAVINGS is EXPECTED_ACCOUNT_ID above.
    public static final String CUSTOMER_A_CURRENT_ACCOUNT_ID = "ACC202602260008";

    // Beneficiary List
    public static final String BENEFICIARY_ID = "beneficiaryId";
    public static final String NAME = "name";
    public static final String BANK_NAME = "bankName";
    public static final String IFSC = "ifsc";
    public static final String STATUS = "status";
    public static final String TRANSFER_TYPE = "transferType";
    public static final String BENEFICIARY_1_NAME = "Ananya Iyer";
    public static final String BENEFICIARY_1_BANK = "Punjab National Bank";
    public static final String BENEFICIARY_1_IFSC = "PUNB0000051";
    public static final String BENEFICIARY_1_TRANSFER_TYPE = "IMPS";
    public static final int EXPECTED_BENEFICIARY_COUNT = 2;
    public static final List<String> VALID_BENEFICIARY_STATUSES = Arrays.asList("ACTIVE", "INACTIVE");
    public static final List<String> VALID_TRANSFER_TYPES = Arrays.asList("IMPS", "NEFT", "RTGS");
    public static final String BENEFICIARY_LIST_NOT_EMPTY = "Beneficiary list should not be empty";
    public static final String BENEFICIARY_ID_EMPTY = "Beneficiary ID should not be empty";
    public static final String BENEFICIARY_NAME_EMPTY = "Beneficiary name should not be empty";
    public static final String BANK_NAME_EMPTY = "Bank name should not be empty";
    public static final String IFSC_EMPTY = "IFSC should not be empty";

    //Loan Statement — LN10005, Rohit Mehta's Home Loan
    public static final String LOAN_ACCOUNT_ID = "LN10005";

    public static final String EMI_PAYMENT_DESCRIPTION = "EMI Payment | Due day 2";

    public static final double EMI_AMOUNT = 15750.00;

    public static final int LOAN_STATEMENT_PAGE = 0;
    public static final int LOAN_STATEMENT_SIZE = 10;
    public static final int LOAN_STATEMENT_TOTAL_PAGES = 6;
    public static final int LOAN_STATEMENT_TOTAL_ELEMENTS = 51;
    // Loan Statement

    public static final String LOAN_STATEMENT_FROM_DATE = "2022-03-01";
    public static final String LOAN_STATEMENT_TO_DATE = "2026-09-01";

    //Loan Overdue — LN10005
    public static final String CUSTOMER_SHORT_NAME = "Rohit Mehta";
    public static final double TOTAL_OVERDUE_AMOUNT = 0.0;

    public static final double LOAN_AMOUNT = 575000.0;
    public static final double PRINCIPAL_BALANCE = 333270.0;
    public static final double TOTAL_OUTSTANDINGS = 333270.0;
    public static final double AVAILABLE_BALANCE = 26661.60;
    public static final double MIN_AMOUNT_DUE = 15750.0;
    public static final double NEXT_INSTALLMENT_AMOUNT = 15750.0;
    public static final double AMOUNT_PAID_TODAY = 0.0;

    public static final String NEXT_DUE_DATE = "2026-09-02";
    public static final String MATURITY_DATE = "2042-06-02";

    //Loan Summary list — Customer A (CIF202602260005 / Rohit Mehta): HOME_LOAN + EDUCATION_LOAN
    public static final String CUSTOMER_NAME = "Rohit Mehta";
    public static final String INR_CURRENCY = "INR";
    public static final String MONTH_TERM_UNIT = "M";
    public static final List<String> VALID_LOAN_STATUSES = Arrays.asList("ACTIVE", "CLOSED");

    public static final String TRANSACTION_TO_DATE = "2026-07-08";

    // Negative test data
    public static final String INVALID_CUSTOMER_ID = "INVALID_CIF_999";
    public static final String INVALID_ACCOUNT_ID = "INVALID_ACC_999";
    public static final String INVALID_LOAN_ACCOUNT_ID = "INVALID_LN_999";
    public static final String INVALID_BENEFICIARY_ID = "00000000-0000-0000-0000-000000000000";
    public static final String INVALID_ACCOUNT_TYPE = "INVALID_TYPE";
    public static final String INVALID_DATE_FORMAT = "not-a-date";
    public static final String REVERSED_FROM_DATE = "2026-09-01";
    public static final String REVERSED_TO_DATE = "2022-03-01";
    public static final double EXCESSIVE_TRANSFER_AMOUNT = 9999999.99;
    public static final double ZERO_TRANSFER_AMOUNT = 0.0;
    public static final double NEGATIVE_TRANSFER_AMOUNT = -100.0;

    // Error response constants
    public static final String ERROR_STATUS = "error";
    public static final String ERR_CUSTOMER_ID_REQUIRED = "customerId is required";
    public static final String ERR_ACCOUNT_ID_REQUIRED = "accountId is required";
    public static final String ERR_AMOUNT_POSITIVE = "Amount must be greater than 0";
    public static final String ERR_ACCOUNT_NOT_FOUND = "Account not found";
    public static final String ERR_LOAN_NOT_FOUND = "Loan details not found";
    public static final String ERR_BENEFICIARY_NOT_FOUND = "Beneficiary not found";
    public static final String ERR_INSUFFICIENT_BALANCE = "Insufficient balance";
    public static final String ERR_PAGE_SIZE_MIN = "size must be >= 1";
    public static final String ERR_DATE_RANGE_REVERSED = "toDate must not be before fromDate";
    public static final String ERR_LOAN_ACCOUNT_REQUIRED = "account is required";
    public static final String ERR_BOTH_IDS_REQUIRED = "accountId is required; customerId is required";
    public static final String ERR_CUSTOMER_NOT_FOUND = "not found";
    public static final String ERR_NULL_ID = "The given id must not be null";

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 100;

    // Transfer test — sender is Customer A (Rohit Mehta, CIF202602260005 / ACC202602260007
    // savings); receiver is Ananya Iyer, a real beneficiary on that account. One-way only: Ananya
    // Iyer doesn't have Rohit Mehta as her own beneficiary, so there's no cleanup/refund path yet.
    public static final String SENDER_CUSTOMER_ID_ORIGINAL = "CIF202602260005";
    public static final String SENDER_ACCOUNT_ID_ORIGINAL = "ACC202602260007";
    public static final String RECEIVER_CUSTOMER_ID_1 = "CIF202602260006";
    public static final String RECEIVER_ACCOUNT_ID_1 = "ACC202602260009";
    public static final String RECEIVER_BENEFICIARY_ID_1 = "05678e14-f770-2f79-84e8-653dd144a25b";

    // --- Ground-truth cross-verification customers (UI7/UI8/UI9 known-customer rows) ---

    // Customer B — Leena Kamat: savings-only, no loan, no beneficiary. Already used inline
    // ("9812341042") by UI7/UI8/UI11; named here so cross-verification call sites don't repeat
    // the literal.
    public static final String CUSTOMER_B_CUSTOMER_ID = "CIF202602260042";
    public static final String CUSTOMER_B_PHONE = "9812341042";
    public static final String CUSTOMER_B_SAVINGS_ACCOUNT_ID = "ACC202602260067";

    // Customer C — Aniket More: savings-only, one active PERSONAL_LOAN, one beneficiary (Priya
    // Singh).
    public static final String CUSTOMER_C_CUSTOMER_ID = "CIF202602260041";
    public static final String CUSTOMER_C_PHONE = "9812341041";
    public static final String CUSTOMER_C_SAVINGS_ACCOUNT_ID = "ACC202602260066";
    public static final String CUSTOMER_C_LOAN_ACCOUNT_ID = "LN10041";

    // Maps a known seeded customer's login phone number to their customerId — used by
    // BaseVoiceTest#currentCustomerId() so a follow-up flow (e.g. UI9's loan-detail walk) can look
    // up ground-truth API data for whichever customer is currently logged in without every call
    // site having to thread customerId through separately.
    public static final Map<String, String> CUSTOMER_ID_BY_PHONE = Map.of(
            EXPECTED_CUSTOMER_MOBILE, EXPECTED_CUSTOMER_ID,
            CUSTOMER_B_PHONE, CUSTOMER_B_CUSTOMER_ID,
            CUSTOMER_C_PHONE, CUSTOMER_C_CUSTOMER_ID);
}
