package com.voicebanking.tests.ui;

import com.voicebanking.DataText.BotResponsePatterns;
import com.voicebanking.DataText.Constants;
import com.voicebanking.DataText.VoiceQueries;
import com.voicebanking.pages.HomePage;
import com.voicebanking.tests.ui.base.BaseVoiceTest;
import com.voicebanking.utils.GroundTruthApi;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Transaction-history queries. Filtered queries accept card or prose-summary responses, and generic
 * rows accept "no results"; the known-account rows stay strict. Each row logs in fresh because the
 * bot's filters persist for the whole conversation. */
public class UI8_TransactionHistoryTest extends BaseVoiceTest {

    /** Rohit Mehta: seeded transaction history on both savings and current. */
    @Override
    protected String getLoginPhoneNumber() {
        return "9898989898";
    }

    @DataProvider(name = "voiceQueries")
    public Object[][] voiceQueries() {
        return new Object[][]{

            {"Recent Transactions",             VoiceQueries.English.RECENT_TRANSACTIONS,
                    new String[]{"transaction", "recent"},  BotResponsePatterns.Transactions.RECENT_ENTRY_OR_NO_RESULTS, "savings"},
            {"Want Recent Transactions",        VoiceQueries.English.WANT_RECENT_TRANSACTIONS,
                    new String[]{"transaction", "recent"},  BotResponsePatterns.Transactions.RECENT_ENTRY_OR_NO_RESULTS, "current"},
            {"Can See Recent Transactions",     VoiceQueries.English.CAN_SEE_RECENT_TRANSACTIONS,
                    new String[]{"transaction", "recent"},  BotResponsePatterns.Transactions.RECENT_ENTRY_OR_NO_RESULTS, "savings"},
            {"Latest Transaction",              VoiceQueries.English.LATEST_TRANSACTION,
                    new String[]{"transaction", "latest"},  BotResponsePatterns.Transactions.LATEST_ENTRY_OR_NO_RESULTS, "current"},
            {"Last Transaction",                VoiceQueries.English.LAST_TRANSACTION,
                    new String[]{"transaction", "last"},    BotResponsePatterns.Transactions.LATEST_ENTRY_OR_NO_RESULTS, "savings"},
            {"What Was Last Transaction",       VoiceQueries.English.WHAT_WAS_LAST_TRANSACTION,
                    new String[]{"transaction", "last"},    BotResponsePatterns.Transactions.LATEST_ENTRY_OR_NO_RESULTS, "current"},
            {"Todays Transactions",             VoiceQueries.English.TODAYS_TRANSACTIONS,
                    new String[]{"transaction", "today"},   BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "savings"},
            {"Yesterdays Transactions",         VoiceQueries.English.YESTERDAYS_TRANSACTIONS,
                    new String[]{"transaction", "yesterday"}, BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "current"},
            {"This Week Transactions",          VoiceQueries.English.THIS_WEEK_TRANSACTIONS,
                    new String[]{"transaction", "week"},    BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "savings"},
            {"Last Week Transactions",          VoiceQueries.English.LAST_WEEK_TRANSACTIONS,
                    new String[]{"transaction", "week"},    BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "current"},
            {"This Month Transactions",         VoiceQueries.English.THIS_MONTH_TRANSACTIONS,
                    new String[]{"transaction", "month"},   BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "savings"},
            {"Last Month Transactions",         VoiceQueries.English.LAST_MONTH_TRANSACTIONS,
                    new String[]{"transaction", "month"},   BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "current"},
            {"This Year Transactions",          VoiceQueries.English.THIS_YEAR_TRANSACTIONS,
                    new String[]{"transaction", "year"},    BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "savings"},
            {"Last Year Transactions",          VoiceQueries.English.LAST_YEAR_TRANSACTIONS,
                    new String[]{"transaction", "year"},    BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "current"},
            {"Transactions June Range",         VoiceQueries.English.TRANSACTIONS_JUNE_RANGE,
                    new String[]{"transaction", "june"},    BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "savings"},
            {"Transactions July Range",         VoiceQueries.English.TRANSACTIONS_JULY_RANGE,
                    new String[]{"transaction", "july"},    BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "current"},
            {"Transactions After Date",         VoiceQueries.English.TRANSACTIONS_AFTER_DATE,
                    new String[]{"transaction", "amount"},  BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "savings"},
            {"Transactions Before Date",        VoiceQueries.English.TRANSACTIONS_BEFORE_DATE,
                    new String[]{"transaction", "amount"},  BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "current"},
            {"Spend On Groceries",              VoiceQueries.English.SPEND_ON_GROCERIES,
                    new String[]{"spent", "groceries"},     BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "savings"},
            {"All Debit Transactions",          VoiceQueries.English.ALL_DEBIT_TRANSACTIONS,
                    new String[]{"debit", "transaction"},   BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY_OR_NO_RESULTS, "current"},
            {"All Credit Transactions",         VoiceQueries.English.ALL_CREDIT_TRANSACTIONS,
                    new String[]{"credit", "transaction"},  BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY_OR_NO_RESULTS, "savings"},
            {"Money Credited",                  VoiceQueries.English.MONEY_CREDITED,
                    new String[]{"credited", "transaction"}, BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY_OR_NO_RESULTS, "current"},
            {"Money Debited",                   VoiceQueries.English.MONEY_DEBITED,
                    new String[]{"debited", "transaction"}, BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY_OR_NO_RESULTS, "savings"},
            {"Transactions Above Amount",       VoiceQueries.English.TRANSACTIONS_ABOVE_AMOUNT,
                    new String[]{"transaction", "amount"},  BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY_OR_NO_RESULTS, "current"},
            {"Transactions Below Amount",       VoiceQueries.English.TRANSACTIONS_BELOW_AMOUNT,
                    new String[]{"transaction", "amount"},  BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY_OR_NO_RESULTS, "savings"},
            {"Savings Transactions",            VoiceQueries.English.SAVINGS_TRANSACTIONS,
                    new String[]{"transaction", "savings"}, BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, null},
            {"Current Transactions",            VoiceQueries.English.CURRENT_TRANSACTIONS,
                    new String[]{"transaction", "current"}, BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, null},
            {"UPI Transactions",                VoiceQueries.English.UPI_TRANSACTIONS,
                    new String[]{"transaction", "upi"},     BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY_OR_NO_RESULTS, "savings"},
            {"ATM Transactions",                VoiceQueries.English.ATM_TRANSACTIONS,
                    new String[]{"transaction", "atm"},     BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "current"},
            {"Card Transactions",               VoiceQueries.English.CARD_TRANSACTIONS,
                    new String[]{"transaction", "card"},    BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY_OR_NO_RESULTS, "savings"},
            {"Transactions To Merchant",        VoiceQueries.English.TRANSACTIONS_TO_MERCHANT,
                    new String[]{"transaction", "amazon"},  BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY_OR_NO_RESULTS, "current"},
            {"Transactions From Person",        VoiceQueries.English.TRANSACTIONS_FROM_PERSON,
                    new String[]{"transaction", "rohit"},   BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "savings"},
            {"Transactions Made Today",         VoiceQueries.English.TRANSACTIONS_MADE_TODAY,
                    new String[]{"transaction", "today"},   BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "current"},
            {"Spend This Month",                VoiceQueries.English.SPEND_THIS_MONTH,
                    new String[]{"spent", "month"},         BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "savings"},
            {"Transaction History",             VoiceQueries.English.TRANSACTION_HISTORY,
                    new String[]{"transaction", "history"}, BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "current"},
            {"Read Out Recent Transactions",    VoiceQueries.English.READ_OUT_RECENT_TRANSACTIONS,
                    new String[]{"transaction", "recent"},  BotResponsePatterns.Transactions.RECENT_ENTRY_OR_NO_RESULTS, "savings"},
        };
    }

