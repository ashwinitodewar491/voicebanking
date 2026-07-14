package com.voicebanking.tests.ui;

import com.voicebanking.DataText.BotResponsePatterns;
import com.voicebanking.DataText.VoiceQueries;
import com.voicebanking.tests.ui.base.BaseVoiceTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Customer A (Sneha Kulkarni, 9765432109) actually has two loans — Home Loan (LN10014) and
 * Personal Loan (LN10015), confirmed via a live trial run. "Education loan" is not one of them;
 * it's used deliberately in two rows as a negative test of the bot's fallback: named a loan type
 * that doesn't exist, the bot should list the real loans and ask which one, matching
 * BotResponsePatterns.Loans.LOAN_OPTIONS_PROMPT.
 * All 69 phrasings are active. Loan-agnostic queries (no type named) answer the disambiguation
 * follow-up alternating Home/Personal via the mic-reacquire fix in BaseVoiceTest. Most {loan type}
 * queries assert on keywords only — the exact response format is confirmed for EMI, interest,
 * outstanding, and next-EMI-due (upgraded to a precise pattern); everything else is exploratory.
 */
public class UI9_LoanInquiryTest extends BaseVoiceTest {

    @Override
    protected String getLoginPhoneNumber() {
        return "9765432109";
    }

    @DataProvider(name = "voiceQueries")
    public Object[][] voiceQueries() {
        return new Object[][]{

            // {queryName, query, expectedKeywords, assertionPattern, disambiguationAccount}

            // No loan type named — bot lists the real loans and asks which; these rows answer the
            // follow-up (alternating Home/Personal) and assert the resolved response. The
            // follow-up mic stream is force-reacquired before speaking (see
            // BaseVoiceTest.reacquireMicrophoneForFollowUp) so Chromium's fake audio device opens
            // fresh and picks up the overwritten WAV, instead of replaying whatever it originally
            // buffered for the main query.
            {"Loan Details",                  VoiceQueries.English.LOAN_DETAILS,
                    new String[]{"loan"}, null, "home loan"},
            {"Active Loans Any",               VoiceQueries.English.ACTIVE_LOANS_ANY,
                    new String[]{"loan"}, null, "personal loan"},

            // A nonexistent loan type — bot must list the real loans and ask, and these rows stop
            // there (no follow-up: disambiguationAccount is null) to assert the fallback itself.
            {"Loan Type Details Education",    VoiceQueries.English.LOAN_TYPE_DETAILS,
                    null, BotResponsePatterns.Loans.LOAN_OPTIONS_PROMPT, null},
            {"Loan Type EMI What Education",   VoiceQueries.English.LOAN_TYPE_EMI_WHAT,
                    null, BotResponsePatterns.Loans.LOAN_OPTIONS_PROMPT, null},

            // Home Loan — a real loan for this customer.
            {"Loan Type Details Home",         VoiceQueries.English.LOAN_TYPE_DETAILS_HOME,
                    new String[]{"loan", "home"}, null, null},
            {"Loan Type EMI Home",             VoiceQueries.English.LOAN_TYPE_EMI_HOME,
                    new String[]{"emi"}, null, null},
            {"Loan Type Interest Home",        VoiceQueries.English.LOAN_TYPE_INTEREST_HOME,
                    new String[]{"interest"}, null, null},
            {"Loan Type Outstanding Home",     VoiceQueries.English.LOAN_TYPE_OUTSTANDING_HOME,
                    null, BotResponsePatterns.Loans.OUTSTANDING, null},
            {"Loan Type Status Home",          VoiceQueries.English.LOAN_TYPE_STATUS_HOME,
                    new String[]{"status"}, null, null},

            // Personal Loan — the other real loan for this customer.
            {"Loan Type EMI Personal",         VoiceQueries.English.LOAN_TYPE_EMI_PERSONAL,
                    new String[]{"emi"}, null, null},
            {"Loan Type Interest Personal",    VoiceQueries.English.LOAN_TYPE_INTEREST_PERSONAL,
                    new String[]{"interest"}, null, null},
            {"Loan Type Outstanding Personal", VoiceQueries.English.LOAN_TYPE_OUTSTANDING_PERSONAL,
                    null, BotResponsePatterns.Loans.OUTSTANDING, null},
            {"Next EMI Due Personal",          VoiceQueries.English.LOAN_TYPE_NEXT_EMI_DUE_PERSONAL,
                    null, BotResponsePatterns.Loans.NEXT_EMI_DUE, null},
            // Remaining {loan type} phrasings — text already alternates home/personal loan in
            // VoiceQueries.java, so no disambiguation triggers; upgraded to a precise pattern
            // where the response format is already confirmed (outstanding, next EMI due).
            {"Loan Type Tell",                 VoiceQueries.English.LOAN_TYPE_TELL,
                    new String[]{"loan", "home"}, null, null},
            {"Loan Type What Can Tell",        VoiceQueries.English.LOAN_TYPE_WHAT_CAN_TELL,
                    new String[]{"loan", "personal"}, null, null},
            {"Loan Type Show Details",         VoiceQueries.English.LOAN_TYPE_SHOW_DETAILS,
                    new String[]{"loan", "home"}, null, null},
            {"Loan Type EMI Tell",             VoiceQueries.English.LOAN_TYPE_EMI_TELL,
                    new String[]{"emi"}, null, null},
            {"Loan Type EMI Want",             VoiceQueries.English.LOAN_TYPE_EMI_WANT,
                    new String[]{"emi"}, null, null},
            {"Loan Type EMI How Much",         VoiceQueries.English.LOAN_TYPE_EMI_HOW_MUCH,
                    new String[]{"emi"}, null, null},
            {"Loan Type Interest Tell",        VoiceQueries.English.LOAN_TYPE_INTEREST_TELL,
                    new String[]{"interest"}, null, null},
            {"Loan Type Interest Want",        VoiceQueries.English.LOAN_TYPE_INTEREST_WANT,
                    new String[]{"interest"}, null, null},
            {"Loan Type Tenure What",          VoiceQueries.English.LOAN_TYPE_TENURE_WHAT,
                    new String[]{"tenure"}, null, null},
            {"Loan Type Tenure For",           VoiceQueries.English.LOAN_TYPE_TENURE_FOR,
                    new String[]{"tenure"}, null, null},
            {"Loan Type Tenure Want",          VoiceQueries.English.LOAN_TYPE_TENURE_WANT,
                    new String[]{"tenure"}, null, null},
            {"Loan Type Tenure Remaining",     VoiceQueries.English.LOAN_TYPE_TENURE_REMAINING,
                    new String[]{"tenure", "remaining"}, null, null},
            {"Loan Type EMIs Remaining",       VoiceQueries.English.LOAN_TYPE_EMIS_REMAINING,
                    new String[]{"emi", "remaining"}, null, null},
            {"Loan Type Outstanding Tell",     VoiceQueries.English.LOAN_TYPE_OUTSTANDING_TELL,
                    null, BotResponsePatterns.Loans.OUTSTANDING, null},
            {"Loan Type Left To Pay",          VoiceQueries.English.LOAN_TYPE_LEFT_TO_PAY,
                    new String[]{"left", "outstanding", "pay"}, null, null},
            {"Loan Type Remaining Balance",    VoiceQueries.English.LOAN_TYPE_REMAINING_BALANCE,
                    new String[]{"remaining", "balance", "outstanding"}, null, null},
            {"Loan Type Sanctioned How Much",  VoiceQueries.English.LOAN_TYPE_SANCTIONED_HOW_MUCH,
                    new String[]{"sanctioned"}, null, null},
            {"Loan Type Sanctioned What",      VoiceQueries.English.LOAN_TYPE_SANCTIONED_WHAT,
                    new String[]{"sanctioned"}, null, null},
            {"Loan Type Total Amount",         VoiceQueries.English.LOAN_TYPE_TOTAL_AMOUNT,
                    new String[]{"loan", "amount"}, null, null},
            {"Loan Type Next EMI Due",         VoiceQueries.English.LOAN_TYPE_NEXT_EMI_DUE,
                    null, BotResponsePatterns.Loans.NEXT_EMI_DUE, null},
            {"Loan Type Payment Status",       VoiceQueries.English.LOAN_TYPE_PAYMENT_STATUS,
                    new String[]{"payment", "status"}, null, null},
            {"Loan Type Status",               VoiceQueries.English.LOAN_TYPE_STATUS,
                    new String[]{"status"}, null, null},
            {"Loan Type Pay Every Month",      VoiceQueries.English.LOAN_TYPE_PAY_EVERY_MONTH,
                    new String[]{"emi", "month"}, null, null},
            {"Loan Type Interest Generic",     VoiceQueries.English.LOAN_TYPE_INTEREST_GENERIC,
                    new String[]{"interest"}, null, null},
            {"Loan Type Amount Pending",       VoiceQueries.English.LOAN_TYPE_AMOUNT_PENDING,
                    new String[]{"pending", "outstanding"}, null, null},
            {"Loan Type Approved How Much",    VoiceQueries.English.LOAN_TYPE_APPROVED_HOW_MUCH,
                    new String[]{"sanctioned", "approved"}, null, null},

            // Loan-agnostic queries — no type named, so the bot disambiguates every time; each row
            // answers the follow-up (alternating Home/Personal) via the mic-reacquire fix.
            {"Loan Accounts Tell",             VoiceQueries.English.LOAN_ACCOUNTS_TELL,
                    new String[]{"loan"}, null, "home loan"},
            {"Loan Accounts What",             VoiceQueries.English.LOAN_ACCOUNTS_WHAT,
                    new String[]{"loan"}, null, "personal loan"},
            {"Loan Accounts Show",             VoiceQueries.English.LOAN_ACCOUNTS_SHOW,
                    new String[]{"loan"}, null, "home loan"},
            {"Loans What",                     VoiceQueries.English.LOANS_WHAT,
                    new String[]{"loan"}, null, "personal loan"},
            {"Loans Tell About",               VoiceQueries.English.LOANS_TELL_ABOUT,
                    new String[]{"loan"}, null, "home loan"},
            {"Active Loans Show",              VoiceQueries.English.ACTIVE_LOANS_SHOW,
                    new String[]{"loan", "active"}, null, "personal loan"},
            {"Loans Running Which",            VoiceQueries.English.LOANS_RUNNING_WHICH,
                    new String[]{"loan"}, null, "home loan"},
            {"Loans Currently Paying",         VoiceQueries.English.LOANS_CURRENTLY_PAYING,
                    new String[]{"loan"}, null, "personal loan"},
            {"EMI How Much",                   VoiceQueries.English.EMI_HOW_MUCH,
                    new String[]{"emi"}, null, "home loan"},
            {"Monthly Installment What",       VoiceQueries.English.MONTHLY_INSTALLMENT_WHAT,
                    new String[]{"installment", "emi"}, null, "personal loan"},
            {"Pay Every Month What",           VoiceQueries.English.PAY_EVERY_MONTH_WHAT,
                    new String[]{"emi", "month"}, null, "home loan"},
            {"Interest How Much",              VoiceQueries.English.INTEREST_HOW_MUCH,
                    new String[]{"interest"}, null, "personal loan"},
            {"Loan Interest Rate My",          VoiceQueries.English.LOAN_INTEREST_RATE_MY,
                    new String[]{"interest"}, null, "home loan"},
            {"Still Owe How Much",             VoiceQueries.English.STILL_OWE_HOW_MUCH,
                    new String[]{"owe", "outstanding", "remaining"}, null, "personal loan"},
            {"Loan Left How Much",             VoiceQueries.English.LOAN_LEFT_HOW_MUCH,
                    new String[]{"loan", "outstanding", "remaining"}, null, "home loan"},
            {"Remaining Loan Balance My",      VoiceQueries.English.REMAINING_LOAN_BALANCE_MY,
                    new String[]{"remaining", "balance"}, null, "personal loan"},
            {"EMIs Left How Many",             VoiceQueries.English.EMIS_LEFT_HOW_MANY,
                    new String[]{"emi"}, null, "home loan"},
            {"Installments Remaining How Many", VoiceQueries.English.INSTALLMENTS_REMAINING_HOW_MANY,
                    new String[]{"installment", "emi", "remaining"}, null, "personal loan"},
            {"Months Left Loan",               VoiceQueries.English.MONTHS_LEFT_LOAN,
                    new String[]{"month", "tenure", "remaining"}, null, "home loan"},
            {"Loan End When",                  VoiceQueries.English.LOAN_END_WHEN,
                    new String[]{"loan", "end", "tenure"}, null, "personal loan"},
            {"Remaining Tenure Loan",          VoiceQueries.English.REMAINING_TENURE_LOAN,
                    new String[]{"tenure", "remaining"}, null, "home loan"},
            {"Still Have To Pay How Long",     VoiceQueries.English.STILL_HAVE_TO_PAY_HOW_LONG,
                    new String[]{"tenure", "month", "remaining"}, null, "personal loan"},
            {"Loan Taken How Much",            VoiceQueries.English.LOAN_TAKEN_HOW_MUCH,
                    new String[]{"loan", "amount"}, null, "home loan"},
            {"Sanctioned Amount Was",          VoiceQueries.English.SANCTIONED_AMOUNT_WAS,
                    new String[]{"sanctioned"}, null, "personal loan"},
            {"Total Loan Amount What",         VoiceQueries.English.TOTAL_LOAN_AMOUNT_WHAT,
                    new String[]{"loan", "amount"}, null, "home loan"},
            {"Next EMI Due When",              VoiceQueries.English.NEXT_EMI_DUE_WHEN,
                    new String[]{"emi", "due"}, null, "personal loan"},
            {"Next EMI Date What",             VoiceQueries.English.NEXT_EMI_DATE_WHAT,
                    new String[]{"emi", "date"}, null, "home loan"},
            {"Payment Due When",               VoiceQueries.English.PAYMENT_DUE_WHEN,
                    new String[]{"emi", "due", "payment"}, null, "personal loan"},
            {"Installment Due When",           VoiceQueries.English.INSTALLMENT_DUE_WHEN,
                    new String[]{"emi", "due", "installment"}, null, "home loan"},
            {"Loan Status What",               VoiceQueries.English.LOAN_STATUS_WHAT,
                    new String[]{"status", "loan"}, null, "personal loan"},
            {"Loan Still Running",             VoiceQueries.English.LOAN_STILL_RUNNING,
                    new String[]{"active", "running", "loan"}, null, "home loan"},
            {"Loan Closed Has",                VoiceQueries.English.LOAN_CLOSED_HAS,
                    new String[]{"closed", "loan", "active"}, null, "personal loan"},
            {"Loan Amount Was",                VoiceQueries.English.LOAN_AMOUNT_WAS,
                    new String[]{"loan", "amount"}, null, "home loan"},
            {"Next EMI Due Date",              VoiceQueries.English.NEXT_EMI_DUE_DATE,
                    new String[]{"emi", "due"}, null, "personal loan"},
            {"Next EMI Pay When",              VoiceQueries.English.NEXT_EMI_PAY_WHEN,
                    new String[]{"emi", "due"}, null, "home loan"},
            {"Latest EMI Paid",                VoiceQueries.English.LATEST_EMI_PAID,
                    new String[]{"emi", "paid"}, null, "personal loan"},
            {"Loan Active Is",                 VoiceQueries.English.LOAN_ACTIVE_IS,
                    new String[]{"active", "loan"}, null, "home loan"},
        };
    }

