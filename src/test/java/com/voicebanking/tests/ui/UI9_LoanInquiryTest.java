package com.voicebanking.tests.ui;

import com.voicebanking.DataText.BotResponsePatterns;
import com.voicebanking.DataText.Constants;
import com.voicebanking.DataText.VoiceQueries;
import com.voicebanking.pages.HomePage;
import com.voicebanking.tests.ui.base.BaseVoiceTest;
import com.voicebanking.utils.GroundTruthApi;
import com.voicebanking.utils.TtsUtil;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

/**
 * Customer A (Rohit Mehta, CIF202602260005, 9898989898) has two loans — Home Loan (LN10005,
 * active) and Education Loan (LN20005, closed) — confirmed live. "Car loan" is not one of them;
 * it's used deliberately in two rows as a negative test of the bot's fallback: named a loan type
 * that doesn't exist, the bot should list the real loans and ask which one, matching
 * BotResponsePatterns.Loans.LOAN_OPTIONS_PROMPT.
 * All 69 phrasings are active. Loan-agnostic queries (no type named) answer the disambiguation
 * follow-up alternating Home/Education via the mic-reacquire fix in BaseVoiceTest. Most {loan type}
 * queries assert on keywords only — the exact response format is confirmed for EMI, interest,
 * outstanding, and next-EMI-due (upgraded to a precise pattern); everything else is exploratory.
 * Rows that assume an *active* loan (EMI due date, min amount due, etc.) may need a second look
 * now that the second loan (Education) is closed rather than active — flag any new failures there
 * for a closer look at whether the bot's response differs for a closed loan.
 */
public class UI9_LoanInquiryTest extends BaseVoiceTest {

    /** All loan-inquiry rows are independent conversational turns against the same customer's two
     * loans — safe to run in one continuous session instead of a fresh browser/login per row, the
     * same reasoning UI7 (balance) already uses. Cuts a 76-row regression run from ~76 logins down
     * to effectively one.
     * <p>
     * Unlike UI7's balance queries, though, this class's follow-up flows are themselves
     * stateful — "which loan?" and "what would you like to know?" disambiguation, and {@link
     * #walkLoanDetailCategories} walking all 7 detail categories in one conversation. UI8
     * (transaction history) hit a real cross-row contamination bug from shared sessions once
     * before (a category filter from one query stuck around and silently answered several later,
     * unrelated queries) and stayed on fresh-login-per-row specifically because of it. If loan
     * regression runs start seeing responses that look like they're answering a *previous* row's
     * question instead of the current one, that's the first thing to suspect — revert this
     * override before investigating further. */
    @Override
    protected boolean useSharedSession() {
        return true;
    }

    @Override
    protected String getLoginPhoneNumber() {
        return "9898989898";
    }

