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
import com.voicebanking.listeners.TestListener;
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

/** Abstract base for all voice-based UI tests. Handles browser lifecycle and the core voice query flow.
 * <p>
 * {@code @Listeners} registers {@link TestListener} directly on the class, not just via
 * testng.xml's suite-level {@code <listener>} tag — that tag is silently skipped by Maven
 * Surefire whenever a run is scoped with {@code -Dtest=SomeClass} (which every run in this
 * project's day-to-day use has been), since that flag makes Surefire build its own ad-hoc suite
 * instead of using testng.xml at all. Without this, {@link TestListener#onFinish} — and so
 * {@link com.voicebanking.utils.SessionEndedTracker#writeToDisk()} and the ExtentReports HTML —
 * never ran for any {@code -Dtest=} invocation; confirmed live by target/session-ended-count.txt
 * and target/extent-report/ both being absent after a full run that did hit a session drop. */
@Listeners(TestListener.class)
public abstract class BaseVoiceTest {

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;
    protected String currentAudioPath;

    /** The audio file path the running browser was launched with, in {@link #useSharedSession()}
     * mode. Fixed for the lifetime of the browser — each query's WAV is copied onto this path
     * rather than the browser being relaunched against a new one. */
    private String sessionAudioPath;
    private boolean sessionInitialized = false;

    /** Phone number currently logged into, in {@link #useSharedSession()} mode. Null until the
     * first login. */
    private String loggedInPhoneNumber;

    /** Cached random phone number so every default-identity call within one shared session logs
     * into the same account instead of a fresh one each time. Unused outside shared-session mode. */
    private String cachedRandomPhone;

    /**
     * Opt-in for test classes whose data-provider rows can safely share one browser/login session
     * across the whole class instead of each row paying for its own browser launch and full login
     * (phone → OTP → language → skip voice-reg). Safe for classes where every row is an
     * independent conversational turn against the same account (e.g. balance inquiry phrasings) —
     * not for rows that need a guaranteed-clean, unused account. Default false preserves the
     * original per-method isolation for every other subclass.
     */
    protected boolean useSharedSession() {
        return false;
    }