    @Test(dataProvider = "voiceQueries", groups = {"ui", "regression", "botverification"},
            description = "Should process English loan-inquiry voice query and verify bot response")
    public void testVoiceQuery(String queryName, String query, String[] expectedKeywords,
                                String assertionPattern, String disambiguationAccount) throws Exception {
        runVoiceQuery(queryName, query, expectedKeywords, assertionPattern, disambiguationAccount);
    }

    /** One representative happy-path query — "is the bot alive and answering loan queries
     * correctly at all." Run this tier for a fast build/deploy health check. */
    @DataProvider(name = "smokeQueries")
    public Object[][] smokeQueries() {
        return new Object[][]{
            {"Loan Type EMI Home", VoiceQueries.English.LOAN_TYPE_EMI_HOME,
                    new String[]{"emi"}, null, null},
        };
    }

    @Test(dataProvider = "smokeQueries", groups = {"ui", "smoke", "botverification"},
            description = "Smoke: should process a basic English loan-inquiry voice query")
    public void testSmokeQuery(String queryName, String query, String[] expectedKeywords,
                                String assertionPattern, String disambiguationAccount) throws Exception {
        runVoiceQuery(queryName, query, expectedKeywords, assertionPattern, disambiguationAccount);
    }

    /** One query per major loan category: named-type direct answer, precise-pattern assertion,
     * loan-agnostic disambiguation, and the nonexistent-type fallback — broader than smoke,
     * still fast. */
    @DataProvider(name = "sanityQueries")
    public Object[][] sanityQueries() {
        return new Object[][]{
            {"Loan Type EMI Home",         VoiceQueries.English.LOAN_TYPE_EMI_HOME,
                    new String[]{"emi"}, null, null},
            {"Loan Type Outstanding Personal", VoiceQueries.English.LOAN_TYPE_OUTSTANDING_PERSONAL,
                    null, BotResponsePatterns.Loans.OUTSTANDING, null},
            {"Loan Details",                VoiceQueries.English.LOAN_DETAILS,
                    new String[]{"loan"}, null, "home loan"},
            {"Loan Type Details Education", VoiceQueries.English.LOAN_TYPE_DETAILS,
                    null, BotResponsePatterns.Loans.LOAN_OPTIONS_PROMPT, null},
        };
    }

    @Test(dataProvider = "sanityQueries", groups = {"ui", "sanity", "botverification"},
            description = "Sanity: should process one loan voice query per major category")
    public void testSanityQuery(String queryName, String query, String[] expectedKeywords,
                                 String assertionPattern, String disambiguationAccount) throws Exception {
        runVoiceQuery(queryName, query, expectedKeywords, assertionPattern, disambiguationAccount);
    }
}
