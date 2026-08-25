package com.voicebanking.tests.ui;

import com.microsoft.playwright.*;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.DataText.MultilingualVoiceQueries;
import com.voicebanking.pages.*;
import com.voicebanking.utils.NoResponseTracker;
import com.voicebanking.utils.ScreenshotUtil;
import com.voicebanking.utils.TtsUtil;
import com.voicebanking.utils.WelcomeMessageTracker;
import com.voicebanking.utils.tts.EdgeTtsEngine;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/** Hindi and Bengali voice-query trials across the core feature set (balance, transactions,
 * transfer, loan, EMI). Self-contained rather than extending BaseVoiceTest, since that base class
 * is hardcoded to the English locale/voice — kept separate so the existing English tests and
 * their data are untouched. */
public class UI12_MultilingualVoiceQueryTest {

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;
    private String audioPath;

    /** The locale we last confirmed active for this account (Rohit Mehta, 9898989898), tracked
     * across rows within this run — see {@link #loginAndSetLocale} for why this lets rows whose
     * locale matches the previous row skip the globe-icon reselect entirely. */
    private String lastConfirmedLocale;

    /** True when this row just switched locale (globe icon or first-ever onboarding) — see
     * {@link #loginAndSetLocale} and {@link #runMultilingualQuery} for why a row that switched
     * must not wait for a pre-existing bot welcome bubble before speaking: confirmed visually that
     * a locale switch clears the chat area without posting a fresh greeting, so that wait was
     * hanging its full 15s for a bubble that would never arrive. */
    private boolean justSwitchedLocale;

    /** Tracks every {@link #checkWelcomeMessage} observation across this run — one entry per
     * landing on Home, formatted "queryName [context]: present/ABSENT". Printed as a summary in
     * {@link #logWelcomeMessageSummary}. Not an instance field cleared between rows, deliberately —
     * the point is to see the pattern across the whole run, not just the current row. */
    private static final List<String> WELCOME_MESSAGE_LOG = new ArrayList<>();

    @DataProvider(name = "multilingualQueries")
    public Object[][] multilingualQueries() {
        return new Object[][]{
            // {queryName, locale, query, voice}
            {"Hindi Savings Balance",    MultilingualVoiceQueries.Hindi.LOCALE,   MultilingualVoiceQueries.Hindi.SAVINGS_BALANCE,          EdgeTtsEngine.VOICE_HINDI},
            {"Hindi Transaction List",   MultilingualVoiceQueries.Hindi.LOCALE,   MultilingualVoiceQueries.Hindi.TRANSACTION_LIST_SAVINGS, EdgeTtsEngine.VOICE_HINDI},
            {"Hindi Transfer Money",     MultilingualVoiceQueries.Hindi.LOCALE,   MultilingualVoiceQueries.Hindi.TRANSFER_TO_BENEFICIARY,  EdgeTtsEngine.VOICE_HINDI},
            {"Hindi Loan Account",       MultilingualVoiceQueries.Hindi.LOCALE,   MultilingualVoiceQueries.Hindi.LOAN_ACCOUNT_SAVINGS,     EdgeTtsEngine.VOICE_HINDI},
            {"Hindi EMI Statement",      MultilingualVoiceQueries.Hindi.LOCALE,   MultilingualVoiceQueries.Hindi.EMI_STATEMENT_HOME_LOAN,  EdgeTtsEngine.VOICE_HINDI},
            {"Bengali Savings Balance",  MultilingualVoiceQueries.Bengali.LOCALE, MultilingualVoiceQueries.Bengali.SAVINGS_BALANCE,          EdgeTtsEngine.VOICE_BENGALI},
            {"Bengali Transaction List", MultilingualVoiceQueries.Bengali.LOCALE, MultilingualVoiceQueries.Bengali.TRANSACTION_LIST_SAVINGS, EdgeTtsEngine.VOICE_BENGALI},
            {"Bengali Transfer Money",   MultilingualVoiceQueries.Bengali.LOCALE, MultilingualVoiceQueries.Bengali.TRANSFER_TO_BENEFICIARY,  EdgeTtsEngine.VOICE_BENGALI},
            {"Bengali Loan Account",     MultilingualVoiceQueries.Bengali.LOCALE, MultilingualVoiceQueries.Bengali.LOAN_ACCOUNT_SAVINGS,     EdgeTtsEngine.VOICE_BENGALI},
            {"Bengali EMI Statement",    MultilingualVoiceQueries.Bengali.LOCALE, MultilingualVoiceQueries.Bengali.EMI_STATEMENT_HOME_LOAN,  EdgeTtsEngine.VOICE_BENGALI},
        };
    }

