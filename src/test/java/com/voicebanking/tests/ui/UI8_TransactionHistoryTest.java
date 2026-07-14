package com.voicebanking.tests.ui;

import com.voicebanking.DataText.BotResponsePatterns;
import com.voicebanking.DataText.VoiceQueries;
import com.voicebanking.tests.ui.base.BaseVoiceTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Most queries assert against BotResponsePatterns.Transactions.ENTRY — the real transaction-card
 * format confirmed via manual testing. Category-filtered queries (UPI, card, all-credit) may
 * instead return a prose summary of the same data, so those use ENTRY_OR_SUMMARY. Queries whose
 * seed data may legitimately have no matches (ATM, today, yesterday, made-today, groceries) use
 * ENTRY_OR_NO_RESULTS to accept a documented "nothing found" response as a pass.
 */
public class UI8_TransactionHistoryTest extends BaseVoiceTest {

    /** Customer A (Sneha Kulkarni, CIF202602260010) — has real seeded transaction history,
     * unlike the random new-user registration BaseVoiceTest defaults to. A brand-new account
     * has nothing for a transaction query to return. */
    @Override
    protected String getLoginPhoneNumber() {
        return "9765432109";
    }

    @DataProvider(name = "voiceQueries")
    public Object[][] voiceQueries() {
        return new Object[][]{

            {"Recent Transactions",             VoiceQueries.English.RECENT_TRANSACTIONS,
                    new String[]{"transaction", "recent"},  BotResponsePatterns.Transactions.ENTRY, "savings"},
            {"Want Recent Transactions",        VoiceQueries.English.WANT_RECENT_TRANSACTIONS,
                    new String[]{"transaction", "recent"},  BotResponsePatterns.Transactions.ENTRY, "current"},
            {"Can See Recent Transactions",     VoiceQueries.English.CAN_SEE_RECENT_TRANSACTIONS,
                    new String[]{"transaction", "recent"},  BotResponsePatterns.Transactions.ENTRY, "savings"},
            {"Latest Transaction",              VoiceQueries.English.LATEST_TRANSACTION,
                    new String[]{"transaction", "latest"},  BotResponsePatterns.Transactions.ENTRY, "current"},
            {"Last Transaction",                VoiceQueries.English.LAST_TRANSACTION,
                    new String[]{"transaction", "last"},    BotResponsePatterns.Transactions.ENTRY, "savings"},
            {"What Was Last Transaction",       VoiceQueries.English.WHAT_WAS_LAST_TRANSACTION,
                    new String[]{"transaction", "last"},    BotResponsePatterns.Transactions.ENTRY, "current"},
            {"Todays Transactions",             VoiceQueries.English.TODAYS_TRANSACTIONS,
                    new String[]{"transaction", "today"},   BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "savings"},
            {"Yesterdays Transactions",         VoiceQueries.English.YESTERDAYS_TRANSACTIONS,
                    new String[]{"transaction", "yesterday"}, BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "current"},
            {"This Week Transactions",          VoiceQueries.English.THIS_WEEK_TRANSACTIONS,
                    new String[]{"transaction", "week"},    BotResponsePatterns.Transactions.ENTRY, "savings"},
            {"Last Week Transactions",          VoiceQueries.English.LAST_WEEK_TRANSACTIONS,
                    new String[]{"transaction", "week"},    BotResponsePatterns.Transactions.ENTRY, "current"},
            {"This Month Transactions",         VoiceQueries.English.THIS_MONTH_TRANSACTIONS,
                    new String[]{"transaction", "month"},   BotResponsePatterns.Transactions.ENTRY, "savings"},
            {"Last Month Transactions",         VoiceQueries.English.LAST_MONTH_TRANSACTIONS,
                    new String[]{"transaction", "month"},   BotResponsePatterns.Transactions.ENTRY, "current"},
            {"This Year Transactions",          VoiceQueries.English.THIS_YEAR_TRANSACTIONS,
                    new String[]{"transaction", "year"},    BotResponsePatterns.Transactions.ENTRY, "savings"},
            {"Last Year Transactions",          VoiceQueries.English.LAST_YEAR_TRANSACTIONS,
                    new String[]{"transaction", "year"},    BotResponsePatterns.Transactions.ENTRY, "current"},
            {"Transactions June Range",         VoiceQueries.English.TRANSACTIONS_JUNE_RANGE,
                    new String[]{"transaction", "june"},    BotResponsePatterns.Transactions.ENTRY, "savings"},
            {"Transactions July Range",         VoiceQueries.English.TRANSACTIONS_JULY_RANGE,
                    new String[]{"transaction", "july"},    BotResponsePatterns.Transactions.ENTRY, "current"},
            {"Transactions After Date",         VoiceQueries.English.TRANSACTIONS_AFTER_DATE,
                    new String[]{"transaction", "amount"},  BotResponsePatterns.Transactions.ENTRY, "savings"},
            {"Transactions Before Date",        VoiceQueries.English.TRANSACTIONS_BEFORE_DATE,
                    new String[]{"transaction", "amount"},  BotResponsePatterns.Transactions.ENTRY, "current"},
            {"Spend On Groceries",              VoiceQueries.English.SPEND_ON_GROCERIES,
                    new String[]{"spent", "groceries"},     BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "savings"},
            {"All Debit Transactions",          VoiceQueries.English.ALL_DEBIT_TRANSACTIONS,
                    new String[]{"debit", "transaction"},   BotResponsePatterns.Transactions.ENTRY, "current"},
            {"All Credit Transactions",         VoiceQueries.English.ALL_CREDIT_TRANSACTIONS,
                    new String[]{"credit", "transaction"},  BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY, "savings"},
            {"Money Credited",                  VoiceQueries.English.MONEY_CREDITED,
                    new String[]{"credited", "transaction"}, BotResponsePatterns.Transactions.ENTRY, "current"},
            {"Money Debited",                   VoiceQueries.English.MONEY_DEBITED,
                    new String[]{"debited", "transaction"}, BotResponsePatterns.Transactions.ENTRY, "savings"},
            {"Transactions Above Amount",       VoiceQueries.English.TRANSACTIONS_ABOVE_AMOUNT,
                    new String[]{"transaction", "amount"},  BotResponsePatterns.Transactions.ENTRY, "current"},
            {"Transactions Below Amount",       VoiceQueries.English.TRANSACTIONS_BELOW_AMOUNT,
                    new String[]{"transaction", "amount"},  BotResponsePatterns.Transactions.ENTRY, "savings"},
            {"Savings Transactions",            VoiceQueries.English.SAVINGS_TRANSACTIONS,
                    new String[]{"transaction", "savings"}, BotResponsePatterns.Transactions.ENTRY, null},
            {"Current Transactions",            VoiceQueries.English.CURRENT_TRANSACTIONS,
                    new String[]{"transaction", "current"}, BotResponsePatterns.Transactions.ENTRY, null},
            {"UPI Transactions",                VoiceQueries.English.UPI_TRANSACTIONS,
                    new String[]{"transaction", "upi"},     BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY, "savings"},
            {"ATM Transactions",                VoiceQueries.English.ATM_TRANSACTIONS,
                    new String[]{"transaction", "atm"},     BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "current"},
            {"Card Transactions",               VoiceQueries.English.CARD_TRANSACTIONS,
                    new String[]{"transaction", "card"},    BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY, "savings"},
            {"Transactions To Merchant",        VoiceQueries.English.TRANSACTIONS_TO_MERCHANT,
                    new String[]{"transaction", "amazon"},  BotResponsePatterns.Transactions.ENTRY, "current"},
            {"Transactions From Person",        VoiceQueries.English.TRANSACTIONS_FROM_PERSON,
                    new String[]{"transaction", "rohit"},   BotResponsePatterns.Transactions.ENTRY, "savings"},
            {"Transactions Made Today",         VoiceQueries.English.TRANSACTIONS_MADE_TODAY,
                    new String[]{"transaction", "today"},   BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "current"},
            {"Spend This Month",                VoiceQueries.English.SPEND_THIS_MONTH,
                    new String[]{"spent", "month"},         BotResponsePatterns.Transactions.ENTRY, "savings"},
            {"Transaction History",             VoiceQueries.English.TRANSACTION_HISTORY,
                    new String[]{"transaction", "history"}, BotResponsePatterns.Transactions.ENTRY, "current"},
            {"Read Out Recent Transactions",    VoiceQueries.English.READ_OUT_RECENT_TRANSACTIONS,
                    new String[]{"transaction", "recent"},  BotResponsePatterns.Transactions.ENTRY, "savings"},
        };
    }

