package com.voicebanking.tests.ui;

import com.voicebanking.DataText.BotResponsePatterns;
import com.voicebanking.DataText.Constants;
import com.voicebanking.DataText.VoiceQueries;
import com.voicebanking.tests.ui.base.BaseVoiceTest;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.regex.Matcher;

public class UI7_BalanceInquiryTest extends BaseVoiceTest {

    /** Default identity for the generic voiceQueries/smokeQueries/sanityQueries rows — the known
     * dual-account (savings + current) customer, Rohit Mehta (CIF202602260005), confirmed to
     * resolve explicit CURRENT-balance queries correctly. Also used as CUSTOMER_A_PHONE below —
     * he's the only one of the three known accounts with both a savings and a current account. */
    private static final String BALANCE_INQUIRY_CUSTOMER_PHONE = "9898989898";

    /** Customer A (Rohit Mehta, CIF202602260005) — dual account: savings + current. Same identity
     * as BALANCE_INQUIRY_CUSTOMER_PHONE; kept as a separate named constant since
     * knownAccountBalanceQueries targets it explicitly by name. */
    private static final String CUSTOMER_A_PHONE = "9898989898";
    /** Customer B (Leena Kamat, CIF202602260042) — savings only, no loan, no beneficiary. */
    private static final String CUSTOMER_B_PHONE = "9812341042";

    /** All balance-inquiry rows are independent conversational turns against the same account —
     * safe to run in one continuous session instead of a fresh browser/login per row. Cuts this
     * class from ~55 logins (one per data-provider row) down to ~3: one shared login covering every
     * voiceQueries/smokeQueries/sanityQueries row, one switch to Customer A for
     * knownAccountBalanceQueries' two Customer-A rows, and one switch to Customer B for its one
     * Customer-B row. */
    @Override
    protected boolean useSharedSession() {
        return true;
    }

    /** Overrides the shared base's random-new-user default: uses a known seeded customer with BOTH
     * a savings and a current account instead of a freshly registered random customer. A random new
     * registration may be provisioned with only a savings account, which would fail every
     * CURRENT-balance assertion in this class regardless of session mode, since there'd be no
     * current account for the bot to report. */
    @Override
    protected String getLoginPhoneNumber() {
        return BALANCE_INQUIRY_CUSTOMER_PHONE;
    }