    /** Five queries chosen to cover all five feature categories (balance, transactions, transfer,
     * loan, EMI) while alternating Hindi/Bengali row by row, so this smoke tier still gets
     * bilingual coverage instead of exercising only one language's TTS/STT/locale path. Run this
     * tier for a fast build/deploy health check. */
    @DataProvider(name = "smokeQueries")
    public Object[][] smokeQueries() {
        return new Object[][]{
            {"Hindi Savings Balance",    MultilingualVoiceQueries.Hindi.LOCALE,   MultilingualVoiceQueries.Hindi.SAVINGS_BALANCE,            EdgeTtsEngine.VOICE_HINDI},
            {"Bengali Transaction List", MultilingualVoiceQueries.Bengali.LOCALE, MultilingualVoiceQueries.Bengali.TRANSACTION_LIST_SAVINGS, EdgeTtsEngine.VOICE_BENGALI},
            {"Hindi Transfer Money",     MultilingualVoiceQueries.Hindi.LOCALE,   MultilingualVoiceQueries.Hindi.TRANSFER_TO_BENEFICIARY,    EdgeTtsEngine.VOICE_HINDI},
            {"Bengali Loan Account",     MultilingualVoiceQueries.Bengali.LOCALE, MultilingualVoiceQueries.Bengali.LOAN_ACCOUNT_SAVINGS,     EdgeTtsEngine.VOICE_BENGALI},
            {"Hindi EMI Statement",      MultilingualVoiceQueries.Hindi.LOCALE,   MultilingualVoiceQueries.Hindi.EMI_STATEMENT_HOME_LOAN,    EdgeTtsEngine.VOICE_HINDI},
        };
    }

    /** TEMP — isolates exactly two locale-switch transitions in one class instance run: row 1
     * switches away from whatever locale the account currently has (English, if nothing else has
     * touched it since) to Hindi; row 2 then switches from Hindi (set by row 1, tracked via
     * lastConfirmedLocale) to Bengali. Not part of any group other test runs would sweep in —
     * only reachable via an explicit -Dtest=...#testLocaleSwitchCheck. Remove after use. */
    @DataProvider(name = "localeSwitchCheck")
    public Object[][] localeSwitchCheck() {
        return new Object[][]{
            {"Hindi Savings Balance",    MultilingualVoiceQueries.Hindi.LOCALE,   MultilingualVoiceQueries.Hindi.SAVINGS_BALANCE,            EdgeTtsEngine.VOICE_HINDI},
            {"Bengali Transaction List", MultilingualVoiceQueries.Bengali.LOCALE, MultilingualVoiceQueries.Bengali.TRANSACTION_LIST_SAVINGS, EdgeTtsEngine.VOICE_BENGALI},
        };
    }

