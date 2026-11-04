package com.voicebanking.tests.ui.base;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.voicebanking.DataText.BotResponsePatterns;
import com.voicebanking.DataText.Constants;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.DataText.VoiceQueries;
import com.voicebanking.pages.HomePage;
import com.voicebanking.pages.LanguagePage;
import com.voicebanking.pages.OtpPage;
import com.voicebanking.pages.VoiceRegistrationPage;
import com.voicebanking.pages.WelcomePage;
import com.voicebanking.listeners.TestListener;
import com.voicebanking.utils.GroundTruthApi;
import com.voicebanking.utils.NoResponseTracker;
import com.voicebanking.utils.ScreenshotUtil;
import com.voicebanking.utils.TtsUtil;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Base class for voice UI tests: browser lifecycle and the core voice query flow.
 * {@code @Listeners} is declared here because Surefire ignores testng.xml's listeners when run with
 * {@code -Dtest=...}. */
@Listeners(TestListener.class)
public abstract class BaseVoiceTest {

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;
    protected String currentAudioPath;

    /** Fixed fake-mic WAV path in shared-session mode; each query's audio is copied onto it. */
    private String sessionAudioPath;
    private boolean sessionInitialized = false;

    /** Phone number of the currently logged-in customer; null before the first login. */
    protected String loggedInPhoneNumber;

    /** Lazily created ground-truth API client, reused across rows. */
    private GroundTruthApi groundTruthApi;

    protected GroundTruthApi groundTruth() {
        if (groundTruthApi == null) {
            groundTruthApi = new GroundTruthApi(Endpoints.getGroundTruthApiBaseUrl());
        }
        return groundTruthApi;
    }

    /** Seeded customerId for the logged-in phone, or null if unknown. */
    protected String currentCustomerId() {
        return loggedInPhoneNumber == null ? null : Constants.CUSTOMER_ID_BY_PHONE.get(loggedInPhoneNumber);
    }


    /** Override to return true to share one browser/login across all rows. Only safe when rows are
     * independent turns on the same account. */
    protected boolean useSharedSession() {
        return false;
    }

    /** Generates the query WAV and launches a browser with it as the fake mic, or in shared-session
     * mode copies it onto the running session's audio path. */
    @BeforeMethod(alwaysRun = true)
    public void setUpBrowserWithAudio(Object[] params) throws Exception {
        String query = (String) params[1];
        String freshWavPath = TtsUtil.generateWav(query);

        if (useSharedSession() && sessionInitialized) {
            Files.copy(Path.of(freshWavPath), Path.of(sessionAudioPath), StandardCopyOption.REPLACE_EXISTING);
            TtsUtil.deleteWav(freshWavPath);
            currentAudioPath = sessionAudioPath;
            new HomePage(page).reacquireMicrophoneForFollowUp();
            return;
        }

        currentAudioPath = freshWavPath;
        launchBrowser(currentAudioPath);

        if (useSharedSession()) {
            sessionAudioPath = currentAudioPath;
            sessionInitialized = true;
        }
    }

