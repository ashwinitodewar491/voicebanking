package com.voicebanking.DataText;

import java.util.Arrays;
import java.util.List;

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

    // Fixed values
    public static final String EXISTING_CUSTOMER_ID = "CIF202602260001";
    public static final String EXISTING_ACCOUNT_ID = "ACC202602260001";
    public static final String RECEIVER_CUSTOMER_ID = "CIF202602260002";
    public static final String RECEIVER_ACCOUNT_ID = "ACC202602260006";
    public static final String RECEIVER_BENEFICIARY_ID = "a3c7e2b5-6f44-4a01-8c45-3f8c2e7b4d04";
    public static final String ORIGINAL_SENDER_BENEFICIARY_ID = "";
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
    public static final String EXPECTED_CUSTOMER_ID = "CIF202602260001";
    public static final String EXPECTED_CUSTOMER_NAME = "Amit Sharma";
    public static final String EXPECTED_CUSTOMER_EMAIL = "amit.sharma@gmail.com";
    public static final String EXPECTED_CUSTOMER_MOBILE = "9876543210";
    public static final String EXPECTED_CUSTOMER_DOB = "1990-05-21";

    // Account Balance
    public static final String EXPECTED_ACCOUNT_ID = "ACC202602260001";
    public static final String EXPECTED_MASKED_ACCOUNT = "XXXX0001";
    public static final double EXPECTED_ACCOUNT_BALANCE = 6931.15;

    // Beneficiary List
    public static final String BENEFICIARY_ID = "beneficiaryId";
    public static final String NAME = "name";
    public static final String BANK_NAME = "bankName";
    public static final String IFSC = "ifsc";
    public static final String STATUS = "status";
    public static final String TRANSFER_TYPE = "transferType";
    public static final String BENEFICIARY_1_NAME = "Rohit Malhotra";
    public static final String BENEFICIARY_1_BANK = "State Bank of India";
    public static final String BENEFICIARY_1_IFSC = "SBIN0000456";
    public static final String BENEFICIARY_1_TRANSFER_TYPE = "IMPS";
    public static final String BENEFICIARY_2_NAME = "Neeraj Khanna";
    public static final String BENEFICIARY_2_BANK = "HDFC Bank";
    public static final String BENEFICIARY_2_IFSC = "HDFC0001234";
    public static final String BENEFICIARY_2_TRANSFER_TYPE = "NEFT";
    public static final int EXPECTED_BENEFICIARY_COUNT = 2;
    public static final List<String> VALID_BENEFICIARY_STATUSES = Arrays.asList("ACTIVE", "INACTIVE");
    public static final List<String> VALID_TRANSFER_TYPES = Arrays.asList("IMPS", "NEFT", "RTGS");
    public static final String BENEFICIARY_LIST_NOT_EMPTY = "Beneficiary list should not be empty";
    public static final String BENEFICIARY_ID_EMPTY = "Beneficiary ID should not be empty";
    public static final String BENEFICIARY_NAME_EMPTY = "Beneficiary name should not be empty";
    public static final String BANK_NAME_EMPTY = "Bank name should not be empty";
    public static final String IFSC_EMPTY = "IFSC should not be empty";

    //Loan Statement
    public static final String LOAN_ACCOUNT_ID = "LN10001";

    public static final String EMI_PAYMENT_DESCRIPTION = "EMI Payment";

    public static final double EMI_AMOUNT = 15000.00;

    public static final int LOAN_STATEMENT_PAGE = 0;
    public static final int LOAN_STATEMENT_SIZE = 10;
    public static final int LOAN_STATEMENT_TOTAL_PAGES = 5;
    public static final int LOAN_STATEMENT_TOTAL_ELEMENTS = 50;
    // Loan Statement

    public static final String LOAN_STATEMENT_FROM_DATE = "2022-03-01";
    public static final String LOAN_STATEMENT_TO_DATE = "2026-09-01";

    //Loan Overdue
    public static final String CUSTOMER_SHORT_NAME = "Amit Sharma";
    public static final double TOTAL_OVERDUE_AMOUNT = 0.0;

    public static final double LOAN_AMOUNT = 500000.0;
    public static final double PRINCIPAL_BALANCE = 450000.0;
    public static final double TOTAL_OUTSTANDINGS = 460000.0;
    public static final double AVAILABLE_BALANCE = 50000.0;
    public static final double MIN_AMOUNT_DUE = 15000.0;
    public static final double NEXT_INSTALLMENT_AMOUNT = 15000.0;
    public static final double AMOUNT_PAID_TODAY = 15000.0;

    public static final String NEXT_DUE_DATE = "2026-06-10";
    public static final String MATURITY_DATE = "2042-01-10";

    //Loan Summary list
    public static final String CUSTOMER_NAME = "Amit Sharma";
    public static final String INR_CURRENCY = "INR";
    public static final String MONTH_TERM_UNIT = "M";

    public static final String TRANSACTION_TO_DATE = "2026-05-26";

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 100;

    public static final String SENDER_CUSTOMER_ID_ORIGINAL = "CIF202602260028";
    public static final String SENDER_ACCOUNT_ID_ORIGINAL = "ACC202602260033";
    public static final String RECEIVER_CUSTOMER_ID_1 = "CIF202602260029";
    public static final String RECEIVER_ACCOUNT_ID_1 = "ACC202602260034";
    public static final String RECEIVER_BENEFICIARY_ID_1 = "0ca1d48f-f967-8a5b-e142-8d76c041e6cc";
    public static final String SENDER_BENEFICIARY_ID_ORIGINAL = "c8fe1a72-3fe3-76d4-d4f3-9d2e085d3a88";

}
