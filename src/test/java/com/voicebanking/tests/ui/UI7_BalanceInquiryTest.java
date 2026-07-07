package com.voicebanking.tests.ui;

import com.voicebanking.DataText.BotResponsePatterns;
import com.voicebanking.DataText.VoiceQueries;
import com.voicebanking.tests.ui.base.BaseVoiceTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class UI7_BalanceInquiryTest extends BaseVoiceTest {

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

    @Test(dataProvider = "voiceQueries", groups = {"ui", "regression", "balance"},
            description = "Should process English balance voice query and verify bot response")
    public void testVoiceQuery(String queryName, String query, String[] expectedKeywords,
                                String assertionPattern, String disambiguationAccount) throws Exception {
        runVoiceQuery(queryName, query, expectedKeywords, assertionPattern, disambiguationAccount);
    }
}
