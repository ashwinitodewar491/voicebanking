package com.voicebanking.tests.ui.base;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.voicebanking.DataText.BotResponsePatterns;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.DataText.VoiceQueries;
import com.voicebanking.pages.HomePage;
import com.voicebanking.pages.LanguagePage;
import com.voicebanking.pages.OtpPage;
import com.voicebanking.pages.VoiceRegistrationPage;
import com.voicebanking.pages.WelcomePage;
import com.voicebanking.utils.TtsUtil;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Abstract base for all voice-based UI tests. Handles browser lifecycle and the core voice query flow. */
public abstract class BaseVoiceTest {

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;
    protected String currentAudioPath;

    /**
     * Generates a WAV file for the test query, then launches a Chromium browser with
     * {@code --use-file-for-fake-audio-capture} pointing at that file so the browser
     * treats it as microphone input.
     */
    @BeforeMethod(alwaysRun = true)
    public void setUpBrowserWithAudio(Object[] params) throws Exception {
        String query = (String) params[1];
        currentAudioPath = TtsUtil.generateWav(query);

        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "false"));

        List<String> chromiumArgs = new ArrayList<>();
        chromiumArgs.add("--disable-gpu");               // required on Windows CI — without this Chromium hangs on GPU init when there is no display
        chromiumArgs.add("--use-fake-device-for-media-stream");
        chromiumArgs.add("--use-fake-ui-for-media-stream");
        chromiumArgs.add("--use-file-for-fake-audio-capture=" + currentAudioPath);

        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setArgs(chromiumArgs));
        context = browser.newContext(
                new Browser.NewContextOptions()
                        .setPermissions(List.of("microphone")));
        page = context.newPage();

        page.onConsoleMessage(msg -> {
            if ("error".equals(msg.type())) System.out.println("[Browser] error: " + msg.text());
        });
    }

    /**
     * Captures a screenshot on test failure, then closes all browser resources and
     * deletes the temporary WAV file. A short sleep paces consecutive test runs.
     */
    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) throws InterruptedException {
        if (result.getStatus() == ITestResult.FAILURE && page != null && !page.isClosed()) {
            try {
                java.io.File dir = new java.io.File("target/screenshots");
                dir.mkdirs();
                String label = result.getParameters().length > 0
                        ? result.getParameters()[0].toString().replaceAll("[^a-zA-Z0-9_-]", "_")
                        : result.getMethod().getMethodName();
                String screenshotPath = "target/screenshots/"
                        + result.getTestClass().getRealClass().getSimpleName()
                        + "_" + label + "_" + System.currentTimeMillis() + ".png";
                page.screenshot(new Page.ScreenshotOptions().setPath(Path.of(screenshotPath)));
                System.out.println("[Screenshot] " + screenshotPath);
            } catch (Exception e) {
                System.out.println("[Screenshot] Failed to capture: " + e.getMessage());
            }
        }

        if (page != null)       page.close();
        if (context != null)    context.close();
        if (browser != null)    browser.close();
        if (playwright != null) playwright.close();

        TtsUtil.deleteWav(currentAudioPath);
        Thread.sleep(3000);
    }

    /** Navigates, logs in, speaks the query, retries on transcription mismatch, and asserts the bot response. */
    protected void runVoiceQuery(String queryName, String query,
                                  String[] expectedKeywords, String assertionPattern) throws Exception {

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
        try {
            languagePage.waitForPageLoad();
            languagePage.selectByLocale(VoiceQueries.English.LOCALE);
            languagePage.clickContinue();
        } catch (PlaywrightException ignored) {
            // language page absent for returning users
        }

        VoiceRegistrationPage voicePage = new VoiceRegistrationPage(page);
        voicePage.waitForPageLoad();
        voicePage.clickSkipForNow();

        HomePage homePage = new HomePage(page);
        homePage.waitForPageLoad();

        int holdMs = (int) TtsUtil.getWavDurationMs(currentAudioPath);
        System.out.println("[" + queryName + "] WAV duration : " + holdMs + " ms (speech + 3 s silence)");

        homePage.holdToSpeakWithRetry(holdMs, 3, 8000);
        homePage.waitForVoiceResponse(15000);

        String transcribed = homePage.getLastTranscribedText();
        String botResponse = homePage.getLastBotResponse();

        System.out.println("[" + queryName + "] Expected    : " + query);
        System.out.println("[" + queryName + "] Transcribed : " + transcribed);
        System.out.println("[" + queryName + "] Bot response: " + botResponse);

        Assert.assertFalse(transcribed.isEmpty(),
                "[" + queryName + "] Voice not recognised — no user message appeared in chat");
        Assert.assertFalse(botResponse.isEmpty(),
                "[" + queryName + "] Bot did not respond after voice query");
        Assert.assertTrue(homePage.isPageVisible(),
                "[" + queryName + "] Home page should remain visible after voice query");

        for (int retryNum = 1; retryNum <= 2 && !transcriptionContainsExpectedWords(transcribed, query); retryNum++) {
            System.out.println("[" + queryName + "] WARN — transcription mismatch (retry " + retryNum + "):"
                    + " expected [" + query + "] got [" + transcribed + "] — retrying...");
            homePage.holdToSpeakWithRetry(holdMs, 3, 8000);
            homePage.waitForVoiceResponse(15000);
            transcribed = homePage.getLastTranscribedText();
            botResponse = homePage.getLastBotResponse();
            System.out.println("[" + queryName + "] Retry " + retryNum + " Transcribed : " + transcribed);
            System.out.println("[" + queryName + "] Retry " + retryNum + " Bot response: " + botResponse);
        }

        Assert.assertTrue(transcriptionContainsExpectedWords(transcribed, query),
                "[" + queryName + "] Transcription mismatch after retries:"
                + " expected [" + query + "] got [" + transcribed + "]");

        if (isAccountDisambiguation(botResponse)) {
            System.out.println("[" + queryName + "] Bot asked to choose account — following up with 'savings'...");

            long oldAudioDurationMs = TtsUtil.getWavDurationMs(currentAudioPath);
            String followUpPath = TtsUtil.generateWav("savings");
            Files.copy(Path.of(followUpPath), Path.of(currentAudioPath), StandardCopyOption.REPLACE_EXISTING);
            int followUpHoldMs = (int) TtsUtil.getWavDurationMs(currentAudioPath);
            TtsUtil.deleteWav(followUpPath);

            int preWaitMs = (int) oldAudioDurationMs + 2000;
            homePage.speakFollowUp(preWaitMs, followUpHoldMs, 8000);
            homePage.waitForVoiceResponse(15000);

            String followUpTranscribed = homePage.getLastTranscribedText();
            String followUpResponse    = homePage.getLastBotResponse();
            System.out.println("[" + queryName + "] Follow-up Transcribed : " + followUpTranscribed);
            System.out.println("[" + queryName + "] Follow-up Bot response: " + followUpResponse);

            boolean matched = Pattern.compile(BotResponsePatterns.Balance.SAVINGS)
                                     .matcher(followUpResponse).find();
            Assert.assertTrue(matched,
                    "[" + queryName + "] After account selection, expected SAVINGS balance.\n"
                    + "  Pattern : " + BotResponsePatterns.Balance.SAVINGS + "\n"
                    + "  Got     : " + followUpResponse);
            System.out.println("[" + queryName + "] PASS");
            return;
        }

        if (assertionPattern != null) {
            boolean matched = Pattern.compile(assertionPattern).matcher(botResponse).find();
            Assert.assertTrue(matched,
                    "[" + queryName + "] Bot response did not match expected pattern.\n"
                    + "  Pattern : " + assertionPattern + "\n"
                    + "  Got     : " + botResponse);
        } else {
            Assert.assertTrue(containsAnyKeyword(botResponse, expectedKeywords),
                    "[" + queryName + "] Bot response not relevant.\n  Expected keywords: "
                    + String.join(", ", expectedKeywords)
                    + "\n  Actual response : " + botResponse);
        }

        System.out.println("[" + queryName + "] PASS");
    }

    /** Returns true when every word in {@code expected} appears in {@code transcribed} (case-insensitive). */
    private boolean transcriptionContainsExpectedWords(String transcribed, String expected) {
        String cleanTranscribed = transcribed.replaceAll("[^a-zA-Z0-9 ]", "").toLowerCase();
        String[] expectedWords  = expected.replaceAll("[^a-zA-Z0-9 ]", "").toLowerCase().split("\\s+");
        for (String word : expectedWords) {
            if (!cleanTranscribed.contains(word)) return false;
        }
        return true;
    }

    /** Returns true when the bot response is asking the user to choose between account types. */
    private boolean isAccountDisambiguation(String response) {
        String lower = response.toLowerCase();
        return lower.contains("choose") && lower.contains("account");
    }

    /** Returns true when at least one keyword appears in the bot response text. */
    protected boolean containsAnyKeyword(String text, String[] keywords) {
        String lower = text.toLowerCase();
        for (String kw : keywords) {
            if (kw.length() > 1 && lower.contains(kw.toLowerCase())) return true;
        }
        return false;
    }
}