    /**
     * Generates a WAV file for the test query, then either launches a fresh Chromium browser with
     * {@code --use-file-for-fake-audio-capture} pointing at that file (default, one per test
     * method), or — in {@link #useSharedSession()} mode — copies the new query's audio onto the
     * already-running session's fixed audio path and forces the app to re-acquire the microphone,
     * reusing the same browser and login across every row in the class.
     */
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
     * Captures a screenshot on test failure. In {@link #useSharedSession()} mode the browser stays
     * open for the next row (torn down once in {@link #tearDownSharedSession()} instead); otherwise
     * closes all browser resources, deletes the temporary WAV file, and paces consecutive launches
     * with a short sleep.
     */
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

    /**
     * Phone number used to log in before speaking the query. Defaults to a fresh random number
     * (new-user registration flow, no seeded history) — in {@link #useSharedSession()} mode that
     * random number is generated once and cached, so every default-identity row in the class logs
     * into the same account instead of a fresh one each time. Override to log in as an existing
     * seeded customer instead — e.g. when the query needs real transaction/loan history to assert
     * against, since a brand-new account has none.
     */
    protected String getLoginPhoneNumber() {
        if (!useSharedSession()) {
            return WelcomePage.generateRandomPhone();
        }
        if (cachedRandomPhone == null) {
            cachedRandomPhone = WelcomePage.generateRandomPhone();
        }
        return cachedRandomPhone;
    }

    /** Navigates, logs in, speaks the query, retries on transcription mismatch, and asserts the bot response. */
    protected void runVoiceQuery(String queryName, String query, String[] expectedKeywords,
                                  String assertionPattern, String disambiguationAccount) throws Exception {
        runVoiceQuery(queryName, query, expectedKeywords, assertionPattern, disambiguationAccount, getLoginPhoneNumber());
    }

    /**
     * Logs in as {@code phoneNumber} and returns the resulting home page. In
     * {@link #useSharedSession()} mode, a call for the same phone number already logged into the
     * running session is a no-op that just hands back a fresh {@link HomePage} wrapper over the
     * existing page — only the very first call, or a call naming a different identity than the one
     * currently logged in (e.g. switching to a known seeded customer), goes through the full
     * navigate → OTP → language → skip-voice-reg flow. The session is never ended proactively here
     * — only {@link HomePage}'s own reactive recovery (triggered when the live bot itself reports
     * "Session Ended") ends and reconnects a session, never a deliberate reset between queries.
     */
    private HomePage ensureLoggedIn(String phoneNumber) throws Exception {
        if (useSharedSession() && phoneNumber.equals(loggedInPhoneNumber)) {
            return new HomePage(page);
        }
        return login(phoneNumber);
    }

    /**
     * Runs the full navigate → OTP → language → skip-voice-reg login flow for {@code phoneNumber}.
     * Called by {@link #ensureLoggedIn(String)} the first time a session logs in, or whenever it
     * switches to a different identity than the one currently logged in.
     */
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

        loggedInPhoneNumber = phoneNumber;
        return homePage;
    }

    /** Same as {@link #runVoiceQuery(String, String, String[], String, String)} but logs in as
     * an explicit phone number instead of {@link #getLoginPhoneNumber()} — for data-provider rows
     * that exercise a specific known seeded customer rather than the class-wide default. */
    protected void runVoiceQuery(String queryName, String query, String[] expectedKeywords,
                                  String assertionPattern, String disambiguationAccount,
                                  String phoneNumber) throws Exception {

        HomePage homePage = ensureLoggedIn(phoneNumber);
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
        // blank response, since that pre-empted every one of these retries.
        for (int reaskNum = 1;
             reaskNum <= MAX_REASK_ATTEMPTS && (isGenericGreeting(botResponse) || botResponse.isBlank());
             reaskNum++) {
            System.out.println("[" + queryName + "] WARN — got a generic greeting/empty response instead of"
                    + " an answer (stuck Processing, or post-reconnect) — re-asking (" + reaskNum + " of "
                    + MAX_REASK_ATTEMPTS + ")...");
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
                boolean gotRealResponse = !followUpResponse.isBlank();
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
        botResponse = handleAdditionalFollowUp(queryName, botResponse, disambiguationAccount, homePage);

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

    /** Asserts {@code condition}, capturing a screenshot at this exact instant first if it's
     * about to fail. Teardown's own captureOnFailure runs later — after the exception has
     * propagated out of the test method and TestNG has invoked @AfterMethod — so by then a
     * transient failure (e.g. a dropped-then-reconnected voice session) may have already
     * self-recovered, making that screenshot show a healthy page even though the assertion
     * genuinely failed moments earlier. This captures the state as it actually was. */
    private void assertOrCapture(boolean condition, String queryName, String message) {
        if (!condition) {
            ScreenshotUtil.captureNow(page, getClass().getSimpleName(), queryName);
        }
        Assert.assertTrue(condition, message);
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

    /** Matches the bot's generic session-start greeting, e.g. "Good afternoon, Welcome Karan
     * Malhotra, I can help you review your recent transactions or check your account
     * balances.How can I help you today?" — observed replacing the real answer specifically as
     * the first response right after a mid-query reconnect. "How can I help you today" is the
     * fixed closer across every greeting observed regardless of time-of-day/name, so that's the
     * anchor, not the dynamic name/time-of-day portion. */
    private static final Pattern GENERIC_GREETING = Pattern.compile("Welcome.*How can I help you today");

    protected boolean isGenericGreeting(String botResponse) {
        return GENERIC_GREETING.matcher(botResponse).find();
    }

    /** Cap on re-asking the same query after a post-reconnect greeting/empty response — see the
     * re-ask loop in {@link #runVoiceQuery(String, String, String[], String, String, String)}. */
    private static final int MAX_REASK_ATTEMPTS = 3;

    /** How long to wait for the bot's reply before treating it as stuck — raised from an earlier
     * 15 s after live runs showed genuine (non-stuck) replies still arriving past that mark,
     * particularly for date-range and filtered transaction queries. */
    private static final int BOT_RESPONSE_TIMEOUT_MS = 30000;

    /** Extension point — see call site in {@link #runVoiceQuery(String, String, String[], String,
     * String, String)}. Override in a test class to handle a follow-up prompt specific to that
     * class's feature; return {@code botResponse} unchanged when there's nothing to do. */
    protected String handleAdditionalFollowUp(String queryName, String botResponse,
                                               String disambiguationAccount, HomePage homePage) throws Exception {
        return botResponse;
    }

    /**
     * Returns the pattern the bot's response should match after the user picks {@code
     * followUpAccount} in response to an account-disambiguation prompt. Defaults to the
     * SAVINGS/CURRENT balance patterns, since that's what UI7's balance queries expect — a balance
     * response explicitly names which account it's for ("The balance in your SAVINGS account
     * is..."). Not every feature works that way: a transaction-history response is just a list of
     * entries with no per-account wording to distinguish, so UI8 overrides this to check the
     * generic transaction-entry shape instead, regardless of which account was picked.
     */
    protected String getDisambiguationExpectedPattern(String followUpAccount) {
        return "current".equalsIgnoreCase(followUpAccount)
                ? BotResponsePatterns.Balance.CURRENT
                : BotResponsePatterns.Balance.SAVINGS;
    }

    /** Returns true when every word in {@code expected} appears in {@code transcribed}
     * (case-insensitive). Numbers are normalized to digits first — STT is inconsistent about
     * rendering a spoken number as the word ("one") or the digit ("1"), sometimes doing either
     * for the exact same audio, so comparing raw text made a perfectly correct transcription
     * fail whenever it didn't happen to pick the same form as the expected text. Currency amounts
     * are normalized the same way: STT sometimes renders "10000 rupees" as "₹10,000" — correct,
     * just a different notation for the same amount — so a leading ₹/Rs./INR marker is treated as
     * the word "rupees" before comparison, rather than requiring both forms to literally match. */
    protected boolean transcriptionContainsExpectedWords(String transcribed, String expected) {
        String cleanTranscribed = normalizeNumberWords(normalizeCurrency(transcribed).toLowerCase());
        String[] expectedWords  = normalizeNumberWords(normalizeCurrency(expected).toLowerCase())
                .split("\\s+");
        for (String word : expectedWords) {
            if (!cleanTranscribed.contains(word)) return false;
        }
        return true;
    }

    /** Replaces a ₹/Rs./INR currency marker with the word "rupees" so "₹10,000" and "10000 rupees"
     * normalize to the same words, then strips everything but letters, digits, and spaces. */
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

    /** Matches one transaction-entry's sign and amount — the same shape as {@link
     * BotResponsePatterns.Transactions#ENTRY}, but with the amount captured so callers can count
     * entries or sum their amounts, rather than just confirming at least one exists. */
    private static final Pattern TRANSACTION_ENTRY_AMOUNT =
            Pattern.compile("[+-]₹([\\d,]+(?:\\.\\d+)?)(?:DEBIT|CREDIT)");

    /** Matches a "Total spent₹<amount>" summary line, capturing the amount. Card responses run
     * their nested elements together with no separating whitespace (see {@link
     * BotResponsePatterns.Transactions#ENTRY}'s own comment on this), so there is deliberately no
     * space between "spent" and the currency symbol here. */
    private static final Pattern TOTAL_SPENT = Pattern.compile("Total spent₹([\\d,]+(?:\\.\\d+)?)");

    /** Counts how many transaction entries appear in a card-format bot response — e.g. to confirm
     * "recent transactions" returned exactly the number of entries the app is known to page at. */
    protected int countTransactionEntries(String botResponse) {
        Matcher matcher = TRANSACTION_ENTRY_AMOUNT.matcher(botResponse);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    /** When a response includes a "Total spent₹X" summary line, verifies X equals the sum of every
     * listed entry's amount — a content-correctness check that doesn't require knowing the seed
     * data's true values in advance, only that the bot's own total is internally consistent with
     * the entries it displayed alongside it. Returns true (nothing to check) when no total line is
     * present, e.g. plain entry-list or prose-summary responses. */
    protected boolean totalMatchesSumOfEntries(String botResponse) {
        Matcher totalMatcher = TOTAL_SPENT.matcher(botResponse);
        if (!totalMatcher.find()) return true;
        double total = parseAmount(totalMatcher.group(1));

        double sum = 0;
        Matcher entryMatcher = TRANSACTION_ENTRY_AMOUNT.matcher(botResponse);
        while (entryMatcher.find()) {
            sum += parseAmount(entryMatcher.group(1));
        }
        return Math.abs(total - sum) < 0.01;
    }

    private double parseAmount(String rawAmount) {
        return Double.parseDouble(rawAmount.replace(",", ""));
    }
}