    @Test(dataProvider = "voiceQueries", groups = {"ui", "regression", "botverification"},
            description = "Should process English transaction-history voice query and verify bot response")
    public void testVoiceQuery(String queryName, String query, String[] expectedKeywords,
                                String assertionPattern, String disambiguationAccount) throws Exception {
        runVoiceQuery(queryName, query, expectedKeywords, assertionPattern, disambiguationAccount);
    }

    /** One representative happy-path query — "is the bot alive and answering transaction
     * queries correctly at all." Run this tier for a fast build/deploy health check. */
    @DataProvider(name = "smokeQueries")
    public Object[][] smokeQueries() {
        return new Object[][]{
            {"Recent Transactions", VoiceQueries.English.RECENT_TRANSACTIONS,
                    new String[]{"transaction", "recent"}, BotResponsePatterns.Transactions.ENTRY, "savings"},
        };
    }

    @Test(dataProvider = "smokeQueries", groups = {"ui", "smoke", "botverification"},
            description = "Smoke: should process a basic English transaction-history voice query")
    public void testSmokeQuery(String queryName, String query, String[] expectedKeywords,
                                String assertionPattern, String disambiguationAccount) throws Exception {
        runVoiceQuery(queryName, query, expectedKeywords, assertionPattern, disambiguationAccount);
    }

