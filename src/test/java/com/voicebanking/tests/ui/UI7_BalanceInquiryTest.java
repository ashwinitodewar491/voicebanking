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

    /** Rohit Mehta, the dual-account (savings + current) customer used by default. */
    private static final String BALANCE_INQUIRY_CUSTOMER_PHONE = "9898989898";

    /** Customer A (Rohit Mehta, CIF202602260005): savings + current. */
    private static final String CUSTOMER_A_PHONE = "9898989898";
    /** Customer B (Leena Kamat, CIF202602260042) — savings only, no loan, no beneficiary. */
    private static final String CUSTOMER_B_PHONE = "9812341042";

    /** Rows are independent turns on the same account, so they share one session. */
    @Override
    protected boolean useSharedSession() {
        return true;
    }

    /** A dual-account seeded customer, so CURRENT-balance assertions have an account to report on. */
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

    /** Fast health-check tier: savings/current disambiguation, explicit savings, explicit current,
     * and an imperative phrasing. */
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

    /** Rows name their account explicitly (no disambiguation); the spoken balance is cross-checked
     * against the Account Balance API. */
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
        System.out.println("[GroundTruth] " + queryName + " — spoken=" + spokenBalance
                + " api=" + actualBalance + " match=" + (Math.abs(spokenBalance - actualBalance) < 0.01));
        Assert.assertEquals(spokenBalance, actualBalance, 0.01,
                "[" + queryName + "] Spoken balance did not match the ground-truth Account Balance API.");
    }
}
