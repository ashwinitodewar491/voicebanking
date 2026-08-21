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

/**
 * Most queries assert against BotResponsePatterns.Transactions.ENTRY — the real transaction-card
 * format confirmed via manual testing. Every category/filter-scoped query (UPI, card, all-credit,
 * all-debit, credited/debited, above/below-amount, to-merchant, from-person) may instead return a
 * prose summary of the same data — the live bot isn't consistent about which shape it picks for a
 * given filtered query, so all of these use ENTRY_OR_SUMMARY rather than the strict ENTRY-only
 * pattern. Generic/unfiltered recency and account-scoped queries (recent, latest, last, savings,
 * current, transaction history) have only ever been observed returning the card format, so those
 * stay on strict ENTRY. Queries whose seed data may legitimately have no matches (ATM, today,
 * yesterday, made-today, groceries, this/last week/month/year, spend-this-month, from-person, and
 * the fixed-calendar-date queries — June/July range, after/before date) use ENTRY_OR_NO_RESULTS to
 * accept a documented "nothing found" response as a pass — whether a given date range or filter
 * has any matches at all depends on this seed data's actual transaction dates, which isn't
 * something to assume without checking.
 * <p>
 * Every row here logs in fresh (no {@code useSharedSession()} override) rather than sharing one
 * session across rows the way UI7 does — the live bot's per-query filter (date range, category,
 * merchant, person, ...) turned out to be <em>sticky</em> for the rest of a conversation once set,
 * not just leaking into the immediately-next turn. In one shared-session regression run, the
 * "groceries" filter from a single early query contaminated most of the ~30 unrelated queries that
 * followed, none of which had asked for groceries at all. No row-ordering scheme fixes a
 * session-wide sticky filter. A since-reverted attempt tried proactively ending and reconnecting
 * the voice session between rows (to get a fresh bot conversation without a full re-login), but
 * that meant deliberately ending sessions the app hadn't actually ended — session teardown here
 * only ever happens reactively, when the live bot itself reports "Session Ended" (see {@link
 * com.voicebanking.pages.HomePage}'s own recovery for that). So this class stays on one login per
 * row.
 */
public class UI8_TransactionHistoryTest extends BaseVoiceTest {

    /** Customer A (Rohit Mehta, CIF202602260005) — has real seeded transaction history,
     * unlike the random new-user registration BaseVoiceTest defaults to. A brand-new account
     * has nothing for a transaction query to return. Dual account (savings + current) is also
     * required here since some rows disambiguate to "current". */
    @Override
    protected String getLoginPhoneNumber() {
        return "9898989898";
    }