    /** One query per major transaction-pattern category (generic recent, account-specific, and
     * category-filtered) — broader than smoke, still fast. */
    @DataProvider(name = "sanityQueries")
    public Object[][] sanityQueries() {
        return new Object[][]{
            {"Recent Transactions",   VoiceQueries.English.RECENT_TRANSACTIONS,
                    new String[]{"transaction", "recent"},  BotResponsePatterns.Transactions.ENTRY, "savings"},
            {"Savings Transactions",  VoiceQueries.English.SAVINGS_TRANSACTIONS,
                    new String[]{"transaction", "savings"}, BotResponsePatterns.Transactions.ENTRY, null},
            {"UPI Transactions",      VoiceQueries.English.UPI_TRANSACTIONS,
                    new String[]{"transaction", "upi"},     BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY, "savings"},
        };
    }

    @Test(dataProvider = "sanityQueries", groups = {"ui", "sanity", "botverification"},
            description = "Sanity: should process one transaction voice query per major pattern category")
    public void testSanityQuery(String queryName, String query, String[] expectedKeywords,
                                 String assertionPattern, String disambiguationAccount) throws Exception {
        runVoiceQuery(queryName, query, expectedKeywords, assertionPattern, disambiguationAccount);
    }

    /** Customer A (Sneha Kulkarni, CIF202602260010, 9765432109) — dual account: savings + current.
     * Customer B (CIF202602260007, 9723456789) — savings only. Account-to-customer mapping is known
     * statically, so each row names its account explicitly (SAVINGS_TRANSACTIONS / CURRENT_TRANSACTIONS)
     * and never triggers the disambiguation follow-up — no need to guess or loop over possibilities. */
    @DataProvider(name = "knownAccountTransactionQueries")
    public Object[][] knownAccountTransactionQueries() {
        return new Object[][]{

            // {queryName, query, expectedKeywords, assertionPattern, disambiguationAccount, phoneNumber}
            {"Customer A Savings Transactions", VoiceQueries.English.SAVINGS_TRANSACTIONS,
                    new String[]{"transaction", "savings"}, BotResponsePatterns.Transactions.ENTRY, null, "9765432109"},
            {"Customer A Current Transactions", VoiceQueries.English.CURRENT_TRANSACTIONS,
                    new String[]{"transaction", "current"}, BotResponsePatterns.Transactions.ENTRY, null, "9765432109"},
            {"Customer B Savings Transactions", VoiceQueries.English.SAVINGS_TRANSACTIONS,
                    new String[]{"transaction", "savings"}, BotResponsePatterns.Transactions.ENTRY, null, "9723456789"},
        };
    }

    @Test(dataProvider = "knownAccountTransactionQueries", groups = {"ui", "regression", "botverification"},
            description = "Should return recent transactions for the correct account for known seeded customers")
    public void testKnownAccountTransactions(String queryName, String query, String[] expectedKeywords,
                                              String assertionPattern, String disambiguationAccount,
                                              String phoneNumber) throws Exception {
        runVoiceQuery(queryName, query, expectedKeywords, assertionPattern, disambiguationAccount, phoneNumber);
    }
}
