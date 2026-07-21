package com.voicebanking.tests.ui.base;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.voicebanking.DataText.BotResponsePatterns;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.DataText.VoiceQueries;
import com.voicebanking.pages.HomePage;
import com.voicebanking.pages.LanguagePage;
import com.voicebanking.pages.OtpPage;
import com.voicebanking.pages.VoiceRegistrationPage;
import com.voicebanking.pages.WelcomePage;
import com.voicebanking.utils.ScreenshotUtil;
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

        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));

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

        // --use-file-for-fake-audio-capture is read once per audio capture stream, not once per
        // browser launch — but if the app opens the mic once and reuses that same stream across
        // every hold-to-speak press, only the *first* stream's buffered audio ever gets played,
        // no matter how many times the underlying WAV file is overwritten for a follow-up
        // utterance. This init script tracks every stream handed out so a follow-up can force
        // them all to stop, making the app (if it behaves reasonably) request a fresh one — which
        // should pick up whatever is currently on disk. Must be registered before the first
        // navigate() to apply.
        page.addInitScript(
                "(function() {"
                + "  window.__micStreams = [];"
                + "  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) return;"
                + "  var original = navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices);"
                + "  navigator.mediaDevices.getUserMedia = function(constraints) {"
                + "    return original(constraints).then(function(stream) {"
                + "      window.__micStreams.push(stream);"
                + "      return stream;"
                + "    });"
                + "  };"
                + "})();");

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
        ScreenshotUtil.captureOnFailure(page, result);

        if (page != null)       page.close();
        if (context != null)    context.close();
        if (browser != null)    browser.close();
        if (playwright != null) playwright.close();

        TtsUtil.deleteWav(currentAudioPath);
        Thread.sleep(3000);
    }

    /**
     * Phone number used to log in before speaking the query. Defaults to a fresh random number
     * (new-user registration flow, no seeded history). Override to log in as an existing seeded
     * customer instead — e.g. when the query needs real transaction/loan history to assert against,
     * since a brand-new account has none.
     */
    protected String getLoginPhoneNumber() {
        return WelcomePage.generateRandomPhone();
    }

    /** Navigates, logs in, speaks the query, retries on transcription mismatch, and asserts the bot response. */
    protected void runVoiceQuery(String queryName, String query, String[] expectedKeywords,
                                  String assertionPattern, String disambiguationAccount) throws Exception {
        runVoiceQuery(queryName, query, expectedKeywords, assertionPattern, disambiguationAccount, getLoginPhoneNumber());
    }

    /** Same as {@link #runVoiceQuery(String, String, String[], String, String)} but logs in as
     * an explicit phone number instead of {@link #getLoginPhoneNumber()} — for data-provider rows
     * that exercise a specific known seeded customer rather than the class-wide default. */
    protected void runVoiceQuery(String queryName, String query, String[] expectedKeywords,
                                  String assertionPattern, String disambiguationAccount,
                                  String phoneNumber) throws Exception {

        WelcomePage welcomePage = new WelcomePage(page, Endpoints.getUiBaseUrl());
        welcomePage.navigate();
        welcomePage.dismissPwaPopupIfPresent();
        welcomePage.enterPhoneNumber(phoneNumber);
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

        for (int retryNum = 1; retryNum <= 2
                && !transcriptionContainsExpectedWords(transcribed, query)
                && !shouldStopRetrying(botResponse); retryNum++) {
            System.out.println("[" + queryName + "] WARN — transcription mismatch (retry " + retryNum + "):"
                    + " expected [" + query + "] got [" + transcribed + "] — retrying...");
            homePage.holdToSpeakWithRetry(holdMs, 3, 8000);
            homePage.waitForVoiceResponse(15000);
            transcribed = homePage.getLastTranscribedText();
            botResponse = homePage.getLastBotResponse();
            System.out.println("[" + queryName + "] Retry " + retryNum + " Transcribed : " + transcribed);
            System.out.println("[" + queryName + "] Retry " + retryNum + " Bot response: " + botResponse);
        }

        // If the bot's response already indicates a recognized in-flow state (account/loan
        // disambiguation, transfer confirmation, ...), don't hard-fail just because this
        // particular utterance's transcription didn't literally match the original query text —
        // the conversation has moved on, and re-playing the same original-query audio again
        // would risk it being misheard as an answer to a *different* question than the one
        // actually being asked now, silently skipping past the follow-up handling below.
        if (!shouldStopRetrying(botResponse)) {
            Assert.assertTrue(transcriptionContainsExpectedWords(transcribed, query),
                    "[" + queryName + "] Transcription mismatch after retries:"
                    + " expected [" + query + "] got [" + transcribed + "]");
        }

        if (isAccountDisambiguation(botResponse)) {
            String followUpAccount = disambiguationAccount != null ? disambiguationAccount : "savings";
            String expectedPattern = "current".equalsIgnoreCase(followUpAccount)
                    ? BotResponsePatterns.Balance.CURRENT
                    : BotResponsePatterns.Balance.SAVINGS;

            String followUpTranscribed = "";
            String followUpResponse = "";

            for (int attempt = 1; attempt <= 3; attempt++) {
                System.out.println("[" + queryName + "] Bot asked to choose account — following up with '"
                        + followUpAccount + "' (attempt " + attempt + ")...");

                long oldAudioDurationMs = TtsUtil.getWavDurationMs(currentAudioPath);
                String followUpPath = TtsUtil.generateWav(followUpAccount);
                Files.copy(Path.of(followUpPath), Path.of(currentAudioPath), StandardCopyOption.REPLACE_EXISTING);
                int followUpHoldMs = (int) TtsUtil.getWavDurationMs(currentAudioPath);
                TtsUtil.deleteWav(followUpPath);

                homePage.reacquireMicrophoneForFollowUp();

                // Extra wait per retry — gives the freshly reacquired mic stream (and the fake
                // audio device behind it) time to settle before speaking.
                int preWaitMs = (int) oldAudioDurationMs + 2000 + ((attempt - 1) * 2000);
                homePage.speakFollowUp(preWaitMs, followUpHoldMs, 8000);
                homePage.waitForVoiceResponse(15000);

                followUpTranscribed = homePage.getLastTranscribedText();
                followUpResponse    = homePage.getLastBotResponse();
                System.out.println("[" + queryName + "] Follow-up Transcribed : " + followUpTranscribed);
                System.out.println("[" + queryName + "] Follow-up Bot response: " + followUpResponse);

                boolean heardExpectedAccount =
                        followUpTranscribed.toLowerCase().contains(followUpAccount.toLowerCase());
                if (heardExpectedAccount && !isAccountDisambiguation(followUpResponse)) {
                    break;
                }
                System.out.println("[" + queryName + "] WARN — follow-up not recognised (attempt " + attempt
                        + "): expected [" + followUpAccount + "] got transcribed [" + followUpTranscribed
                        + "], bot [" + followUpResponse + "] — retrying...");
            }

            boolean matched = Pattern.compile(expectedPattern)
                                     .matcher(followUpResponse).find();
            Assert.assertTrue(matched,
                    "[" + queryName + "] After account selection, expected " + followUpAccount.toUpperCase()
                    + " balance.\n"
                    + "  Pattern : " + expectedPattern + "\n"
                    + "  Got     : " + followUpResponse);
            System.out.println("[" + queryName + "] PASS");
            return;
        }

        // Extension point for test classes whose queries need follow-up handling beyond generic
        // savings/current account disambiguation — e.g. loan-type disambiguation (UI9) or the
        // transfer confirm/OTP/beneficiary flow (UI10). Default is a no-op; each owning test
        // class overrides this itself rather than growing this shared base with logic only it
        // uses, since string-matching heuristics for one feature can misfire on another's
        // responses (this happened once already between account- and loan-disambiguation).
        botResponse = handleAdditionalFollowUp(queryName, botResponse, disambiguationAccount, homePage);

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

    /** Returns true when {@code botResponse} already indicates a recognized in-flow state that
     * the transcription-retry loop should stop trying to power through — see the call site in
     * {@link #runVoiceQuery(String, String, String[], String, String, String)} for why replaying
     * the original query's audio at that point is actively risky rather than merely wasteful.
     * Default covers account disambiguation, since that's shared by UI7/UI8. A test class adding
     * its own recognized states (loan disambiguation, transfer confirmation, ...) should call
     * {@code super.shouldStopRetrying(botResponse)} too, not replace it. */
    protected boolean shouldStopRetrying(String botResponse) {
        return isAccountDisambiguation(botResponse);
    }

    /** Extension point — see call site in {@link #runVoiceQuery(String, String, String[], String,
     * String, String)}. Override in a test class to handle a follow-up prompt specific to that
     * class's feature; return {@code botResponse} unchanged when there's nothing to do. */
    protected String handleAdditionalFollowUp(String queryName, String botResponse,
                                               String disambiguationAccount, HomePage homePage) throws Exception {
        return botResponse;
    }

    /** Returns true when every word in {@code expected} appears in {@code transcribed}
     * (case-insensitive). Numbers are normalized to digits first — STT is inconsistent about
     * rendering a spoken number as the word ("one") or the digit ("1"), sometimes doing either
     * for the exact same audio, so comparing raw text made a perfectly correct transcription
     * fail whenever it didn't happen to pick the same form as the expected text. */
    protected boolean transcriptionContainsExpectedWords(String transcribed, String expected) {
        String cleanTranscribed = normalizeNumberWords(transcribed.replaceAll("[^a-zA-Z0-9 ]", "").toLowerCase());
        String[] expectedWords  = normalizeNumberWords(expected.replaceAll("[^a-zA-Z0-9 ]", "").toLowerCase())
                .split("\\s+");
        for (String word : expectedWords) {
            if (!cleanTranscribed.contains(word)) return false;
        }
        return true;
    }

    private static final java.util.Map<String, String> NUMBER_WORDS = java.util.Map.ofEntries(
            java.util.Map.entry("zero", "0"), java.util.Map.entry("one", "1"),
            java.util.Map.entry("two", "2"), java.util.Map.entry("three", "3"),
            java.util.Map.entry("four", "4"), java.util.Map.entry("five", "5"),
            java.util.Map.entry("six", "6"), java.util.Map.entry("seven", "7"),
            java.util.Map.entry("eight", "8"), java.util.Map.entry("nine", "9"),
            java.util.Map.entry("ten", "10"));

    /** Replaces spelled-out digits 0–10 with their numeral form, word by word, so "one" and "1"
     * compare equal after normalization. */
    private String normalizeNumberWords(String text) {
        String[] words = text.split("\\s+");
        StringBuilder normalized = new StringBuilder();
        for (String word : words) {
            normalized.append(NUMBER_WORDS.getOrDefault(word, word)).append(' ');
        }
        return normalized.toString().trim();
    }

    /** Returns true when the bot response is asking the user to choose between account types.
     * Excludes loan-related responses — "which loan account would you like to check" is asking
     * the caller to pick a loan, not a savings/current account, and the savings/current follow-up
     * mechanism below doesn't know how to answer that. */
    private boolean isAccountDisambiguation(String response) {
        String lower = response.toLowerCase();
        if (lower.contains("loan")) return false;
        boolean asksToChoose = (lower.contains("choose") || lower.contains("which")) && lower.contains("account");
        boolean listsBothAccounts = lower.contains("current") && lower.contains("savings") && lower.contains("account");
        return asksToChoose || listsBothAccounts;
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