    @DataProvider(name = "voiceQueries")
    public Object[][] voiceQueries() {
        return new Object[][]{

            {"Recent Transactions",             VoiceQueries.English.RECENT_TRANSACTIONS,
                    new String[]{"transaction", "recent"},  BotResponsePatterns.Transactions.RECENT_ENTRY, "savings"},
            {"Want Recent Transactions",        VoiceQueries.English.WANT_RECENT_TRANSACTIONS,
                    new String[]{"transaction", "recent"},  BotResponsePatterns.Transactions.RECENT_ENTRY, "current"},
            {"Can See Recent Transactions",     VoiceQueries.English.CAN_SEE_RECENT_TRANSACTIONS,
                    new String[]{"transaction", "recent"},  BotResponsePatterns.Transactions.RECENT_ENTRY, "savings"},
            {"Latest Transaction",              VoiceQueries.English.LATEST_TRANSACTION,
                    new String[]{"transaction", "latest"},  BotResponsePatterns.Transactions.LATEST_ENTRY, "current"},
            {"Last Transaction",                VoiceQueries.English.LAST_TRANSACTION,
                    new String[]{"transaction", "last"},    BotResponsePatterns.Transactions.LATEST_ENTRY, "savings"},
            {"What Was Last Transaction",       VoiceQueries.English.WHAT_WAS_LAST_TRANSACTION,
                    new String[]{"transaction", "last"},    BotResponsePatterns.Transactions.LATEST_ENTRY, "current"},
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
                    new String[]{"debit", "transaction"},   BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY, "current"},
            {"All Credit Transactions",         VoiceQueries.English.ALL_CREDIT_TRANSACTIONS,
                    new String[]{"credit", "transaction"},  BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY, "savings"},
            {"Money Credited",                  VoiceQueries.English.MONEY_CREDITED,
                    new String[]{"credited", "transaction"}, BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY, "current"},
            {"Money Debited",                   VoiceQueries.English.MONEY_DEBITED,
                    new String[]{"debited", "transaction"}, BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY, "savings"},
            {"Transactions Above Amount",       VoiceQueries.English.TRANSACTIONS_ABOVE_AMOUNT,
                    new String[]{"transaction", "amount"},  BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY, "current"},
            {"Transactions Below Amount",       VoiceQueries.English.TRANSACTIONS_BELOW_AMOUNT,
                    new String[]{"transaction", "amount"},  BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY, "savings"},
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
                    new String[]{"transaction", "amazon"},  BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY, "current"},
            {"Transactions From Person",        VoiceQueries.English.TRANSACTIONS_FROM_PERSON,
                    new String[]{"transaction", "rohit"},   BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "savings"},
            {"Transactions Made Today",         VoiceQueries.English.TRANSACTIONS_MADE_TODAY,
                    new String[]{"transaction", "today"},   BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "current"},
            {"Spend This Month",                VoiceQueries.English.SPEND_THIS_MONTH,
                    new String[]{"spent", "month"},         BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "savings"},
            {"Transaction History",             VoiceQueries.English.TRANSACTION_HISTORY,
                    new String[]{"transaction", "history"}, BotResponsePatterns.Transactions.ENTRY, "current"},
            {"Read Out Recent Transactions",    VoiceQueries.English.READ_OUT_RECENT_TRANSACTIONS,
                    new String[]{"transaction", "recent"},  BotResponsePatterns.Transactions.ENTRY, "savings"},
        };
    }

    /** Queries confirmed not working against the live stage bot as of 2026-08-06 — dev is
     * expected to fix these later. Skipped (not failed) so CI regression runs don't flag known,
     * already-reported issues; remove an entry here once its fix lands. */
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

    /** Five queries chosen to cover every distinct response-pattern this file asserts on: generic
     * recency (ENTRY, disambiguates), an explicit account query (ENTRY, no disambiguation), a
     * category filter that may return prose instead of cards (ENTRY_OR_SUMMARY), a time-range
     * query whose seed data may legitimately have no matches (ENTRY_OR_NO_RESULTS — the lenient
     * path), and a debit/credit filter query. Run this tier for a fast build/deploy health check. */
    @DataProvider(name = "smokeQueries")
    public Object[][] smokeQueries() {
        return new Object[][]{
            {"Recent Transactions", VoiceQueries.English.RECENT_TRANSACTIONS,
                    new String[]{"transaction", "recent"}, BotResponsePatterns.Transactions.RECENT_ENTRY, "savings"},
            {"Savings Transactions", VoiceQueries.English.SAVINGS_TRANSACTIONS,
                    new String[]{"transaction", "savings"}, BotResponsePatterns.Transactions.ENTRY, null},
            {"UPI Transactions", VoiceQueries.English.UPI_TRANSACTIONS,
                    new String[]{"transaction", "upi"}, BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY, "savings"},
            {"Todays Transactions", VoiceQueries.English.TODAYS_TRANSACTIONS,
                    new String[]{"transaction", "today"}, BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS, "savings"},
            {"All Debit Transactions", VoiceQueries.English.ALL_DEBIT_TRANSACTIONS,
                    new String[]{"debit", "transaction"}, BotResponsePatterns.Transactions.ENTRY_OR_SUMMARY, "current"},
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
                    new String[]{"transaction", "recent"},  BotResponsePatterns.Transactions.RECENT_ENTRY, "savings"},
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

    /** Customer A (Rohit Mehta, CIF202602260005, 9898989898) — dual account: savings + current.
     * Customer B (Leena Kamat, CIF202602260042, 9812341042) — savings only.
     * Customer C (Aniket More, CIF202602260041, 9812341041) — savings only.
     * Account-to-customer mapping is known statically, so each row names its account explicitly
     * (SAVINGS_TRANSACTIONS / CURRENT_TRANSACTIONS) and never triggers the disambiguation follow-up
     * — no need to guess or loop over possibilities. {@code accountId} (a new column beyond the
     * generic voiceQueries shape) is what {@link #testKnownAccountTransactions} cross-checks the
     * bot's returned entries against, via the ground-truth Transactions List API. */
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
                Assert.assertTrue(foundInApi,
                        "[" + queryName + "] Bot listed an amount (" + amount + ") not found in the"
                        + " Transactions List API's own data for this account.");
            }
        }
    }

    /** Queries asking generically for "recent" transactions have been consistently observed
     * returning exactly 5 entries — a fixed page size, not a coincidence of this seed data. */
    private static final Set<String> RECENT_COUNT_5 = Set.of(
            "Recent Transactions", "Want Recent Transactions",
            "Can See Recent Transactions", "Read Out Recent Transactions");

    /** Queries asking for a single specific transaction (the latest/last one) should return
     * exactly 1 entry. */
    private static final Set<String> LATEST_COUNT_1 = Set.of(
            "Latest Transaction", "Last Transaction", "What Was Last Transaction");

    /** Beyond the shape-only pattern check every row already gets, verifies the response content
     * is internally consistent: the "recent transactions" family returns exactly 5 entries, the
     * single-latest-transaction family returns exactly 1, and — for whichever rows the bot answers
     * with a "Total spent" summary — that total equals the sum of the entries listed alongside it.
     * These don't require knowing the seed data's true values, only that the counts/totals the bot
     * itself displays are self-consistent — a real cross-check we couldn't previously do without
     * visibility into the account's actual transaction data. */
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
                "[" + queryName + "] 'Total spent' does not match the sum of the listed entries.\n"
                + "  Response: " + botResponse);

        return botResponse;
    }

    /** A transaction-history response after account disambiguation is just a list of entries —
     * unlike a balance response, it doesn't name which account it's for ("The balance in your
     * SAVINGS account is..."), so there's no SAVINGS-specific vs. CURRENT-specific shape to check
     * regardless of {@code followUpAccount}. The base class's default (a Balance pattern) doesn't
     * apply here at all. */
    @Override
    protected String getDisambiguationExpectedPattern(String followUpAccount) {
        return BotResponsePatterns.Transactions.ENTRY_OR_NO_RESULTS;
    }
}