    @DataProvider(name = "voiceQueries")
    public Object[][] voiceQueries() {
        return new Object[][]{

            // {queryName, query, expectedKeywords, assertionPattern, disambiguationAccount}

            // No loan type named — bot lists the real loans and asks which; these rows answer the
            // follow-up (alternating Home/Education) and assert the resolved response. The
            // follow-up mic stream is force-reacquired before speaking (see
            // BaseVoiceTest.reacquireMicrophoneForFollowUp) so Chromium's fake audio device opens
            // fresh and picks up the overwritten WAV, instead of replaying whatever it originally
            // buffered for the main query.
            {"Loan Details",                  VoiceQueries.English.LOAN_DETAILS,
                    new String[]{"loan"}, null, "home loan"},
            {"Active Loans Any",               VoiceQueries.English.ACTIVE_LOANS_ANY,
                    new String[]{"loan"}, null, "education loan"},

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

            // Education Loan — the other real loan for this customer (closed status).
            {"Loan Type EMI Education",        VoiceQueries.English.LOAN_TYPE_EMI_PERSONAL,
                    new String[]{"emi"}, null, null},
            {"Loan Type Interest Education",   VoiceQueries.English.LOAN_TYPE_INTEREST_PERSONAL,
                    new String[]{"interest"}, null, null},
            {"Loan Type Outstanding Education", VoiceQueries.English.LOAN_TYPE_OUTSTANDING_PERSONAL,
                    null, BotResponsePatterns.Loans.OUTSTANDING, null},
            {"Next EMI Due Education",          VoiceQueries.English.LOAN_TYPE_NEXT_EMI_DUE_PERSONAL,
                    null, BotResponsePatterns.Loans.NEXT_EMI_DUE, null},
            // Remaining {loan type} phrasings — text already alternates home/education loan in
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
            // answers the follow-up (alternating Home/Education) via the mic-reacquire fix.
            {"Loan Accounts Tell",             VoiceQueries.English.LOAN_ACCOUNTS_TELL,
                    new String[]{"loan"}, null, "home loan"},
            {"Loan Accounts What",             VoiceQueries.English.LOAN_ACCOUNTS_WHAT,
                    new String[]{"loan"}, null, "education loan"},
            {"Loan Accounts Show",             VoiceQueries.English.LOAN_ACCOUNTS_SHOW,
                    new String[]{"loan"}, null, "home loan"},
            {"Loans What",                     VoiceQueries.English.LOANS_WHAT,
                    new String[]{"loan"}, null, "education loan"},
            {"Loans Tell About",               VoiceQueries.English.LOANS_TELL_ABOUT,
                    new String[]{"loan"}, null, "home loan"},
            {"Active Loans Show",              VoiceQueries.English.ACTIVE_LOANS_SHOW,
                    new String[]{"loan", "active"}, null, "education loan"},
            {"Loans Running Which",            VoiceQueries.English.LOANS_RUNNING_WHICH,
                    new String[]{"loan"}, null, "home loan"},
            {"Loans Currently Paying",         VoiceQueries.English.LOANS_CURRENTLY_PAYING,
                    new String[]{"loan"}, null, "education loan"},
            {"EMI How Much",                   VoiceQueries.English.EMI_HOW_MUCH,
                    new String[]{"emi"}, null, "home loan"},
            {"Monthly Installment What",       VoiceQueries.English.MONTHLY_INSTALLMENT_WHAT,
                    new String[]{"installment", "emi"}, null, "education loan"},
            {"Pay Every Month What",           VoiceQueries.English.PAY_EVERY_MONTH_WHAT,
                    new String[]{"emi", "month"}, null, "home loan"},
            {"Interest How Much",              VoiceQueries.English.INTEREST_HOW_MUCH,
                    new String[]{"interest"}, null, "education loan"},
            {"Loan Interest Rate My",          VoiceQueries.English.LOAN_INTEREST_RATE_MY,
                    new String[]{"interest"}, null, "home loan"},
            {"Still Owe How Much",             VoiceQueries.English.STILL_OWE_HOW_MUCH,
                    new String[]{"owe", "outstanding", "remaining"}, null, "education loan"},
            {"Loan Left How Much",             VoiceQueries.English.LOAN_LEFT_HOW_MUCH,
                    new String[]{"loan", "outstanding", "remaining"}, null, "home loan"},
            {"Remaining Loan Balance My",      VoiceQueries.English.REMAINING_LOAN_BALANCE_MY,
                    new String[]{"remaining", "balance"}, null, "education loan"},
            {"EMIs Left How Many",             VoiceQueries.English.EMIS_LEFT_HOW_MANY,
                    new String[]{"emi"}, null, "home loan"},
            {"Installments Remaining How Many", VoiceQueries.English.INSTALLMENTS_REMAINING_HOW_MANY,
                    new String[]{"installment", "emi", "remaining"}, null, "education loan"},
            {"Months Left Loan",               VoiceQueries.English.MONTHS_LEFT_LOAN,
                    new String[]{"month", "tenure", "remaining"}, null, "home loan"},
            {"Loan End When",                  VoiceQueries.English.LOAN_END_WHEN,
                    new String[]{"loan", "end", "tenure"}, null, "education loan"},
            {"Remaining Tenure Loan",          VoiceQueries.English.REMAINING_TENURE_LOAN,
                    new String[]{"tenure", "remaining"}, null, "home loan"},
            {"Still Have To Pay How Long",     VoiceQueries.English.STILL_HAVE_TO_PAY_HOW_LONG,
                    new String[]{"tenure", "month", "remaining"}, null, "education loan"},
            {"Loan Taken How Much",            VoiceQueries.English.LOAN_TAKEN_HOW_MUCH,
                    new String[]{"loan", "amount"}, null, "home loan"},
            {"Sanctioned Amount Was",          VoiceQueries.English.SANCTIONED_AMOUNT_WAS,
                    new String[]{"sanctioned"}, null, "education loan"},
            {"Total Loan Amount What",         VoiceQueries.English.TOTAL_LOAN_AMOUNT_WHAT,
                    new String[]{"loan", "amount"}, null, "home loan"},
            {"Next EMI Due When",              VoiceQueries.English.NEXT_EMI_DUE_WHEN,
                    new String[]{"emi", "due"}, null, "education loan"},
            {"Next EMI Date What",             VoiceQueries.English.NEXT_EMI_DATE_WHAT,
                    new String[]{"emi", "date"}, null, "home loan"},
            {"Payment Due When",               VoiceQueries.English.PAYMENT_DUE_WHEN,
                    new String[]{"emi", "due", "payment"}, null, "education loan"},
            {"Installment Due When",           VoiceQueries.English.INSTALLMENT_DUE_WHEN,
                    new String[]{"emi", "due", "installment"}, null, "home loan"},
            {"Loan Status What",               VoiceQueries.English.LOAN_STATUS_WHAT,
                    new String[]{"status", "loan"}, null, "education loan"},
            {"Loan Still Running",             VoiceQueries.English.LOAN_STILL_RUNNING,
                    new String[]{"active", "running", "loan"}, null, "home loan"},
            {"Loan Closed Has",                VoiceQueries.English.LOAN_CLOSED_HAS,
                    new String[]{"closed", "loan", "active"}, null, "education loan"},
            {"Loan Amount Was",                VoiceQueries.English.LOAN_AMOUNT_WAS,
                    new String[]{"loan", "amount"}, null, "home loan"},
            {"Next EMI Due Date",              VoiceQueries.English.NEXT_EMI_DUE_DATE,
                    new String[]{"emi", "due"}, null, "education loan"},
            {"Next EMI Pay When",              VoiceQueries.English.NEXT_EMI_PAY_WHEN,
                    new String[]{"emi", "due"}, null, "home loan"},
            {"Latest EMI Paid",                VoiceQueries.English.LATEST_EMI_PAID,
                    new String[]{"emi", "paid"}, null, "education loan"},
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

    /** Five queries chosen to cover every distinct loan-flow category and both real loans for
     * this customer: a named-type keyword query (Home), a precise-pattern query confirmed via
     * manual testing (Outstanding, Education — different loan than the row above), the
     * loan-agnostic disambiguation flow (no type named, resolves to Home), the nonexistent-type
     * fallback (bot lists real loans instead of answering), and a second precise-pattern category
     * (Next EMI Due, Education) distinct from Outstanding. Run this tier for a fast build/deploy
     * health check. */
    @DataProvider(name = "smokeQueries")
    public Object[][] smokeQueries() {
        return new Object[][]{
            {"Loan Type EMI Home", VoiceQueries.English.LOAN_TYPE_EMI_HOME,
                    new String[]{"emi"}, null, null},
            {"Loan Type Outstanding Education", VoiceQueries.English.LOAN_TYPE_OUTSTANDING_PERSONAL,
                    null, BotResponsePatterns.Loans.OUTSTANDING, null},
            {"Loan Details", VoiceQueries.English.LOAN_DETAILS,
                    new String[]{"loan"}, null, "home loan"},
            {"Loan Type Details Education", VoiceQueries.English.LOAN_TYPE_DETAILS,
                    null, BotResponsePatterns.Loans.LOAN_OPTIONS_PROMPT, null},
            {"Next EMI Due Education", VoiceQueries.English.LOAN_TYPE_NEXT_EMI_DUE_PERSONAL,
                    null, BotResponsePatterns.Loans.NEXT_EMI_DUE, null},
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
            {"Loan Type Outstanding Education", VoiceQueries.English.LOAN_TYPE_OUTSTANDING_PERSONAL,
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

    /** Negative/positive-case cross-verification: {@code expectedLoanCount} is independently
     * confirmed against the Loan Summary API itself (not just assumed) before the voice query
     * even runs, so a real seed-data change would fail loudly here instead of a row silently
     * asserting the wrong thing forever.
     * <p>
     * Customer B (Leena Kamat, CIF202602260042) has no loans at all — expectedKeywords stays a
     * loose check since the bot's "no loans" wording isn't confirmed, and there's no loan-detail
     * prompt to walk either way. Customer C (Aniket More, CIF202602260041) has one active
     * PERSONAL_LOAN — confirmed live to use the same "What would you like to know about your
     * Personal Loan?" phrasing as Home/Education, so {@link BotResponsePatterns.Loans} covers
     * "Personal" in every alternation and this row gets the full {@link
     * #walkLoanDetailCategories} cross-verification (EMI, tenure, interest rate, outstanding,
     * sanctioned amount, next EMI due) for free — nothing further to wire up here. */
    @DataProvider(name = "knownAccountLoanQueries")
    public Object[][] knownAccountLoanQueries() {
        return new Object[][]{

            // {queryName, query, expectedKeywords, phoneNumber, expectedLoanCount}
            {"Customer B No Loans", VoiceQueries.English.LOAN_DETAILS,
                    new String[]{"loan"}, Constants.CUSTOMER_B_PHONE, 0},
            {"Customer C Personal Loan", VoiceQueries.English.LOAN_DETAILS,
                    new String[]{"loan"}, Constants.CUSTOMER_C_PHONE, 1},
        };
    }

    @Test(dataProvider = "knownAccountLoanQueries", groups = {"ui", "regression", "botverification"},
            description = "Should return correct loan details (or confirmed absence/count of loans) for known seeded customers")
    public void testKnownAccountLoanDetails(String queryName, String query, String[] expectedKeywords,
                                             String phoneNumber, int expectedLoanCount) throws Exception {
        String customerId = Constants.CUSTOMER_ID_BY_PHONE.get(phoneNumber);
        java.util.List<GroundTruthApi.LoanRecord> apiLoans = groundTruth().loanSummary(customerId);
        System.out.println("[GroundTruth] " + queryName + " — expectedLoanCount=" + expectedLoanCount
                + " apiLoanCount=" + apiLoans.size());
        Assert.assertEquals(apiLoans.size(), expectedLoanCount,
                "[" + queryName + "] Expected " + expectedLoanCount + " loan(s) for " + phoneNumber
                + " per the Loan Summary API, got " + apiLoans.size()
                + " — seed data may have changed, update this row's expectations.");

        runVoiceQuery(queryName, query, expectedKeywords, null, null, phoneNumber);
    }

    /** Also recognizes loan disambiguation and the loan-detail-category prompt, so the
     * transcription-retry loop stops re-playing the original query's audio once the bot has
     * already moved to "which loan?" or "what would you like to know?" — otherwise a retry
     * risks being misheard as an answer to that question instead of the one intended. */
    @Override
    protected boolean shouldStopRetrying(String botResponse) {
        return super.shouldStopRetrying(botResponse)
                || isLoanDisambiguation(botResponse)
                || isLoanDetailPrompt(botResponse);
    }

    /** A generic "give me loan details" query can land in one of several places depending on
     * how many loans this customer actually has and how much the bot needs to ask to narrow
     * down the answer — so each stage below only engages when its own prompt is actually seen,
     * never assumed, and a single-loan customer who gets a direct answer straight away simply
     * skips both stages untouched:
     * <p>
     * Stage 1 — "which loan?" (only when the query named no type AND the customer has more
     * than one loan; reuses disambiguationAccount's slot as "what to say if asked"). Rows that
     * want to assert this prompt itself (e.g. an invalid loan type) pass disambiguationAccount
     * as null, which skips this stage entirely.
     * <p>
     * Stage 2 — "what would you like to know about your X loan?" (asked once the loan itself
     * is known, either named directly by the query or just resolved by stage 1) — walks every
     * detail category the bot claims to support in {@link BotResponsePatterns.Loans#LOAN_DETAIL_OPTIONS_PROMPT},
     * asserting each in turn.
     * <p>
     * The final check in runVoiceQuery reuses the row's own assertionPattern / expectedKeywords
     * rather than a hardcoded pattern, since the right answer depends on what was actually
     * asked (EMI, interest, outstanding, ...). */
    @Override
    protected String handleAdditionalFollowUp(String queryName, String query, String botResponse,
                                               String disambiguationAccount, HomePage homePage) throws Exception {
        if (isLoanDisambiguation(botResponse) && disambiguationAccount != null) {
            botResponse = answerWhichLoan(queryName, query, botResponse, homePage, disambiguationAccount);
        }

        if (isLoanDetailPrompt(botResponse)) {
            botResponse = walkLoanDetailCategories(queryName, query, botResponse, homePage);
        }

        return botResponse;
    }

    private String answerWhichLoan(String queryName, String query, String botResponse, HomePage homePage,
                                    String followUpLoan) throws Exception {
        String followUpResponse = botResponse;

        for (int attempt = 1; attempt <= 3; attempt++) {
            followUpResponse = reestablishIntentIfSessionEnded(homePage, queryName, query, followUpResponse);
            // Excludes a still-broken context-lost fallback — if re-sending the original query got
            // the same fallback back, the bot hasn't actually recovered; keep retrying instead of
            // handing that fallback off as if it were a resolved answer.
            if (!isLoanDisambiguation(followUpResponse) && !isContextLostFallback(followUpResponse)) {
                // Re-sending the original query after a session drop either answered it directly
                // or landed somewhere else entirely — either way there's no "which loan" prompt
                // left to answer.
                return followUpResponse;
            }

            System.out.println("[" + queryName + "] Bot asked to choose a loan — following up with '"
                    + followUpLoan + "' (attempt " + attempt + ")...");

            long oldAudioDurationMs = TtsUtil.getWavDurationMs(currentAudioPath);
            String followUpPath = TtsUtil.generateWav(followUpLoan);
            Files.copy(Path.of(followUpPath), Path.of(currentAudioPath), StandardCopyOption.REPLACE_EXISTING);
            int followUpHoldMs = (int) TtsUtil.getWavDurationMs(currentAudioPath);
            TtsUtil.deleteWav(followUpPath);

            homePage.reacquireMicrophoneForFollowUp();

            int preWaitMs = (int) oldAudioDurationMs + 2000 + ((attempt - 1) * 2000);
            homePage.speakFollowUp(preWaitMs, followUpHoldMs, 8000);
            homePage.waitForVoiceResponse(15000);

            String followUpTranscribed = homePage.getLastTranscribedText();
            followUpResponse = homePage.getLastBotResponse();
            System.out.println("[" + queryName + "] Follow-up Transcribed : " + followUpTranscribed);
            System.out.println("[" + queryName + "] Follow-up Bot response: " + followUpResponse);

            boolean heardExpectedLoan = followUpTranscribed.toLowerCase().contains(followUpLoan.toLowerCase());
            // Excludes the context-lost fallback for the same reason as BaseVoiceTest's own
            // account-disambiguation loop — it isn't the "which loan" prompt either, so without
            // this the loop would break out treating the fallback as a real answer instead of
            // retrying/recovering.
            if (heardExpectedLoan && !isLoanDisambiguation(followUpResponse) && !isContextLostFallback(followUpResponse)) {
                break;
            }
            System.out.println("[" + queryName + "] WARN — follow-up not recognised (attempt " + attempt
                    + "): expected [" + followUpLoan + "] got transcribed [" + followUpTranscribed
                    + "], bot [" + followUpResponse + "] — retrying...");
        }

        return followUpResponse;
    }

    /** Every detail category the "what would you like to know about your X loan?" prompt
     * claims to support, in the order confirmed live. "Loan amount" is last since, unlike the
     * other six, its response pattern ({@link BotResponsePatterns.Loans#LOAN_AMOUNT}) is an
     * inferred guess rather than a confirmed live response — if this one fails, that pattern is
     * the first place to check. */
    private static final String[] LOAN_DETAIL_CATEGORIES = {
            "EMI", "Tenure", "Pending tenure", "Interest rate", "Outstanding amount",
            "Next EMI due date", "Loan amount"
    };

    /** Walks every category in {@link #LOAN_DETAIL_CATEGORIES} in the same conversation, one at
     * a time, asserting each response against the exact format confirmed live for that category
     * (see BotResponsePatterns.Loans) — only the amount/rate/date/month value itself varies per
     * account. Every category gets asserted internally regardless, but the response returned to
     * the caller (whose own final assertion checks it again, against the row's own pattern) is
     * whichever category the row's query actually named — not simply whichever was walked last.
     * That matters for a row like "Next EMI Due Education", which expects its own final check
     * (NEXT_EMI_DUE) to pass: a live STT mishearing can turn that query into something the bot
     * doesn't recognize as naming a specific detail, landing here via the generic prompt even
     * though the row itself is not generic — confirmed live. Falls back to the last category
     * walked for rows that genuinely are generic (e.g. "Loan Details", which names no category
     * and only expects a loose keyword match, so it doesn't matter which one comes back). */
    private String walkLoanDetailCategories(String queryName, String query, String promptResponse,
                                             HomePage homePage) throws Exception {
        java.util.Map<String, String> responsesByCategory = new java.util.LinkedHashMap<>();

        // Ground-truth cross-verification: the "what would you like to know about your X loan?"
        // prompt itself names the loan (see BotResponsePatterns.Loans.LOAN_DETAIL_OPTIONS_PROMPT),
        // so the loan record to check every category's spoken value against is resolved once,
        // up front, rather than threading the loan type through from answerWhichLoan — that's
        // simpler here since not every row reaches this method via disambiguation (a row that
        // names its loan directly, e.g. "home loan", skips answerWhichLoan entirely and lands
        // here straight away). Null (not thrown) when the loan type can't be resolved from the
        // prompt or the customer isn't a known one — see crossCheckLoanDetail for how that's
        // handled per category rather than failing the whole walk.
        GroundTruthApi.LoanRecord loanRecord = resolveLoanRecord(promptResponse);

        for (String category : LOAN_DETAIL_CATEGORIES) {
            String expectedPattern = loanDetailPatternFor(category);
            String response = "";

            for (int attempt = 1; attempt <= 3; attempt++) {
                // A session drop here loses the "which loan"/"what would you like to know"
                // context this category follow-up depends on — re-send the original query first
                // so we're back at (at worst) the loan-detail prompt before asking for this
                // category by name, rather than "EMI"/"Tenure"/... landing as a context-less
                // standalone query.
                String restored = reestablishIntentIfSessionEnded(homePage, queryName, query, "");
                // A still-broken context-lost fallback here means recovery hasn't actually
                // succeeded yet — don't hand it off as "landed outside the loan-detail prompt";
                // fall through and keep retrying instead.
                if (!restored.isEmpty() && !isLoanDetailPrompt(restored) && !isContextLostFallback(restored)) {
                    System.out.println("[" + queryName + "] Session-drop recovery for '" + category
                            + "' landed outside the loan-detail prompt — using that response as-is: " + restored);
                    response = restored;
                    break;
                }

                System.out.println("[" + queryName + "] Loan-detail follow-up: asking '" + category
                        + "' (attempt " + attempt + ")...");

                long oldAudioDurationMs = TtsUtil.getWavDurationMs(currentAudioPath);
                String followUpPath = TtsUtil.generateWav(category);
                Files.copy(Path.of(followUpPath), Path.of(currentAudioPath), StandardCopyOption.REPLACE_EXISTING);
                int followUpHoldMs = (int) TtsUtil.getWavDurationMs(currentAudioPath);
                TtsUtil.deleteWav(followUpPath);

                homePage.reacquireMicrophoneForFollowUp();

                int preWaitMs = (int) oldAudioDurationMs + 2000 + ((attempt - 1) * 2000);
                homePage.speakFollowUp(preWaitMs, followUpHoldMs, 8000);
                homePage.waitForVoiceResponse(15000);

                String transcribed = homePage.getLastTranscribedText();
                response = homePage.getLastBotResponse();
                System.out.println("[" + queryName + "] '" + category + "' Transcribed : " + transcribed);
                System.out.println("[" + queryName + "] '" + category + "' Bot response: " + response);

                if (!response.isBlank() && Pattern.compile(expectedPattern).matcher(response).find()) {
                    break;
                }
                System.out.println("[" + queryName + "] WARN — '" + category
                        + "' response didn't match the expected pattern yet (attempt " + attempt + ") — retrying...");
            }

            Assert.assertTrue(Pattern.compile(expectedPattern).matcher(response).find(),
                    "[" + queryName + "] '" + category + "' response did not match expected pattern.\n"
                    + "  Pattern : " + expectedPattern + "\n"
                    + "  Got     : " + response);
            responsesByCategory.put(category, response);

            crossCheckLoanDetail(queryName, category, response, loanRecord);
        }

        return responseMatchingQuery(queryName, responsesByCategory);
    }

    /** Picks the response for whichever category the row's queryName actually names, checked in
     * order from most to least specific (e.g. "pending"/"remaining" before the bare "tenure"
     * they're both a kind of, "next emi due" before the bare "emi" it contains) so a name like
     * "Next EMI Due Education" resolves to "Next EMI due date" rather than accidentally matching
     * "EMI" first. Falls back to the last category walked when nothing in the name points at a
     * specific one — true generic-query rows land here, and any of the (already individually
     * asserted) responses satisfies their own loose keyword check. */
    private String responseMatchingQuery(String queryName, java.util.Map<String, String> responsesByCategory) {
        String q = queryName.toLowerCase();

        if (q.contains("next") && q.contains("emi") && q.contains("due")) return responsesByCategory.get("Next EMI due date");
        if (q.contains("pending") || q.contains("remaining")) return responsesByCategory.get("Pending tenure");
        if (q.contains("tenure")) return responsesByCategory.get("Tenure");
        if (q.contains("interest")) return responsesByCategory.get("Interest rate");
        if (q.contains("outstanding")) return responsesByCategory.get("Outstanding amount");
        if (q.contains("emi")) return responsesByCategory.get("EMI");
        if (q.contains("sanctioned") || q.contains("amount") || q.contains("taken")) return responsesByCategory.get("Loan amount");

        return responsesByCategory.values().stream().reduce((first, last) -> last).orElse("");
    }

    private String loanDetailPatternFor(String category) {
        return switch (category) {
            case "EMI" -> BotResponsePatterns.Loans.EMI_AMOUNT;
            case "Tenure" -> BotResponsePatterns.Loans.TENURE;
            case "Pending tenure" -> BotResponsePatterns.Loans.PENDING_TENURE;
            case "Interest rate" -> BotResponsePatterns.Loans.INTEREST_RATE;
            case "Outstanding amount" -> BotResponsePatterns.Loans.OUTSTANDING;
            case "Next EMI due date" -> BotResponsePatterns.Loans.NEXT_EMI_DUE;
            case "Loan amount" -> BotResponsePatterns.Loans.LOAN_AMOUNT;
            default -> throw new IllegalStateException("Unknown loan-detail category: " + category);
        };
    }

    /** Resolves the {@link GroundTruthApi.LoanRecord} the loan-detail prompt is talking about, by
     * matching the loan type named in the prompt text (see {@link
     * BotResponsePatterns.Loans#LOAN_DETAIL_OPTIONS_PROMPT}) against the current customer's Loan
     * Summary. Returns null — rather than throwing — when the customer isn't one of the known,
     * mapped seeded customers ({@link #currentCustomerId()}), or the API doesn't return a loan of
     * the named type; either way {@link #crossCheckLoanDetail} just skips the numeric check for
     * that row instead of failing the whole walk, so the existing pattern-shape assertions above
     * (which already run regardless) remain the only requirement for customers this cross-check
     * doesn't cover yet. */
    private GroundTruthApi.LoanRecord resolveLoanRecord(String promptResponse) throws Exception {
        String customerId = currentCustomerId();
        if (customerId == null) {
            return null;
        }

        String lowerPrompt = promptResponse.toLowerCase();
        String wantedType = lowerPrompt.contains("education") ? "EDUCATION_LOAN"
                : lowerPrompt.contains("home") ? "HOME_LOAN"
                : lowerPrompt.contains("personal") ? "PERSONAL_LOAN" : null;
        if (wantedType == null) {
            return null;
        }

        for (GroundTruthApi.LoanRecord loan : groundTruth().loanSummary(customerId)) {
            if (wantedType.equals(loan.loanType)) {
                return loan;
            }
        }
        return null;
    }

    /** Extracts the numeric/text value the bot actually spoke for {@code category} and asserts it
     * matches the ground-truth Loan Summary/Overdue APIs for {@code loan} — the real
     * cross-verification this class was missing, on top of the shape-only pattern check the
     * caller already ran. A null {@code loan} (unresolvable customer/loan type — see {@link
     * #resolveLoanRecord}) skips the numeric check entirely rather than failing; "Next EMI due
     * date" further skips when the loan is CLOSED, since a closed loan's own bot response is the
     * literal string "N/A" (a legitimate
     * answer, not a date to fetch from Loan Overdue). */
    private void crossCheckLoanDetail(String queryName, String category, String response,
                                       GroundTruthApi.LoanRecord loan) throws Exception {
        if (loan == null) {
            return;
        }

        switch (category) {
            case "Tenure" -> assertExtractedEquals(queryName, category, response,
                    BotResponsePatterns.Loans.TENURE_VALUE, (double) loan.loanTenure);
            case "Pending tenure" -> assertExtractedEquals(queryName, category, response,
                    BotResponsePatterns.Loans.PENDING_TENURE_VALUE, (double) loan.pendingTenure);
            case "Interest rate" -> assertExtractedEquals(queryName, category, response,
                    BotResponsePatterns.Loans.INTEREST_RATE_VALUE, loan.interestRate);
            case "Outstanding amount" -> assertExtractedEquals(queryName, category, response,
                    BotResponsePatterns.Loans.OUTSTANDING_VALUE, loan.outstandingAmount);
            case "Loan amount" -> assertExtractedEquals(queryName, category, response,
                    BotResponsePatterns.Loans.LOAN_AMOUNT_VALUE, loan.loanAmount);
            case "EMI" -> {
                if (Constants.ACTIVE_STATUS.equals(loan.accountStatus)) {
                    GroundTruthApi.OverdueRecord overdue = groundTruth().loanOverdue(loan.accountId);
                    assertExtractedEquals(queryName, category, response,
                            BotResponsePatterns.Loans.EMI_AMOUNT_VALUE, overdue.nextInstallmentAmount);
                }
            }
            case "Next EMI due date" -> {
                if (Constants.ACTIVE_STATUS.equals(loan.accountStatus)) {
                    java.util.regex.Matcher m = BotResponsePatterns.Loans.NEXT_EMI_DUE_VALUE.matcher(response);
                    if (m.find() && !"N/A".equalsIgnoreCase(m.group(1).trim())) {
                        GroundTruthApi.OverdueRecord overdue = groundTruth().loanOverdue(loan.accountId);
                        System.out.println("[GroundTruth] " + queryName + " — '" + category + "' spoken="
                                + m.group(1).trim() + " api=" + overdue.nextDueDate
                                + " match=" + m.group(1).trim().equals(overdue.nextDueDate));
                        Assert.assertEquals(m.group(1).trim(), overdue.nextDueDate,
                                "[" + queryName + "] '" + category + "' spoken date did not match Loan Overdue API");
                    }
                }
            }
            default -> { }
        }
    }

    private void assertExtractedEquals(String queryName, String category, String response,
                                        Pattern capturingPattern, double expected) {
        java.util.regex.Matcher matcher = capturingPattern.matcher(response);
        Assert.assertTrue(matcher.find(),
                "[" + queryName + "] Could not extract a value for '" + category + "' from: " + response);

        double actual = Double.parseDouble(matcher.group(1).replace(",", ""));
        System.out.println("[GroundTruth] " + queryName + " — '" + category + "' spoken=" + actual
                + " api=" + expected + " match=" + (Math.abs(actual - expected) < 0.01));
        Assert.assertEquals(actual, expected, 0.01,
                "[" + queryName + "] '" + category + "' spoken value did not match the ground-truth API.\n"
                + "  Response : " + response);
    }

    /** Returns true when the bot response is asking the user to choose between loans. Deliberately
     * loose — observed phrasings vary ("You have the following loans: ...", "I can see multiple
     * loan accounts for you: ...") but all include "which loan". */
    private boolean isLoanDisambiguation(String response) {
        return response.toLowerCase().contains("which loan");
    }

    /** Returns true when the bot is asking which detail category the caller wants about an
     * already-known loan (e.g. "What would you like to know about your Home Loan?..."). */
    private boolean isLoanDetailPrompt(String response) {
        return Pattern.compile(BotResponsePatterns.Loans.LOAN_DETAIL_OPTIONS_PROMPT).matcher(response).find();
    }
}