    @Test(dataProvider = "localeSwitchCheck", groups = {"localeSwitchCheck"},
            description = "TEMP: isolates English->Hindi then Hindi->Bengali locale switching")
    public void testLocaleSwitchCheck(String queryName, String locale, String query, String voice) throws Exception {
        runMultilingualQuery(queryName, locale, query, voice);
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp(Object[] params) throws Exception {
        String query = (String) params[2];
        String voice = (String) params[3];
        audioPath = TtsUtil.generateWav(query, voice);

        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));

        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setArgs(List.of(
                        "--disable-gpu",
                        "--use-fake-device-for-media-stream",
                        "--use-fake-ui-for-media-stream",
                        "--use-file-for-fake-audio-capture=" + audioPath)));
        context = browser.newContext(new Browser.NewContextOptions().setPermissions(List.of("microphone")));
        page = context.newPage();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) throws InterruptedException {
        ScreenshotUtil.captureOnFailure(page, result);

        if (page != null) page.close();
        if (context != null) context.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
        TtsUtil.deleteWav(audioPath);
        Thread.sleep(3000);
    }

    @Test(dataProvider = "multilingualQueries", groups = {"ui", "multilingual"},
            description = "Should process a Hindi/Bengali voice query and log the bot's response for manual cross-verification")
    public void testMultilingualVoiceQuery(String queryName, String locale, String query, String voice) throws Exception {
        runMultilingualQuery(queryName, locale, query, voice);
    }

    @Test(dataProvider = "smokeQueries", groups = {"ui", "smoke", "multilingual"},
            description = "Smoke: should process a basic Hindi/Bengali voice query per feature category")
    public void testSmokeQuery(String queryName, String locale, String query, String voice) throws Exception {
        runMultilingualQuery(queryName, locale, query, voice);
    }

    /**
     * Logs in as Rohit Mehta (CIF202602260005, 9898989898) rather than a fresh random-phone
     * registration — stage no longer accepts unregistered numbers at the OTP step (same issue
     * fixed in UI11_VoiceRegistrationAuthTest). He has everything every row here needs: dual
     * account for balance/transaction queries, a real beneficiary for the transfer rows, and a
     * loan for the loan/EMI rows.
     * <p>
     * Unlike voice registration, language can be changed at any time via the home globe icon
     * (see HomePage#clickLanguageButton, confirmed in UI6_HomePageTest) — not just once during
     * onboarding — so this doesn't need a cleanup step the way UI11 does. On his first-ever login
     * the onboarding LanguagePage/VoiceRegistrationPage screens appear and are walked normally;
     * on every later run (returning user, those screens skipped) it lands on Home directly.
     * <p>
     * The account's locale is persisted server-side, not per-browser/per-device — confirmed
     * manually (logging in from a different device never re-shows the language picker once a
     * locale has been selected), and consistent with the app recognizing this account as a
     * returning user at all despite every row here launching a completely fresh browser context
     * with no shared cookies/localStorage. So the globe-icon reselect is only actually needed when
     * this row's locale differs from {@link #lastConfirmedLocale} — reselecting unconditionally on
     * every row (the previous behavior here) was redundant UI work with no basis in how the app
     * actually behaves, and each unnecessary reselect is one more chance to race or drop the
     * session on an already-flaky stage backend.
     */
    private HomePage loginAndSetLocale(String queryName, String locale) throws Exception {
        WelcomePage welcomePage = new WelcomePage(page, Endpoints.getUiBaseUrl());
        welcomePage.navigate();
        welcomePage.dismissPwaPopupIfPresent();
        welcomePage.enterPhoneNumber("9898989898");
        welcomePage.clickSendOtp();

        OtpPage otpPage = new OtpPage(page);
        otpPage.waitForPageLoad();
        otpPage.enterOtp(OtpPage.getTestOtp());
        otpPage.clickContinue();

        LanguagePage languagePage = new LanguagePage(page);
        HomePage homePage;
        try {
            languagePage.waitForPageLoad();
            languagePage.selectByLocale(locale);
            languagePage.waitForLocaleSelected(locale);
            languagePage.clickContinue();

            VoiceRegistrationPage voicePage = new VoiceRegistrationPage(page);
            voicePage.waitForPageLoad();
            voicePage.clickSkipForNow();

            homePage = new HomePage(page);
            homePage.waitForPageLoad();
            lastConfirmedLocale = locale;
            justSwitchedLocale = true;
            System.out.println("[Locale] First-ever login for this account — onboarding picker shown, set to '" + locale + "'");
            waitForAndAssertWelcomeMessage(homePage, queryName, "first-login");
        } catch (PlaywrightException returningUser) {
            // Onboarding screens absent for a returning user — but NOT the "Enable Voice
            // Banking" consent screen itself: unlike LanguagePage/VoiceRegistrationPage in the
            // try block above (skipped once, only during first-ever onboarding), this consent
            // screen reappears on every fresh browser session for any account that hasn't
            // completed real voice registration — every account this class uses, since real
            // registration is only ever performed by UI5/UI11. BaseVoiceTest#login expects and
            // skips it completely unconditionally on every single login for exactly this reason;
            // confirmed live here too — the very first landing after OTP, before any language
            // switch, showed this screen instead of Home, not just after switching locale (an
            // earlier version of this fix only handled the post-switch case and still timed out
            // 5/5 on this first one instead). Land on Home directly first, every time.
            homePage = new HomePage(page);
            skipVoiceRegistrationIfPresent();
            homePage.waitForPageLoad();
            waitForAndAssertWelcomeMessage(homePage, queryName, "returning-user-initial");

            if (!locale.equals(lastConfirmedLocale)) {
                // Only reselect via the globe icon when this row's locale actually differs from
                // what the account was last confirmed to have — see the class javadoc on
                // lastConfirmedLocale for why matching this row's locale needs no UI action at
                // all, same as a human never seeing the picker again once already selected.
                // Waiting for the welcome message above, before this decision, is deliberate: we
                // only ever want to act on the globe icon once the initial login has genuinely
                // finished settling, not race a switch attempt against a page still arriving.
                System.out.println("[Locale] Switching: was '" + lastConfirmedLocale + "', row needs '" + locale + "' — reselecting via globe icon");
                homePage.clickLanguageButton();
                languagePage.waitForPageLoad();
                // Wait for the language picker (flags/options) to be fully settled before
                // clicking a locale button — languagePage.waitForPageLoad() only confirms the
                // English button is visible, not that the picker has finished rendering.
                page.waitForTimeout(2000);
                languagePage.selectByLocale(locale);
                languagePage.waitForLocaleSelected(locale);
                languagePage.clickContinue();

                // Wait after submitting the locale change before checking what's next — gives the
                // app time to actually apply the new locale server-side rather than racing ahead
                // while it's still mid-transition (the likely cause of a stale-language welcome
                // message showing up after a switch).
                page.waitForTimeout(3000);

                // The consent screen can reappear again here too, after the locale switch itself —
                // same underlying cause as above, just triggered a second time by this navigation
                // instead of the initial login. Tolerates it not reappearing as the expected case
                // (e.g. re-selecting the locale already active).
                skipVoiceRegistrationIfPresent();

                // Confirmed live via screenshot: the session can drop right around the switch
                // itself, landing on the "Session Ended" screen instead of Home — which shows a
                // *different* button (listening-reconnect-btn) than what waitForPageLoad() below
                // waits for (listening-hold-to-speak-btn), so without recovering first that wait
                // just times out hard after 60s with no chance to recover, since the only other
                // recoverFromSessionEndedIfPresent() call in this method was still below this
                // point. Recovering here first means waitForPageLoad() only ever has to wait for
                // a page that's actually able to reach Home.
                homePage.recoverFromSessionEndedIfPresent();
                homePage.waitForPageLoad();
                lastConfirmedLocale = locale;
                justSwitchedLocale = true;

                // A locale switch clears the chat area without posting a fresh welcome bubble —
                // confirmed visually via the failure screenshots (Home fully loaded, mic ready,
                // chat area empty). No amount of waiting here fixes that, since the bubble simply
                // never arrives — the actual fix is runMultilingualQuery calling
                // holdToSpeakWithRetry's 4-arg overload with waitForExistingWelcomeBubble=false
                // for this row. This settle just lets the click-through finish rendering.
                homePage.recoverFromSessionEndedIfPresent();
                page.waitForTimeout(2000);
                checkWelcomeMessage(homePage, queryName, "post-switch");
            } else {
                System.out.println("[Locale] Already '" + locale + "' — skipping globe icon, going straight to Home");
            }
        }
        return homePage;
    }

    /** Clicks past the "Enable Voice Banking" consent screen if it's currently showing, tolerant
     * of it not being there — see the two call sites in {@link #loginAndSetLocale} for why this
     * can legitimately appear at either point. */
    private void skipVoiceRegistrationIfPresent() {
        try {
            VoiceRegistrationPage voicePage = new VoiceRegistrationPage(page);
            voicePage.waitForPageLoad();
            voicePage.clickSkipForNow();
        } catch (PlaywrightException notShowing) {
            // Already on Home — nothing to skip.
        }
    }

    /** Checks and logs, without waiting or asserting, whether a welcome/bot message is present in
     * the chat right after landing on Home — used only for the "post-switch" landing point, where
     * a missing message is already known-expected (a locale switch clears the chat and never
     * posts a fresh greeting), so this stays an observation there, never a failure. Recorded into
     * {@link #WELCOME_MESSAGE_LOG} so the pattern across the whole run is visible in
     * {@link #logWelcomeMessageSummary}. See {@link #waitForAndAssertWelcomeMessage} for the two
     * landing points where a missing welcome message is a real failure instead. */
    private void checkWelcomeMessage(HomePage homePage, String queryName, String context) {
        boolean present = homePage.isWelcomeMessageVisible();
        System.out.println("[Welcome] " + queryName + " [" + context + "]: message "
                + (present ? "present" : "ABSENT"));
        WELCOME_MESSAGE_LOG.add(queryName + " [" + context + "]: " + (present ? "present" : "ABSENT"));
        if (!present) {
            WelcomeMessageTracker.recordOccurrence(queryName, context);
        }
    }

    /** Waits up to 15s for the welcome message to appear right after login ("first-login" or
     * "returning-user-initial"), then fails the row if it never does — the account's welcome
     * greeting is expected reliably at login itself, unlike right after a locale switch (see
     * {@link #checkWelcomeMessage}, used there instead, where absence is already known-expected).
     * Called before any globe-icon language decision is made, so a switch never races ahead of a
     * login that hasn't genuinely settled yet. Recorded into {@link #WELCOME_MESSAGE_LOG} the same
     * way as {@link #checkWelcomeMessage}, so both show up together in the run summary. */
    private void waitForAndAssertWelcomeMessage(HomePage homePage, String queryName, String context) {
        boolean present = homePage.waitForWelcomeMessage(15000);
        System.out.println("[Welcome] " + queryName + " [" + context + "]: message "
                + (present ? "present" : "ABSENT"));
        WELCOME_MESSAGE_LOG.add(queryName + " [" + context + "]: " + (present ? "present" : "ABSENT"));
        assertOrCapture(present, queryName,
                "[" + queryName + "] Welcome message did not appear within 15s after login (" + context + ")");
    }

    /** Prints a full summary of every {@link #checkWelcomeMessage} observation from this run,
     * plus counts by landing-point context, once all rows have finished. */
    @AfterClass(alwaysRun = true)
    public void logWelcomeMessageSummary() {
        System.out.println("[Welcome] ===== Summary (" + WELCOME_MESSAGE_LOG.size() + " checks) =====");
        for (String entry : WELCOME_MESSAGE_LOG) {
            System.out.println("[Welcome]   " + entry);
        }
        for (String context : List.of("first-login", "returning-user-initial", "post-switch")) {
            long total = WELCOME_MESSAGE_LOG.stream().filter(e -> e.contains("[" + context + "]")).count();
            long absent = WELCOME_MESSAGE_LOG.stream()
                    .filter(e -> e.contains("[" + context + "]") && e.endsWith("ABSENT")).count();
            if (total > 0) {
                System.out.println("[Welcome] " + context + ": " + absent + "/" + total + " absent");
            }
        }
    }

    /** Cap on re-asking the same query when the bot comes back blank — mirrors
     * BaseVoiceTest#MAX_REASK_ATTEMPTS. This class can't reuse BaseVoiceTest's own
     * isGenericGreeting()/isContextLostFallback() checks alongside the blank check, since both are
     * English-only regexes ("Welcome...How can I help you today", "didn't understand
     * that...what would you like to do") that would never match Hindi/Bengali text — only the
     * blank check is language-agnostic, so that's the only re-ask trigger ported here. */
    private static final int MAX_REASK_ATTEMPTS = 3;

    /** How long to wait for the bot's reply before treating it as stuck — mirrors
     * BaseVoiceTest#BOT_RESPONSE_TIMEOUT_MS (raised from an earlier 15 s after live runs showed
     * genuine replies still arriving past that mark). This class previously used a bare 15 s with
     * no re-ask at all, which — confirmed live via UI12_MultilingualVoiceQueryTest#testLocaleSwitchCheck,
     * 2/2 rows failing on a blank response despite the locale switch itself working correctly —
     * gave every row far less tolerance for a slow reply than every BaseVoiceTest-derived class
     * gets automatically. */
    private static final int BOT_RESPONSE_TIMEOUT_MS = 30000;

    private void runMultilingualQuery(String queryName, String locale, String query, String voice) throws Exception {
        justSwitchedLocale = false;
        HomePage homePage = loginAndSetLocale(queryName, locale);
        homePage.setCurrentQueryName(queryName);

        int holdMs = (int) TtsUtil.getWavDurationMs(audioPath);
        homePage.holdToSpeakWithRetry(holdMs, 3, 8000, !justSwitchedLocale);
        homePage.waitForVoiceResponse(BOT_RESPONSE_TIMEOUT_MS);

        String transcribed = homePage.getLastTranscribedText();
        String botResponse = homePage.getLastBotResponse();

        System.out.println("[" + queryName + "] Query        : " + query);
        System.out.println("[" + queryName + "] Transcribed  : " + transcribed);
        System.out.println("[" + queryName + "] Bot response : " + botResponse);

        for (int reaskNum = 1; reaskNum <= MAX_REASK_ATTEMPTS && botResponse.isBlank(); reaskNum++) {
            // Recorded here — not only if every retry below eventually fails — so a no-response
            // that self-heals on a later re-ask still shows up in the stability signal, the same
            // way SessionEndedTracker counts a recovered session drop (see BaseVoiceTest's
            // identical re-ask loop for the pattern this mirrors).
            NoResponseTracker.recordOccurrence(queryName);
            System.out.println("[" + queryName + "] WARN — bot did not respond (stuck Processing, or"
                    + " post-reconnect) — re-asking (" + reaskNum + " of " + MAX_REASK_ATTEMPTS + ")...");
            homePage.holdToSpeakWithRetry(holdMs, 3, 8000, !justSwitchedLocale);
            homePage.waitForVoiceResponse(BOT_RESPONSE_TIMEOUT_MS);
            transcribed = homePage.getLastTranscribedText();
            botResponse = homePage.getLastBotResponse();
            System.out.println("[" + queryName + "] Re-ask " + reaskNum + " Transcribed : " + transcribed);
            System.out.println("[" + queryName + "] Re-ask " + reaskNum + " Bot response : " + botResponse);
        }

        long reviewPauseMs = Long.parseLong(System.getProperty("reviewPauseMs", "0"));
        if (reviewPauseMs > 0) {
            Thread.sleep(reviewPauseMs);
        }

        assertOrCapture(homePage.isPageVisible(), queryName, "[" + queryName + "] Home page should remain visible after voice query");
        assertOrCapture(!transcribed.isEmpty(), queryName, "[" + queryName + "] Voice not recognised — no user message appeared in chat");
        assertOrCapture(!botResponse.isEmpty(), queryName, "[" + queryName + "] Bot did not respond after voice query");
    }

    /** Asserts {@code condition}, capturing a screenshot at this exact instant first if it's
     * about to fail — see BaseVoiceTest#assertOrCapture for why the teardown screenshot alone
     * isn't reliable for a transient failure like a dropped-then-reconnected voice session. */
    private void assertOrCapture(boolean condition, String queryName, String message) {
        if (!condition) {
            ScreenshotUtil.captureNow(page, getClass().getSimpleName(), queryName);
        }
        Assert.assertTrue(condition, message);
    }
}
