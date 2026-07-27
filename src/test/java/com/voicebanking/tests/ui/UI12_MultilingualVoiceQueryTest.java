package com.voicebanking.tests.ui;

import com.microsoft.playwright.*;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.DataText.MultilingualVoiceQueries;
import com.voicebanking.pages.*;
import com.voicebanking.utils.ScreenshotUtil;
import com.voicebanking.utils.TtsUtil;
import com.voicebanking.utils.tts.EdgeTtsEngine;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

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

    private void runMultilingualQuery(String queryName, String locale, String query, String voice) throws Exception {
        WelcomePage welcomePage = new WelcomePage(page, Endpoints.getUiBaseUrl());
        welcomePage.navigate();
        welcomePage.dismissPwaPopupIfPresent();
        welcomePage.enterPhoneNumber(WelcomePage.generateRandomPhone());
        welcomePage.clickSendOtp();

        OtpPage otpPage = new OtpPage(page);
        otpPage.waitForPageLoad();
        otpPage.enterOtp(OtpPage.getTestOtp());
        otpPage.clickContinue();

        LanguagePage languagePage = new LanguagePage(page);
        languagePage.waitForPageLoad();
        languagePage.selectByLocale(locale);
        languagePage.clickContinue();

        VoiceRegistrationPage voicePage = new VoiceRegistrationPage(page);
        voicePage.waitForPageLoad();
        voicePage.clickSkipForNow();

        HomePage homePage = new HomePage(page);
        homePage.waitForPageLoad();

        int holdMs = (int) TtsUtil.getWavDurationMs(audioPath);
        homePage.holdToSpeakWithRetry(holdMs, 3, 8000);
        homePage.waitForVoiceResponse(15000);

        String transcribed = homePage.getLastTranscribedText();
        String botResponse = homePage.getLastBotResponse();

        System.out.println("[" + queryName + "] Query        : " + query);
        System.out.println("[" + queryName + "] Transcribed  : " + transcribed);
        System.out.println("[" + queryName + "] Bot response : " + botResponse);

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