    @DataProvider(name = "voiceQueries")
    public Object[][] voiceQueries() {
        return new Object[][]{

            // {queryName, query, expectedKeywords, assertionPattern, disambiguationAccount}
            // disambiguationAccount is the account ("savings"/"current") spoken back when the bot
            // asks to disambiguate; null where the query already names an account explicitly.
            {"Account Balance",              VoiceQueries.English.ACCOUNT_BALANCE,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "savings"},
            {"Savings Balance",              VoiceQueries.English.SAVINGS_BALANCE,
                    new String[]{"balance", "savings"},  BotResponsePatterns.Balance.SAVINGS, null},
            {"How Much Money",               VoiceQueries.English.HOW_MUCH_MONEY,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "current"},
            {"Balance Query",                VoiceQueries.English.BALANCE_QUERY,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "current"},
            {"Balance Short",                VoiceQueries.English.BALANCE_SHORT,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "current"},
            {"Current Balance",              VoiceQueries.English.CURRENT_BALANCE,
                    new String[]{"balance", "current"},  BotResponsePatterns.Balance.CURRENT, null},
            {"Money In Account",             VoiceQueries.English.MONEY_IN_ACCOUNT,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "savings"},
            {"Show Account Balance",         VoiceQueries.English.SHOW_ACCOUNT_BALANCE,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "current"},
            {"Tell Account Balance",         VoiceQueries.English.TELL_ACCOUNT_BALANCE,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "savings"},
            {"Can Tell Balance",             VoiceQueries.English.CAN_TELL_BALANCE,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "current"},
            {"Please Check Balance",         VoiceQueries.English.PLEASE_CHECK_BALANCE,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "savings"},
            {"Check Account Balance",        VoiceQueries.English.CHECK_ACCOUNT_BALANCE,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "current"},
            {"Show My Balance",              VoiceQueries.English.SHOW_MY_BALANCE,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "savings"},
            {"Balance Available",            VoiceQueries.English.BALANCE_AVAILABLE,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "current"},
            {"Available Balance In Account", VoiceQueries.English.AVAILABLE_BALANCE_IN_ACCOUNT,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "savings"},
            {"Amount Left",                  VoiceQueries.English.AMOUNT_LEFT,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "current"},
            {"Cash In Account",              VoiceQueries.English.CASH_IN_ACCOUNT,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "savings"},
            {"Amount In Account",            VoiceQueries.English.AMOUNT_IN_ACCOUNT,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "current"},
            {"Savings Balance Tell",         VoiceQueries.English.SAVINGS_BALANCE_TELL,
                    new String[]{"balance", "savings"},  BotResponsePatterns.Balance.SAVINGS, null},
            {"Current Balance Tell",         VoiceQueries.English.CURRENT_BALANCE_TELL,
                    new String[]{"balance", "current"},  BotResponsePatterns.Balance.CURRENT, null},
            {"Savings Balance Of",           VoiceQueries.English.SAVINGS_BALANCE_OF,
                    new String[]{"balance", "savings"},  BotResponsePatterns.Balance.SAVINGS, null},
            {"Current Balance Of",           VoiceQueries.English.CURRENT_BALANCE_OF,
                    new String[]{"balance", "current"},  BotResponsePatterns.Balance.CURRENT, null},
            {"Enough Money",                 VoiceQueries.English.ENOUGH_MONEY,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "savings"},
            {"Can Know Balance",             VoiceQueries.English.CAN_KNOW_BALANCE,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "current"},
            {"Want To Know Balance",         VoiceQueries.English.WANT_TO_KNOW_BALANCE,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "savings"},
            {"Get Account Balance",          VoiceQueries.English.GET_ACCOUNT_BALANCE,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "current"},
            {"Read Out Balance",             VoiceQueries.English.READ_OUT_BALANCE,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "savings"},
            {"Access Account Balance",       VoiceQueries.English.ACCESS_ACCOUNT_BALANCE,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "current"},
            {"Fetch Account Balance",        VoiceQueries.English.FETCH_ACCOUNT_BALANCE,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "savings"},
            {"Balance In Account",           VoiceQueries.English.BALANCE_IN_ACCOUNT,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "current"},
            {"Account Balance Query",        VoiceQueries.English.ACCOUNT_BALANCE_QUERY,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "savings"},
            {"Savings Balance Query",        VoiceQueries.English.SAVINGS_BALANCE_QUERY,
                    new String[]{"balance", "savings"},  BotResponsePatterns.Balance.SAVINGS, null},
            {"Current Account Balance Query", VoiceQueries.English.CURRENT_ACCOUNT_BALANCE_QUERY,
                    new String[]{"balance", "current"},  BotResponsePatterns.Balance.CURRENT, null},
            {"My Balance",                   VoiceQueries.English.MY_BALANCE,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "current"},
            {"Available Balance",            VoiceQueries.English.AVAILABLE_BALANCE,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "savings"},
            {"Remaining Balance",            VoiceQueries.English.REMAINING_BALANCE,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "current"},
            {"Check Balance",                VoiceQueries.English.CHECK_BALANCE,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "savings"},
            {"Show Balance",                 VoiceQueries.English.SHOW_BALANCE,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "current"},
            {"Need Balance",                 VoiceQueries.English.NEED_BALANCE,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "savings"},
            {"Remaining Amount",             VoiceQueries.English.REMAINING_AMOUNT,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "current"},
            {"Funds Available",              VoiceQueries.English.FUNDS_AVAILABLE,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "savings"},
            {"Amount Available",             VoiceQueries.English.AMOUNT_AVAILABLE,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "current"},
            {"Balance Check",                VoiceQueries.English.BALANCE_CHECK,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "savings"},
            {"Account Balance Check",        VoiceQueries.English.ACCOUNT_BALANCE_CHECK,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY, "current"},
        };
    }

    @Test(dataProvider = "voiceQueries", groups = {"ui", "regression", "botverification"},
            description = "Should process English balance voice query and verify bot response")
    public void testVoiceQuery(String queryName, String query, String[] expectedKeywords,
                                String assertionPattern, String disambiguationAccount) throws Exception {
        runVoiceQuery(queryName, query, expectedKeywords, assertionPattern, disambiguationAccount);
    }

    /** Five queries chosen to cover every distinct code path in this file's assertion patterns
     * rather than just one happy path: generic phrasing disambiguating to savings, generic
     * phrasing disambiguating to current (the opposite direction), an explicit savings-only
     * query (no disambiguation, SAVINGS pattern), an explicit current-only query (no
     * disambiguation, CURRENT pattern), and a short imperative phrasing style distinct from the
     * question-form rows above it. Run this tier for a fast build/deploy health check. */
    @DataProvider(name = "smokeQueries")
    public Object[][] smokeQueries() {
        return new Object[][]{
            {"Account Balance", VoiceQueries.English.ACCOUNT_BALANCE,
                    new String[]{"balance", "account"}, BotResponsePatterns.Balance.ANY, "savings"},
            {"How Much Money", VoiceQueries.English.HOW_MUCH_MONEY,
                    new String[]{"balance", "account"}, BotResponsePatterns.Balance.ANY, "current"},
            {"Savings Balance", VoiceQueries.English.SAVINGS_BALANCE,
                    new String[]{"balance", "savings"}, BotResponsePatterns.Balance.SAVINGS, null},
            {"Current Balance", VoiceQueries.English.CURRENT_BALANCE,
                    new String[]{"balance", "current"}, BotResponsePatterns.Balance.CURRENT, null},
            {"Check Balance", VoiceQueries.English.CHECK_BALANCE,
                    new String[]{"balance", "account"}, BotResponsePatterns.Balance.ANY, "savings"},
        };
    }