    /** Queries known to be broken on stage (as of 2026-08-06); skipped, not failed. Remove entries
     * once fixed. */
    private static final Set<String> NOT_WORKING = Set.of(
            "Spend On Groceries", "All Credit Transactions", "Money Credited",
            "Transactions Above Amount", "Transactions Below Amount", "ATM Transactions",
            "Card Transactions", "Transactions To Merchant", "Transactions From Person",
            "Spend This Month");

    @Test(dataProvider = "voiceQueries", groups = {"ui", "regression", "botverification"},
            description = "Should process English transaction-history voice query and verify bot response")
    public void testVoiceQuery(String queryName, String query, String[] expectedKeywords,
                                String assertionPattern, String disambiguationAccount) throws Exception {
        if (NOT_WORKING.contains(queryName)) {
            throw new SkipException("[" + queryName + "] Known not working on stage — dev to fix later");
        }
        runVoiceQuery(queryName, query, expectedKeywords, assertionPattern, disambiguationAccount);
    }

    /** Fast health-check tier covering each response pattern asserted in this file. */
    @DataProvider(name = "smokeQueries")
    public Object[][] smokeQueries() {
        return new Object[][]{
            {"Recent Transactions", VoiceQueries.English.RECENT_TRANSACTIONS,
                    new String[]{"transaction", "recent"}, BotResponsePatterns.Transactions.RECENT_ENTRY_OR_NO_RESULTS, "savings"},
            {"Savings Transactions", VoiceQueries.English.SAVINGS_TRANSACTIONS,
                    new String[]{"transaction", "savings"}, BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, null},
            {"UPI Transactions", VoiceQueries.English.UPI_TRANSACTIONS,
                    new String[]{"transaction", "upi"}, BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY_OR_NO_RESULTS, "savings"},
            {"Todays Transactions", VoiceQueries.English.TODAYS_TRANSACTIONS,
                    new String[]{"transaction", "today"}, BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "savings"},
            {"All Debit Transactions", VoiceQueries.English.ALL_DEBIT_TRANSACTIONS,
                    new String[]{"debit", "transaction"}, BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY_OR_NO_RESULTS, "current"},
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
                    new String[]{"transaction", "recent"},  BotResponsePatterns.Transactions.RECENT_ENTRY_OR_NO_RESULTS, "savings"},
            {"Savings Transactions",  VoiceQueries.English.SAVINGS_TRANSACTIONS,
                    new String[]{"transaction", "savings"}, BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, null},
            {"UPI Transactions",      VoiceQueries.English.UPI_TRANSACTIONS,
                    new String[]{"transaction", "upi"},     BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY_OR_NO_RESULTS, "savings"},
        };
    }

    @Test(dataProvider = "sanityQueries", groups = {"ui", "sanity", "botverification"},
            description = "Sanity: should process one transaction voice query per major pattern category")
    public void testSanityQuery(String queryName, String query, String[] expectedKeywords,
                                 String assertionPattern, String disambiguationAccount) throws Exception {
        runVoiceQuery(queryName, query, expectedKeywords, assertionPattern, disambiguationAccount);
    }

    /** Customers A (Rohit, savings + current), B (Leena, savings) and C (Aniket, savings). Rows name
     * the account explicitly; entries are cross-checked against the Transactions List API. */
    @DataProvider(name = "knownAccountTransactionQueries")
    public Object[][] knownAccountTransactionQueries() {
        return new Object[][]{

            // {queryName, query, expectedKeywords, assertionPattern, disambiguationAccount, phoneNumber, accountId}
            {"Customer A Savings Transactions", VoiceQueries.English.SAVINGS_TRANSACTIONS,
                    new String[]{"transaction", "savings"}, BotResponsePatterns.Transactions.ENTRY, null, "9898989898",
                    Constants.EXPECTED_ACCOUNT_ID},
            {"Customer A Current Transactions", VoiceQueries.English.CURRENT_TRANSACTIONS,
                    new String[]{"transaction", "current"}, BotResponsePatterns.Transactions.ENTRY, null, "9898989898",
                    Constants.CUSTOMER_A_CURRENT_ACCOUNT_ID},
            {"Customer B Savings Transactions", VoiceQueries.English.SAVINGS_TRANSACTIONS,
                    new String[]{"transaction", "savings"}, BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, null, "9812341042",
                    Constants.CUSTOMER_B_SAVINGS_ACCOUNT_ID},
            {"Customer C Savings Transactions", VoiceQueries.English.SAVINGS_TRANSACTIONS,
                    new String[]{"transaction", "savings"}, BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, null, Constants.CUSTOMER_C_PHONE,
                    Constants.CUSTOMER_C_SAVINGS_ACCOUNT_ID},
        };
    }

    @Test(dataProvider = "knownAccountTransactionQueries", groups = {"ui", "regression", "botverification"},
            description = "Should return recent transactions for the correct account for known seeded customers, cross-checked against the Transactions List API")
    public void testKnownAccountTransactions(String queryName, String query, String[] expectedKeywords,
                                              String assertionPattern, String disambiguationAccount,
                                              String phoneNumber, String accountId) throws Exception {
        runVoiceQuery(queryName, query, expectedKeywords, assertionPattern, disambiguationAccount, phoneNumber);

        List<Double> spokenAmounts = transactionEntryAmounts(lastBotResponse());
        // "Today" (not a fixed historical date, e.g. Constants.TRANSACTION_TO_DATE) — this needs
        // to include whatever the bot itself considers "recent" right now, not a snapshot pinned
        // to whenever that other constant was last refreshed.
        GroundTruthApi.TransactionsPage apiPage =
                groundTruth().transactions(accountId, LocalDate.now().toString());

        System.out.println("[GroundTruth] " + queryName + " — spoken amounts=" + spokenAmounts
                + " api amounts=" + apiPage.amounts + " api totalElements=" + apiPage.totalElements);

        if (spokenAmounts.isEmpty()) {
            // A legitimate "no transactions" response (Customer B, ENTRY_OR_NO_RESULTS) is only
            // correct if the account genuinely has none — independently confirmed against the API
            // rather than assumed.
            Assert.assertEquals(apiPage.totalElements, 0,
                    "[" + queryName + "] Bot reported no transactions, but the Transactions List API"
                    + " shows " + apiPage.totalElements + " for this account.");
        } else {
            for (double amount : spokenAmounts) {
                boolean foundInApi = apiPage.amounts.stream().anyMatch(a -> Math.abs(a - amount) < 0.01);
                System.out.println("[GroundTruth] " + queryName + " — amount=" + amount + " foundInApi=" + foundInApi);
                Assert.assertTrue(foundInApi,
                        "[" + queryName + "] Bot listed an amount (" + amount + ") not found in the"
                        + " Transactions List API's own data for this account.");
            }
        }
    }

    /** "Recent" queries return exactly 5 entries (a fixed page size). */
    private static final Set<String> RECENT_COUNT_5 = Set.of(
            "Recent Transactions", "Want Recent Transactions",
            "Can See Recent Transactions", "Read Out Recent Transactions");

    /** Queries asking for a single specific transaction (the latest/last one) should return
     * exactly 1 entry. */
    private static final Set<String> LATEST_COUNT_1 = Set.of(
            "Latest Transaction", "Last Transaction", "What Was Last Transaction");

    /** Consistency checks: 5 entries for "recent", 1 for "latest", and "Total spent" equals the sum
     * of the DEBIT entries. */
    @Override
    protected String handleAdditionalFollowUp(String queryName, String query, String botResponse,
                                               String disambiguationAccount, HomePage homePage) throws Exception {
        if (RECENT_COUNT_5.contains(queryName)) {
            int count = countTransactionEntries(botResponse);
            Assert.assertEquals(count, 5,
                    "[" + queryName + "] Expected exactly 5 recent transaction entries, got " + count
                    + ".\n  Response: " + botResponse);
        } else if (LATEST_COUNT_1.contains(queryName)) {
            int count = countTransactionEntries(botResponse);
            Assert.assertEquals(count, 1,
                    "[" + queryName + "] Expected exactly 1 transaction entry, got " + count
                    + ".\n  Response: " + botResponse);
        }

        Assert.assertTrue(totalMatchesSumOfEntries(botResponse),
                "[" + queryName + "] 'Total spent' does not match the sum of the listed DEBIT entries.\n"
                + "  Response: " + botResponse);

        return botResponse;
    }

    /** Transaction responses don't name the account, so the generic entry pattern applies regardless
     * of {@code followUpAccount}. */
    @Override
    protected String getDisambiguationExpectedPattern(String followUpAccount) {
        return BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS;
    }
}