    private void launchBrowser(String audioPath) {
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));

        List<String> chromiumArgs = new ArrayList<>();
        chromiumArgs.add("--disable-gpu");               // required on Windows CI — without this Chromium hangs on GPU init when there is no display
        chromiumArgs.add("--use-fake-device-for-media-stream");
        chromiumArgs.add("--use-fake-ui-for-media-stream");
        chromiumArgs.add("--use-file-for-fake-audio-capture=" + audioPath);

        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setArgs(chromiumArgs));
        // Video is recorded for the lifetime of this context — in useSharedSession() mode that's
        // the whole class's run (one context spans every row, closed once in
        // tearDownSharedSession()), so this yields one continuous video covering the entire
        // regression run rather than one clip per row. The file is only finalized on context
        // close, so it only appears in target/videos/ once the run (or class) finishes. Local-only
        // — target/ is gitignored, nothing here gets pushed.
        context = browser.newContext(
                new Browser.NewContextOptions()
                        .setPermissions(List.of("microphone"))
                        .setRecordVideoDir(Path.of("target/videos")));
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

    /** Screenshots on failure. Closes the browser unless in shared-session mode. */
    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) throws InterruptedException {
        ScreenshotUtil.captureOnFailure(page, result);

        if (useSharedSession()) {
            if (page != null && !page.isClosed()) page.waitForTimeout(1000);
            return;
        }

        if (page != null)       page.close();
        if (context != null)    context.close();
        if (browser != null)    browser.close();
        if (playwright != null) playwright.close();

        TtsUtil.deleteWav(currentAudioPath);
        Thread.sleep(3000);
    }

    /** Closes the shared browser/session once, after the last test method in a
     * {@link #useSharedSession()} class has run. No-op otherwise. */
    @AfterClass(alwaysRun = true)
    public void tearDownSharedSession() {
        if (!useSharedSession() || !sessionInitialized) return;

        if (page != null)       page.close();
        if (context != null)    context.close();
        if (browser != null)    browser.close();
        if (playwright != null) playwright.close();

        TtsUtil.deleteWav(sessionAudioPath);
    }

    /** Login phone number; defaults to Rohit Mehta, a seeded customer with real history. */
    protected String getLoginPhoneNumber() {
        return Constants.EXPECTED_CUSTOMER_MOBILE;
    }

    /** Navigates, logs in, speaks the query, retries on transcription mismatch, and asserts the bot response. */
    protected void runVoiceQuery(String queryName, String query, String[] expectedKeywords,
                                  String assertionPattern, String disambiguationAccount) throws Exception {
        runVoiceQuery(queryName, query, expectedKeywords, assertionPattern, disambiguationAccount, getLoginPhoneNumber());
    }

    /** Logs in as {@code phoneNumber}; in shared-session mode, a no-op if already logged in as that
     * number. */
    private HomePage ensureLoggedIn(String phoneNumber) throws Exception {
        if (useSharedSession() && phoneNumber.equals(loggedInPhoneNumber)) {
            return new HomePage(page);
        }
        return login(phoneNumber);
    }

    /** Full login flow: navigate, OTP, language, skip voice registration. */
    private HomePage login(String phoneNumber) throws Exception {
        // A prior login in this same browser context (shared-session mode, or simply a repeat call)
        // leaves an auth cookie/localStorage session behind. Navigating to /welcome while still
        // authenticated skips straight past the phone screen to Home instead of prompting again,
        // which left the phone-input locator below waiting the full 30 s for an element that was
        // never going to appear. Clearing storage first guarantees a genuinely logged-out /welcome.
        context.clearCookies();
        try {
            page.evaluate("() => { try { localStorage.clear(); } catch (e) {} "
                    + "try { sessionStorage.clear(); } catch (e) {} }");
        } catch (PlaywrightException ignored) {
            // nothing to clear yet on the very first login of the session
        }

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
        boolean returningUser = false;
        try {
            languagePage.waitForPageLoad();
            languagePage.selectByLocale(VoiceQueries.English.LOCALE);
            languagePage.waitForLocaleSelected(VoiceQueries.English.LOCALE);
            languagePage.clickContinue();
        } catch (PlaywrightException ignored) {
            // language page absent for returning users
            returningUser = true;
        }

        // The app no longer auto-shows this screen on every login once an account has skipped it
        // once before — confirmed as an intentional app change, not a bug: registering later
        // requires the user menu's own "Register your voice" option instead. This login flow
        // never needs a real voiceprint (unlike UI11/UI13's own registration flows), so there's
        // nothing to force here either way — just skip it if shown, do nothing if it isn't.
        try {
            VoiceRegistrationPage voicePage = new VoiceRegistrationPage(page);
            voicePage.waitForPageLoad();
            voicePage.clickSkipForNow();
        } catch (PlaywrightException notShowing) {
            // Already effectively on Home — nothing to skip.
        }

        HomePage homePage = new HomePage(page);
        homePage.waitForPageLoad();

        if (returningUser) {
            enforceEnglishLocale(homePage, languagePage);
        }

        loggedInPhoneNumber = phoneNumber;
        return homePage;
    }

    /** Switches a returning user back to English, since locale persists server-side and UI12 may
     * have changed it. */
    private void enforceEnglishLocale(HomePage homePage, LanguagePage languagePage) {
        homePage.clickLanguageButton();
        languagePage.waitForPageLoad();

        if (languagePage.isEnglishSelected()) {
            languagePage.clickBack();
            return;
        }

        System.out.println("[Locale] Account was not on English at login — switching back.");
        languagePage.selectEnglish();
        languagePage.waitForLocaleSelected(VoiceQueries.English.LOCALE);
        languagePage.clickContinue();

        // The "Enable Voice Banking" consent screen can reappear after a locale switch, same as
        // confirmed in UI12_MultilingualVoiceQueryTest#skipVoiceRegistrationIfPresent — tolerate
        // it not reappearing as the expected case here too.
        try {
            VoiceRegistrationPage voicePage = new VoiceRegistrationPage(page);
            voicePage.waitForPageLoad();
            voicePage.clickSkipForNow();
        } catch (PlaywrightException notShowing) {
            // Already back on Home — nothing to skip.
        }
        homePage.waitForPageLoad();
    }

    /** HomePage used for the latest query; reused because {@link HomePage#getLastBotResponse()}
     * depends on its tracked state. */
    private HomePage lastHomePage;

    /** Bot response for the latest query, for callers that inspect it after {@link #runVoiceQuery}. */
    protected String lastBotResponse() {
        return lastHomePage.getLastBotResponse();
    }

    /** Same as {@link #runVoiceQuery(String, String, String[], String, String)} but logs in as
     * {@code phoneNumber}. */
    protected void runVoiceQuery(String queryName, String query, String[] expectedKeywords,
                                  String assertionPattern, String disambiguationAccount,
                                  String phoneNumber) throws Exception {

        HomePage homePage = ensureLoggedIn(phoneNumber);
        lastHomePage = homePage;
        homePage.setCurrentQueryName(queryName);

        int holdMs = (int) TtsUtil.getWavDurationMs(currentAudioPath);
        System.out.println("[" + queryName + "] WAV duration : " + holdMs + " ms (speech + 3 s silence)");

        homePage.holdToSpeakWithRetry(holdMs, 3, 8000);
        homePage.waitForVoiceResponse(BOT_RESPONSE_TIMEOUT_MS);

        String transcribed = homePage.getLastTranscribedText();
        String botResponse = homePage.getLastBotResponse();

        System.out.println("[" + queryName + "] Expected    : " + query);
        System.out.println("[" + queryName + "] Transcribed : " + transcribed);
        System.out.println("[" + queryName + "] Bot response: " + botResponse);

        assertOrCapture(!transcribed.isEmpty(), queryName,
                "[" + queryName + "] Voice not recognised — no user message appeared in chat");

        // A "Session Ended" state can appear during or right after this turn's answer — before
        // the next query ever tries to speak (the only place recovery ran previously). Recovering
        // here too means the page-visible check below sees the reconnected hold-to-speak button
        // rather than failing hard on a session-ended screen recovery never got a chance to fix.
        homePage.recoverFromSessionEndedIfPresent();
        assertOrCapture(homePage.isPageVisible(), queryName,
                "[" + queryName + "] Home page should remain visible after voice query");

        // A blank botResponse here means one of two things: the bot is still genuinely stuck on
        // "Processing…" past BOT_RESPONSE_TIMEOUT_MS, or a reconnect (see
        // recoverFromSessionEndedIfPresent() above) re-established the session but the query
        // spoken right after got swallowed by the bot's own session-start greeting instead of
        // being answered — confirmed live: correct transcription, yet the response was "Good
        // afternoon, Welcome <name> ... How can I help you today?" instead of the actual
        // transaction data. Either way, the fix is the same: press hold-to-speak again with the
        // same query audio. holdToSpeakWithRetry() below recovers from a session-ended state
        // again itself before it (re-)speaks (including a second drop landing right on the heels
        // of the first), so this loop self-heals through repeated drops, not just a single one.
        // Only once every attempt here is exhausted does the caller's own "bot never responded"
        // check further down get to fail the test — deliberately not failing on the very first
        // blank response, since that pre-empted every one of these retries. Also re-asks on
        // isContextLostFallback() — the same silent-context-loss symptom as isGenericGreeting(),
        // just a "didn't understand" capability reset instead of a session-start greeting; see
        // reestablishIntentIfSessionEnded() for the equivalent check on the follow-up path.
        for (int reaskNum = 1;
             reaskNum <= MAX_REASK_ATTEMPTS
                     && (isGenericGreeting(botResponse) || isContextLostFallback(botResponse) || botResponse.isBlank());
             reaskNum++) {
            if (botResponse.isBlank()) {
                // Recorded here — not only if every retry below eventually fails — so a
                // no-response that self-heals on a later re-ask still shows up in the stability
                // signal, the same way SessionEndedTracker counts a recovered session drop.
                NoResponseTracker.recordOccurrence(queryName);
            }
            System.out.println("[" + queryName + "] WARN — got a generic greeting/fallback/empty response"
                    + " instead of an answer (stuck Processing, or post-reconnect) — re-asking (" + reaskNum
                    + " of " + MAX_REASK_ATTEMPTS + ")...");
            homePage.holdToSpeakWithRetry(holdMs, 3, 8000);
            homePage.waitForVoiceResponse(BOT_RESPONSE_TIMEOUT_MS);
            transcribed = homePage.getLastTranscribedText();
            botResponse = homePage.getLastBotResponse();
            System.out.println("[" + queryName + "] Re-ask " + reaskNum + " Transcribed : " + transcribed);
            System.out.println("[" + queryName + "] Re-ask " + reaskNum + " Bot response: " + botResponse);
        }

        assertOrCapture(!botResponse.isEmpty(), queryName,
                "[" + queryName + "] Bot did not respond after voice query, even after "
                + MAX_REASK_ATTEMPTS + " re-ask attempts");

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
            assertOrCapture(transcriptionContainsExpectedWords(transcribed, query), queryName,
                    "[" + queryName + "] Transcription mismatch after retries:"
                    + " expected [" + query + "] got [" + transcribed + "]");
        }

        if (isAccountDisambiguation(botResponse)) {
            String followUpAccount = disambiguationAccount != null ? disambiguationAccount : "savings";
            String expectedPattern = getDisambiguationExpectedPattern(followUpAccount);

            String followUpTranscribed = "";
            // Seeded with botResponse (the disambiguation prompt) so the first iteration's
            // reestablishIntentIfSessionEnded() call below can still see it; from the second
            // iteration on this holds the previous attempt's actual follow-up response (including
            // a context-lost fallback, if that's what came back) — passing the stale outer
            // botResponse here instead, as an earlier version did, meant isContextLostFallback()
            // was only ever checked against the original prompt and never against what the bot
            // actually said after the follow-up, so recovery silently never fired past attempt 1.
            String followUpResponse = botResponse;

            for (int attempt = 1; attempt <= 3; attempt++) {
                followUpResponse = reestablishIntentIfSessionEnded(homePage, queryName, query, followUpResponse);
                // Excludes a still-broken context-lost fallback — if the bot is stuck badly enough
                // that even re-sending the original query got the same fallback back, that isn't a
                // resolved state to hand off as the final answer; keep retrying instead of giving
                // up on the spot (confirmed live: a single retry attempt wasn't always enough).
                if (!isAccountDisambiguation(followUpResponse) && !isContextLostFallback(followUpResponse)) {
                    // Re-sending the original query already answered it directly (no
                    // disambiguation needed this time) or landed somewhere else entirely —
                    // either way there's no "which account" prompt left to answer.
                    break;
                }

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
                // A context-lost fallback (see isContextLostFallback) is non-blank and isn't the
                // disambiguation prompt either — without excluding it here too, this treats the
                // fallback as a real answer and breaks out on attempt 1, never reaching a later
                // attempt where reestablishIntentIfSessionEnded() would actually recover it.
                boolean gotRealResponse = !followUpResponse.isBlank() && !isContextLostFallback(followUpResponse);
                if (heardExpectedAccount && gotRealResponse && !isAccountDisambiguation(followUpResponse)) {
                    break;
                }
                System.out.println("[" + queryName + "] WARN — follow-up not recognised (attempt " + attempt
                        + "): expected [" + followUpAccount + "] got transcribed [" + followUpTranscribed
                        + "], bot [" + (gotRealResponse ? followUpResponse : "<empty>") + "] — retrying...");
            }

            boolean matched = Pattern.compile(expectedPattern)
                                     .matcher(followUpResponse).find();
            assertOrCapture(matched, queryName,
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
        botResponse = handleAdditionalFollowUp(queryName, query, botResponse, disambiguationAccount, homePage);

        if (assertionPattern != null) {
            boolean matched = Pattern.compile(assertionPattern).matcher(botResponse).find();
            assertOrCapture(matched, queryName,
                    "[" + queryName + "] Bot response did not match expected pattern.\n"
                    + "  Pattern : " + assertionPattern + "\n"
                    + "  Got     : " + botResponse);
        } else {
            assertOrCapture(containsAnyKeyword(botResponse, expectedKeywords), queryName,
                    "[" + queryName + "] Bot response not relevant.\n  Expected keywords: "
                    + String.join(", ", expectedKeywords)
                    + "\n  Actual response : " + botResponse);
        }

        System.out.println("[" + queryName + "] PASS");
    }

    /** Asserts {@code condition}, screenshotting immediately on failure since the app may
     * self-recover before teardown. */
    private void assertOrCapture(boolean condition, String queryName, String message) {
        if (!condition) {
            ScreenshotUtil.captureNow(page, getClass().getSimpleName(), queryName);
        }
        Assert.assertTrue(condition, message);
    }

    /** True if the response is a recognized in-flow state (e.g. account disambiguation) where
     * replaying the query would be risky. Overrides should call super. */
    protected boolean shouldStopRetrying(String botResponse) {
        return isAccountDisambiguation(botResponse);
    }

    /** The bot's generic session-start greeting, sometimes returned instead of an answer right after
     * a reconnect. */
    private static final Pattern GENERIC_GREETING = Pattern.compile("Welcome.*How can I help you today");

    protected boolean isGenericGreeting(String botResponse) {
        return GENERIC_GREETING.matcher(botResponse).find();
    }

    /** The bot's "Sorry, I didn't understand that... What would you like to do?" fallback, shown
     * when it silently loses dialogue state. */
    private static final Pattern CONTEXT_LOST_FALLBACK =
            Pattern.compile("(?i)didn.t understand that.*what would you like to do");

    protected boolean isContextLostFallback(String botResponse) {
        return CONTEXT_LOST_FALLBACK.matcher(botResponse).find();
    }

    /** Max re-asks after a greeting or empty response. */
    private static final int MAX_REASK_ATTEMPTS = 3;

    /** How long to wait for a bot reply before treating it as stuck; 15s proved too short for some
     * queries. */
    private static final int BOT_RESPONSE_TIMEOUT_MS = 30000;

    /** Hook for class-specific follow-up prompts; return {@code botResponse} unchanged if there's
     * nothing to do. */
    protected String handleAdditionalFollowUp(String queryName, String query, String botResponse,
                                               String disambiguationAccount, HomePage homePage) throws Exception {
        return botResponse;
    }

    /** If the session ended or the bot lost context before a follow-up, reconnects and re-speaks the
     * original query so the follow-up isn't treated as a new standalone query. Call before
     * overwriting the audio file. Returns the new response, or {@code currentBotResponse} if no
     * recovery was needed. */
    protected String reestablishIntentIfSessionEnded(HomePage homePage, String queryName, String query,
                                                       String currentBotResponse) throws Exception {
        boolean sessionEnded = homePage.isSessionEnded();
        if (!sessionEnded && !isContextLostFallback(currentBotResponse)) return currentBotResponse;

        if (sessionEnded) {
            System.out.println("[" + queryName + "] Session ended before follow-up — re-sending original query "
                    + "to restore context before answering...");
            homePage.recoverFromSessionEndedIfPresent();
        } else {
            System.out.println("[" + queryName + "] Bot lost conversation context before follow-up (got the"
                    + " generic \"didn't understand\" fallback, no \"Session Ended\" banner) — re-sending"
                    + " original query to restore context before answering...");
        }

        String freshQueryPath = TtsUtil.generateWav(query);
        Files.copy(Path.of(freshQueryPath), Path.of(currentAudioPath), StandardCopyOption.REPLACE_EXISTING);
        int holdMs = (int) TtsUtil.getWavDurationMs(currentAudioPath);
        TtsUtil.deleteWav(freshQueryPath);

        homePage.holdToSpeakWithRetry(holdMs, 3, 8000);
        homePage.waitForVoiceResponse(BOT_RESPONSE_TIMEOUT_MS);

        String restoredResponse = homePage.getLastBotResponse();
        System.out.println("[" + queryName + "] Re-sent original query — bot response: " + restoredResponse);
        return restoredResponse;
    }

    /** Expected pattern after answering account disambiguation with {@code followUpAccount}.
     * Defaults to the balance patterns; UI8 overrides it. */
    protected String getDisambiguationExpectedPattern(String followUpAccount) {
        return "current".equalsIgnoreCase(followUpAccount)
                ? BotResponsePatterns.Balance.CURRENT
                : BotResponsePatterns.Balance.SAVINGS;
    }

    /** True if every word of {@code expected} is in {@code transcribed}, ignoring case and
     * normalizing number words and currency notation. */
    protected boolean transcriptionContainsExpectedWords(String transcribed, String expected) {
        String cleanTranscribed = normalizeNumberWords(normalizeCurrency(transcribed).toLowerCase());
        String[] expectedWords  = normalizeNumberWords(normalizeCurrency(expected).toLowerCase())
                .split("\\s+");
        for (String word : expectedWords) {
            if (!cleanTranscribed.contains(word)) return false;
        }
        return true;
    }

    /** Normalizes ₹/Rs./INR to "rupees" and strips punctuation. */
    private String normalizeCurrency(String text) {
        return text.replaceAll("(?i)₹|\\bRs\\.?\\b|\\bINR\\b", " rupees ")
                .replaceAll("[^a-zA-Z0-9 ]", "");
    }

    private static final java.util.Map<String, String> NUMBER_WORDS = java.util.Map.ofEntries(
            java.util.Map.entry("zero", "0"), java.util.Map.entry("one", "1"),
            java.util.Map.entry("two", "2"), java.util.Map.entry("three", "3"),
            java.util.Map.entry("four", "4"), java.util.Map.entry("five", "5"),
            java.util.Map.entry("six", "6"), java.util.Map.entry("seven", "7"),
            java.util.Map.entry("eight", "8"), java.util.Map.entry("nine", "9"),
            java.util.Map.entry("ten", "10"));

    /** Converts number words 0-10 to digits. */
    private String normalizeNumberWords(String text) {
        String[] words = text.split("\\s+");
        StringBuilder normalized = new StringBuilder();
        for (String word : words) {
            normalized.append(NUMBER_WORDS.getOrDefault(word, word)).append(' ');
        }
        return normalized.toString().trim();
    }

    /** True if the bot is asking to choose between savings/current (not loan) accounts. */
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

    /** One transaction entry's sign and amount, capturing the amount. */
    private static final Pattern TRANSACTION_ENTRY_AMOUNT =
            Pattern.compile("[+-]₹([\\d,]+(?:\\.\\d+)?)(?:DEBIT|CREDIT)");

    /** A "Total spent₹X" line; card text has no space before the ₹. */
    private static final Pattern TOTAL_SPENT = Pattern.compile("Total spent₹([\\d,]+(?:\\.\\d+)?)");

    /** Counts transaction entries in a card-format response. */
    protected int countTransactionEntries(String botResponse) {
        Matcher matcher = TRANSACTION_ENTRY_AMOUNT.matcher(botResponse);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    /** All entry amounts in a card-format response, in order. */
    protected java.util.List<Double> transactionEntryAmounts(String botResponse) {
        java.util.List<Double> amounts = new java.util.ArrayList<>();
        Matcher matcher = TRANSACTION_ENTRY_AMOUNT.matcher(botResponse);
        while (matcher.find()) {
            amounts.add(parseAmount(matcher.group(1)));
        }
        return amounts;
    }

    /** Matches one DEBIT entry's amount — money going out, i.e. what "Total spent" adds up. */
    private static final Pattern DEBIT_ENTRY_AMOUNT = Pattern.compile("-₹([\\d,]+(?:\\.\\d+)?)DEBIT");

    /** If a "Total spent" line is present, checks it equals the sum of the DEBIT entries. True when
     * there's no total line. */
    protected boolean totalMatchesSumOfEntries(String botResponse) {
        Matcher totalMatcher = TOTAL_SPENT.matcher(botResponse);
        if (!totalMatcher.find()) return true;
        double total = parseAmount(totalMatcher.group(1));

        double sum = 0;
        Matcher entryMatcher = DEBIT_ENTRY_AMOUNT.matcher(botResponse);
        while (entryMatcher.find()) {
            sum += parseAmount(entryMatcher.group(1));
        }
        return Math.abs(total - sum) < 0.01;
    }

    private double parseAmount(String rawAmount) {
        return Double.parseDouble(rawAmount.replace(",", ""));
    }
}