    @Test(dataProvider = "smokeQueries", groups = {"ui", "smoke", "botverification"},
            description = "Smoke: should process a basic English balance voice query")
    public void testSmokeQuery(String queryName, String query, String[] expectedKeywords,
                                String assertionPattern, String disambiguationAccount) throws Exception {
        runVoiceQuery(queryName, query, expectedKeywords, assertionPattern, disambiguationAccount);
    }

    /** One query per major balance-pattern category (savings-specific, current-specific, and
     * generic/either-account) — broader than smoke, still fast. */
    @DataProvider(name = "sanityQueries")
    public Object[][] sanityQueries() {
        return new Object[][]{
            {"Savings Balance", VoiceQueries.English.SAVINGS_BALANCE,
                    new String[]{"balance", "savings"}, BotResponsePatterns.Balance.SAVINGS, null},
            {"Current Balance", VoiceQueries.English.CURRENT_BALANCE,
                    new String[]{"balance", "current"}, BotResponsePatterns.Balance.CURRENT, null},
            {"Balance Query",   VoiceQueries.English.BALANCE_QUERY,
                    new String[]{"balance", "account"}, BotResponsePatterns.Balance.ANY, "current"},
        };
    }

    @Test(dataProvider = "sanityQueries", groups = {"ui", "sanity", "botverification"},
            description = "Sanity: should process one balance voice query per major pattern category")
    public void testSanityQuery(String queryName, String query, String[] expectedKeywords,
                                 String assertionPattern, String disambiguationAccount) throws Exception {
        runVoiceQuery(queryName, query, expectedKeywords, assertionPattern, disambiguationAccount);
    }

    /** Account-to-customer mapping is known statically, so each row names its account explicitly
     * (SAVINGS_BALANCE / CURRENT_BALANCE) and never triggers the disambiguation follow-up — no
     * need to guess or loop over possibilities. {@code accountId}/{@code accountType} (new columns
     * beyond the generic voiceQueries shape) are what {@link #testKnownAccountBalance} cross-checks
     * the spoken balance against, via the ground-truth Account Balance API. */
    @DataProvider(name = "knownAccountBalanceQueries")
    public Object[][] knownAccountBalanceQueries() {
        return new Object[][]{

            // {queryName, query, expectedKeywords, assertionPattern, disambiguationAccount, phoneNumber, accountId, accountType}
            {"Customer A Savings Balance", VoiceQueries.English.SAVINGS_BALANCE,
                    new String[]{"balance", "savings"}, BotResponsePatterns.Balance.SAVINGS, null, CUSTOMER_A_PHONE,
                    Constants.EXPECTED_ACCOUNT_ID, Constants.SAVINGS_ACCOUNT_TYPE},
            {"Customer A Current Balance", VoiceQueries.English.CURRENT_BALANCE,
                    new String[]{"balance", "current"}, BotResponsePatterns.Balance.CURRENT, null, CUSTOMER_A_PHONE,
                    Constants.CUSTOMER_A_CURRENT_ACCOUNT_ID, Constants.CURRENT_ACCOUNT_TYPE},
            {"Customer B Savings Balance", VoiceQueries.English.SAVINGS_BALANCE,
                    new String[]{"balance", "savings"}, BotResponsePatterns.Balance.SAVINGS, null, CUSTOMER_B_PHONE,
                    Constants.CUSTOMER_B_SAVINGS_ACCOUNT_ID, Constants.SAVINGS_ACCOUNT_TYPE},
            {"Customer C Savings Balance", VoiceQueries.English.SAVINGS_BALANCE,
                    new String[]{"balance", "savings"}, BotResponsePatterns.Balance.SAVINGS, null, Constants.CUSTOMER_C_PHONE,
                    Constants.CUSTOMER_C_SAVINGS_ACCOUNT_ID, Constants.SAVINGS_ACCOUNT_TYPE},
        };
    }

    @Test(dataProvider = "knownAccountBalanceQueries", groups = {"ui", "regression", "botverification"},
            description = "Should return the correct account balance for known seeded customers, cross-checked against the Account Balance API")
    public void testKnownAccountBalance(String queryName, String query, String[] expectedKeywords,
                                         String assertionPattern, String disambiguationAccount,
                                         String phoneNumber, String accountId, String accountType) throws Exception {
        runVoiceQuery(queryName, query, expectedKeywords, assertionPattern, disambiguationAccount, phoneNumber);

        Matcher matcher = BotResponsePatterns.Balance.VALUE.matcher(lastBotResponse());
        Assert.assertTrue(matcher.find(),
                "[" + queryName + "] Could not extract a balance value from: " + lastBotResponse());
        double spokenBalance = Double.parseDouble(matcher.group(1).replace(",", ""));

        double actualBalance = groundTruth().accountBalance(accountId, Constants.CUSTOMER_ID_BY_PHONE.get(phoneNumber), accountType);
        Assert.assertEquals(spokenBalance, actualBalance, 0.01,
                "[" + queryName + "] Spoken balance did not match the ground-truth Account Balance API.");
    }
}
