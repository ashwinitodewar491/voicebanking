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

            // --- CI smoke set (4 queries) — uncomment the rest once CI is green ---
            {"Account Balance",              VoiceQueries.English.ACCOUNT_BALANCE,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
            {"Savings Balance",              VoiceQueries.English.SAVINGS_BALANCE,
                    new String[]{"balance", "savings"},  BotResponsePatterns.Balance.SAVINGS},
            {"How Much Money",               VoiceQueries.English.HOW_MUCH_MONEY,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
            {"Balance Query",                VoiceQueries.English.BALANCE_QUERY,
                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},

            // --- Full suite (uncomment when CI smoke passes) ---
//            {"Balance Short",                VoiceQueries.English.BALANCE_SHORT,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Current Balance",              VoiceQueries.English.CURRENT_BALANCE,
//                    new String[]{"balance", "current"},  BotResponsePatterns.Balance.ANY},
//            {"Money In Account",             VoiceQueries.English.MONEY_IN_ACCOUNT,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Show Account Balance",         VoiceQueries.English.SHOW_ACCOUNT_BALANCE,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Tell Account Balance",         VoiceQueries.English.TELL_ACCOUNT_BALANCE,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Can Tell Balance",             VoiceQueries.English.CAN_TELL_BALANCE,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Please Check Balance",         VoiceQueries.English.PLEASE_CHECK_BALANCE,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Check Account Balance",        VoiceQueries.English.CHECK_ACCOUNT_BALANCE,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Show My Balance",              VoiceQueries.English.SHOW_MY_BALANCE,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Balance Available",            VoiceQueries.English.BALANCE_AVAILABLE,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Available Balance In Account", VoiceQueries.English.AVAILABLE_BALANCE_IN_ACCOUNT,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Amount Left",                  VoiceQueries.English.AMOUNT_LEFT,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Cash In Account",              VoiceQueries.English.CASH_IN_ACCOUNT,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Amount In Account",            VoiceQueries.English.AMOUNT_IN_ACCOUNT,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Savings Balance Tell",         VoiceQueries.English.SAVINGS_BALANCE_TELL,
//                    new String[]{"balance", "savings"},  BotResponsePatterns.Balance.SAVINGS},
//            {"Current Balance Tell",         VoiceQueries.English.CURRENT_BALANCE_TELL,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Savings Balance Of",           VoiceQueries.English.SAVINGS_BALANCE_OF,
//                    new String[]{"balance", "savings"},  BotResponsePatterns.Balance.SAVINGS},
//            {"Current Balance Of",           VoiceQueries.English.CURRENT_BALANCE_OF,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Enough Money",                 VoiceQueries.English.ENOUGH_MONEY,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Can Know Balance",             VoiceQueries.English.CAN_KNOW_BALANCE,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Want To Know Balance",         VoiceQueries.English.WANT_TO_KNOW_BALANCE,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Get Account Balance",          VoiceQueries.English.GET_ACCOUNT_BALANCE,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Read Out Balance",             VoiceQueries.English.READ_OUT_BALANCE,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Access Account Balance",       VoiceQueries.English.ACCESS_ACCOUNT_BALANCE,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Fetch Account Balance",        VoiceQueries.English.FETCH_ACCOUNT_BALANCE,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Balance In Account",           VoiceQueries.English.BALANCE_IN_ACCOUNT,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Account Balance Query",        VoiceQueries.English.ACCOUNT_BALANCE_QUERY,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Savings Balance Query",        VoiceQueries.English.SAVINGS_BALANCE_QUERY,
//                    new String[]{"balance", "savings"},  BotResponsePatterns.Balance.SAVINGS},
//            {"Current Account Balance Query", VoiceQueries.English.CURRENT_ACCOUNT_BALANCE_QUERY,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"My Balance",                   VoiceQueries.English.MY_BALANCE,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Available Balance",            VoiceQueries.English.AVAILABLE_BALANCE,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Remaining Balance",            VoiceQueries.English.REMAINING_BALANCE,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Check Balance",                VoiceQueries.English.CHECK_BALANCE,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Show Balance",                 VoiceQueries.English.SHOW_BALANCE,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Need Balance",                 VoiceQueries.English.NEED_BALANCE,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Remaining Amount",             VoiceQueries.English.REMAINING_AMOUNT,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Funds Available",              VoiceQueries.English.FUNDS_AVAILABLE,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Amount Available",             VoiceQueries.English.AMOUNT_AVAILABLE,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Balance Check",                VoiceQueries.English.BALANCE_CHECK,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
//            {"Account Balance Check",        VoiceQueries.English.ACCOUNT_BALANCE_CHECK,
//                    new String[]{"balance", "account"},  BotResponsePatterns.Balance.ANY},
        };
    }

    @Test(dataProvider = "voiceQueries", groups = {"ui", "regression", "balance"},
            description = "Should process English balance voice query and verify bot response")
    public void testVoiceQuery(String queryName, String query,
                               String[] expectedKeywords, String assertionPattern) throws Exception {
        runVoiceQuery(queryName, query, expectedKeywords, assertionPattern);
    }
}
