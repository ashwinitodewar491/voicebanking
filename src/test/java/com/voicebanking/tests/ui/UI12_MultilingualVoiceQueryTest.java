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

/** Hindi and Bengali voice queries across balance, transactions, transfer, loan and EMI. Standalone
 * because BaseVoiceTest is English-only. */
public class UI12_MultilingualVoiceQueryTest {

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;
    private String audioPath;

    /** Locale last confirmed for this account, so rows with the same locale skip reselecting it. */
    private String lastConfirmedLocale;

    /** True when this row just switched locale; the switch clears the chat, so don't wait for a
     * welcome bubble. */
    private boolean justSwitchedLocale;

    /** Welcome-message observations across the whole run, printed by
     * {@link #logWelcomeMessageSummary}. */
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

    /** Fast health-check tier: one query per feature, alternating Hindi and Bengali. */
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

    /** TEMP: two consecutive locale switches (to Hindi, then Bengali). Only run via
     * -Dtest=...#testLocaleSwitchCheck. Remove after use. */
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

    /** Logs in as Rohit Mehta and sets {@code locale} via the globe icon only if it differs from
     * {@link #lastConfirmedLocale}; locale persists server-side. */
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
            // Set before waitForPageLoad/any recovery below — currentQueryName defaults to
            // "unknown" (see HomePage#currentQueryName) and runMultilingualQuery doesn't set the
            // real one until after this method returns, so a session drop caught during login
            // itself would otherwise show up in SessionEndedTracker's detail log attributed to
            // "unknown" instead of this row.
            homePage.setCurrentQueryName(queryName);
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
            // Same reasoning as the first-login branch above — set before skipVoiceRegistrationIfPresent
            // and the recoverFromSessionEndedIfPresent() calls further down, so a drop during this
            // row's login/locale-switch attributes correctly instead of showing "unknown".
            homePage.setCurrentQueryName(queryName);
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

    /** Clicks past the voice-registration consent screen if it's showing. */
    private void skipVoiceRegistrationIfPresent() {
        try {
            VoiceRegistrationPage voicePage = new VoiceRegistrationPage(page);
            voicePage.waitForPageLoad();
            voicePage.clickSkipForNow();
        } catch (PlaywrightException notShowing) {
            // Already on Home — nothing to skip.
        }
    }

    /** Logs (without asserting) whether a welcome message is present after a locale switch, where
     * its absence is expected. */
    private void checkWelcomeMessage(HomePage homePage, String queryName, String context) {
        boolean present = homePage.isWelcomeMessageVisible();
        System.out.println("[Welcome] " + queryName + " [" + context + "]: message "
                + (present ? "present" : "ABSENT"));
        WELCOME_MESSAGE_LOG.add(queryName + " [" + context + "]: " + (present ? "present" : "ABSENT"));
        if (!present) {
            WelcomeMessageTracker.recordOccurrence(queryName, context);
        }
    }

    /** Waits up to 15s for the welcome message after login and fails the row if it never appears. */
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

    /** Max re-asks on a blank response. Only the blank check is ported from BaseVoiceTest; its
     * greeting/fallback patterns are English-only. */
    private static final int MAX_REASK_ATTEMPTS = 3;

    /** How long to wait for a bot reply before treating it as stuck (matches BaseVoiceTest). */
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

    /** Asserts {@code condition}, screenshotting immediately on failure (see
     * BaseVoiceTest#assertOrCapture). */
    private void assertOrCapture(boolean condition, String queryName, String message) {
        if (!condition) {
            ScreenshotUtil.captureNow(page, getClass().getSimpleName(), queryName);
        }
        Assert.assertTrue(condition, message);
    }
}
