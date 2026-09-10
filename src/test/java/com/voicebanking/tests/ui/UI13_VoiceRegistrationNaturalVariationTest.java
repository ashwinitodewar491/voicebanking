package com.voicebanking.tests.ui;

import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Playwright;
import com.voicebanking.DataText.BotResponsePatterns;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.DataText.MultilingualVoiceQueries;
import com.voicebanking.DataText.VoiceQueries;
import com.voicebanking.pages.BasePage;
import com.voicebanking.pages.HomePage;
import com.voicebanking.pages.LanguagePage;
import com.voicebanking.pages.OtpPage;
import com.voicebanking.pages.VoiceRegistrationPage;
import com.voicebanking.pages.WelcomePage;
import com.voicebanking.utils.NoResponseTracker;
import com.voicebanking.utils.ScreenshotUtil;
import com.voicebanking.utils.TtsUtil;
import com.voicebanking.utils.tts.AudioEffects;
import com.voicebanking.utils.tts.EdgeTtsEngine;
import com.voicebanking.utils.tts.VoiceVariation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Same voice-authentication coverage as {@link UI11_VoiceRegistrationAuthTest}, but this class
 * puts the natural pitch/pace variation on the <b>query</b> side instead of enrollment.
 * Enrollment here uses {@link EdgeTtsEngine#VOICE} completely unvaried — byte-identical in shape
 * to UI11's own enrollment audio, one single voice, no offset. The later balance-query audio is
 * then synthesized as the same registered voice shifted by a rate/pitch offset (see {@link
 * VoiceVariation}):
 * <ul>
 *   <li>{@link #testPositiveVoiceMatchIsAuthorizedWithNaturalVariation()} uses a small offset
 *       ({@code SMALL_RATE_RANGE_PERCENT}/{@code SMALL_PITCH_RANGE_HZ}) meant to still read as
 *       the same person on a slightly different take, and asserts the app still authorizes it.
 *   <li>{@link #testNegativeVoiceMismatchIsRejectedWithNaturalVariation()} uses a fixed {@code
 *       +REJECTION_PITCH_HZ}Hz pitch shift — large enough that it should no longer match the
 *       enrolled voiceprint — and asserts the app rejects it with "Not authorized", the same
 *       response UI11 gets from querying with an entirely different edge-tts voice. This is
 *       testing the matcher's sensitivity boundary on a single underlying voice, not swapping to
 *       a different speaker.
 * </ul>
 *
 * <p>A completely separate file/class from UI11 by design, so that class and its existing
 * coverage are entirely untouched by this addition.
 *
 * <p>An earlier version of this class instead varied <i>enrollment</i> audio (either per-rep or
 * once per session) — per-rep variation was dropped after it reliably broke {@code
 * VoiceRegistrationPage}'s single persistent mic stream (see git history / earlier revisions of
 * this file for the root-cause writeup), and once-per-session enrollment variation was dropped
 * in favor of putting the variation on the query side instead, per direct instruction.
 *
 * <p><b>Registers once for the whole class, not once per test.</b> Every {@code @Test} method
 * calls {@link #registerVoiceAndReachHome()}, but only the first one TestNG happens to run
 * actually enrolls — every method after that finds the account already registered (via {@link
 * HomePage#isVoiceRegistered()}) and skips straight to Home, since no test removes the
 * registration in a {@code finally} block anymore. The voiceprint is removed exactly once, by
 * {@link #removeVoiceRegistrationAfterAllTests()}, after every test in the class has run. This
 * matches how voice auth is actually used (enroll once, verify many times) and avoids paying for
 * a fresh 3-rep enrollment — the slowest, most failure-prone part of this whole flow — on every
 * single query variant.
 */
public class UI13_VoiceRegistrationNaturalVariationTest extends BasePage {

    private static final int ENROLLMENT_REPS = 3;
    private static final int MAX_TAKES_PER_REP = 3;

    /** Range for the positive test's query — small enough to still read as the same person on a
     * slightly different take. */
    private static final int SMALL_RATE_RANGE_PERCENT = 5;
    private static final int SMALL_PITCH_RANGE_HZ = 8;

    /** Fixed pitch shift for the negative test's query — per direct instruction, a query this far
     * off the enrolled voice's pitch is expected to be rejected. Live-confirmed across multiple
     * runs that this hypothesis does NOT hold (the app still authorizes it) — the assertion is
     * left as originally specified so that finding stays visible as a failing test rather than
     * being quietly edited away. */
    private static final String REJECTION_PITCH_HZ = "+17Hz";
    private static final String REJECTION_RATE_PERCENT = "+0%";

    /** Wider pitch shifts than {@link #REJECTION_PITCH_HZ}, probing whether a bigger deviation
     * finds an actual rejection boundary the 17Hz case didn't. */
    private static final String WIDE_PITCH_SHIFT_25HZ = "+25Hz";
    private static final String WIDE_PITCH_SHIFT_35HZ = "+35Hz";

    /** Linear volume multipliers for {@link AudioEffects#applyVolume} — a quieter/louder delivery
     * of the same words, not a different speaker. */
    private static final double VOLUME_QUIETER_FACTOR = 0.6;
    private static final double VOLUME_LOUDER_FACTOR = 1.4;

    /** Playback-speed multiplier for {@link AudioEffects#applyTempo} — within ffmpeg's single-pass
     * atempo range [0.5, 2.0]; kept modest since this is meant to read as a faster-talking take of
     * the same person, not an obviously sped-up recording. */
    private static final double TEMPO_FASTER_FACTOR = 1.15;

    /** Shelf-EQ gains for {@link AudioEffects#applyToneShift} simulating a duller/muffled
     * recording (bass boosted, treble cut) — e.g. weak mic placement or speaking away from the
     * device, not a different speaker. */
    private static final int MUFFLED_BASS_GAIN_DB = 6;
    private static final int MUFFLED_TREBLE_GAIN_DB = -6;

    /** Pink-noise amplitude for {@link AudioEffects#applyBackgroundNoise} — a subtle, constant
     * hiss under clearly-intelligible speech (a real room), not overpowering noise. */
    private static final double BACKGROUND_NOISE_AMPLITUDE = 0.03;

    private String generatedWavPath;

    /** The one login/registration session every {@code @Test} method shares — set up once by
     * {@link #registerOnceForAllTests()} and reused for every query, exactly like a real user
     * who registers their voice once and then asks several different things without logging out
     * in between. */
    private HomePage sharedHomePage;

    @BeforeClass(alwaysRun = true)
    public void setAudioFile() throws Exception {
        generatedWavPath = TtsUtil.generateWav(
                VoiceQueries.English.VOICE_ENROLLMENT_PHRASE, EdgeTtsEngine.VOICE);
    }

    @AfterClass(alwaysRun = true)
    public void clearAudioFile() {
        TtsUtil.deleteWav(generatedWavPath);
    }

    /** Same browser/mic setup as {@link UI11_VoiceRegistrationAuthTest#setUpBrowser()} — see
     * that method's javadoc for why the extra Chromium args and the two init scripts
     * (window.__micStreams tracking, AnalyserNode stub) are needed.
     *
     * <p>Runs once for the whole class ({@code @BeforeClass}, not {@code @BeforeMethod}) — every
     * {@code @Test} method shares this one browser/page instance instead of getting its own, so
     * the login/registration session below survives across all of them. {@code
     * dependsOnMethods = "setAudioFile"} guarantees {@link #generatedWavPath} is ready first;
     * TestNG does not otherwise order multiple {@code @BeforeClass} methods in one class. */
    @Override
    @BeforeClass(alwaysRun = true, dependsOnMethods = "setAudioFile")
    public void setUpBrowser() {
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));

        List<String> args = new ArrayList<>();
        args.add("--disable-gpu");
        args.add("--use-fake-device-for-media-stream");
        args.add("--use-fake-ui-for-media-stream");
        args.add("--use-file-for-fake-audio-capture=" + generatedWavPath);

        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setArgs(args));
        context = browser.newContext(
                new Browser.NewContextOptions()
                        .setPermissions(List.of("microphone")));
        page = context.newPage();

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

        page.addInitScript(
                "(function() {"
                + "  window.AnalyserNode.prototype.getByteFrequencyData = function(array) {"
                + "    for (var i = 0; i < array.length; i++) { array[i] = 255; }"
                + "  };"
                + "})();");

        // The registration screen's "Play image description" button reads a per-image
        // description aloud via speechSynthesis.speak() — there's no DOM attribute exposing that
        // text (the <img> itself has empty alt=""), so the only way to read it is to intercept
        // the call itself, the same interception technique as the getUserMedia patch above.
        // Confirmed live via manual DOM inspection before writing this.
        page.addInitScript(
                "(function() {"
                + "  window.__lastImageDescription = null;"
                + "  if (!window.speechSynthesis) return;"
                + "  var originalSpeak = window.speechSynthesis.speak.bind(window.speechSynthesis);"
                + "  window.speechSynthesis.speak = function(utterance) {"
                + "    window.__lastImageDescription = utterance.text;"
                + "    return originalSpeak(utterance);"
                + "  };"
                + "})();");
    }

    /** Logs in once and registers once for the whole class — every {@code @Test} method below
     * reuses {@link #sharedHomePage} to send its own query rather than logging in and registering
     * itself. Runs after {@link #setUpBrowser()} ({@code dependsOnMethods}) since it needs {@code
     * page} to already exist. */
    @BeforeClass(alwaysRun = true, dependsOnMethods = "setUpBrowser")
    public void registerOnceForAllTests() throws Exception {
        sharedHomePage = registerVoiceAndReachHome();
    }

    /** Screenshots on failure like {@link com.voicebanking.pages.BasePage#tearDown} normally
     * would, but deliberately does <b>not</b> close the browser/page/context — all 9 {@code
     * @Test} methods in this class share one continuous login/registration session (see {@link
     * #sharedHomePage}), so tearing anything down after an individual method would end that
     * shared session early. Actual teardown happens once, in {@link
     * #removeVoiceRegistrationAfterAllTests()}, after every test in the class has run. */
    @Override
    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (page != null) {
            ScreenshotUtil.captureOnFailure(page, result);
        }
    }

    /** Logs in as Leena Kamat and completes 3x voice-registration enrollment — each rep spoken
     * as {@link EdgeTtsEngine#VOICE} actually describing that rep's on-screen image (captured
     * live via {@link #captureImageDescription()}), instead of a fixed canned phrase unrelated
     * to what's shown — then clicks Start Banking to land on Home. Otherwise mirrors {@link
     * UI11_VoiceRegistrationAuthTest#registerVoiceAndReachHome()} (same account, same
     * known-returning-user fallback, same reasons) — see that method's javadoc for why this
     * specific account is used.
     *
     * <p>Deliberately does <b>not</b> force a mic-stream reacquire between reps the way an
     * earlier version of this class did (see git history) — that hack was what corrupted rep 2's
     * audio there. The app itself transitions to a new image after each Submit, and that's a
     * strong signal its own React lifecycle re-arms the mic for the new step already; this
     * relies on that instead of fighting it. Confirmed live whether that hypothesis holds — see
     * the try-run this was validated against. */
    private HomePage registerVoiceAndReachHome() throws Exception {
        // Placeholder content only — real per-rep audio is generated once each image is on
        // screen and its description captured, below. This just needs to be a valid non-empty
        // WAV present at browser-launch time (setUpBrowser already wired --use-file-for-fake-
        // audio-capture to generatedWavPath before this method ever runs).
        WelcomePage welcomePage = new WelcomePage(page, Endpoints.getUiBaseUrl());
        welcomePage.navigate();
        welcomePage.dismissPwaPopupIfPresent();
        welcomePage.enterPhoneNumber("9812341042");
        welcomePage.clickSendOtp();

        OtpPage otpPage = new OtpPage(page);
        otpPage.waitForPageLoad();
        otpPage.enterOtp(OtpPage.getTestOtp());
        otpPage.clickContinue();

        LanguagePage languagePage = new LanguagePage(page);
        try {
            languagePage.waitForPageLoad();
            languagePage.selectEnglish();
            languagePage.clickContinue();
        } catch (PlaywrightException ignored) {
            // language page absent for returning users
        }

        VoiceRegistrationPage voicePage = new VoiceRegistrationPage(page);
        try {
            voicePage.waitForPageLoad();
        } catch (PlaywrightException notAutoPrompted) {
            // The app does NOT reliably auto-prompt for registration on login regardless of
            // actual registration status — confirmed live that an unregistered account can land
            // straight on Home without this screen ever appearing. So "the screen didn't show up"
            // is not proof of being registered; check the user menu directly instead of assuming.
            HomePage homePage = new HomePage(page);
            homePage.waitForPageLoad();

            if (homePage.isVoiceRegistered()) {
                return homePage;
            }

            System.out.println("[VoiceVariation] Registration screen didn't auto-appear but the "
                    + "account isn't actually registered — forcing it via the user menu instead.");
            homePage.clickRegisterVoiceFromMenu();
            voicePage.waitForPageLoad();
        }

        voicePage.checkConsent();
        voicePage.clickStartRegistration();

        for (int rep = 1; rep <= ENROLLMENT_REPS; rep++) {
            voicePage.waitForRecordingScreenReady();
            Assert.assertTrue(voicePage.waitForImageLoaded(5000),
                    "[VoiceVariation] Enrollment rep " + rep + "'s image failed to load");

            String description = captureImageDescription(voicePage);
            System.out.println("[VoiceVariation] Enrollment rep " + rep + " image description: "
                    + description);

            String repWavPath = TtsUtil.generateWav(description, EdgeTtsEngine.VOICE);
            Files.copy(Path.of(repWavPath), Path.of(generatedWavPath), StandardCopyOption.REPLACE_EXISTING);
            TtsUtil.deleteWav(repWavPath);
            recordAcceptedTake(voicePage, rep);
            voicePage.clickSubmit();
        }

        voicePage.clickStartBanking();

        HomePage homePage = new HomePage(page);
        homePage.waitForPageLoad();
        return homePage;
    }


    /** Taps the "Play image description" button and waits for the {@code speechSynthesis.speak()}
     * interception (see {@link #setUpBrowser()}) to capture its text — that's the actual
     * description of whichever image is currently on screen for this rep. Throws if nothing was
     * captured within the timeout, rather than silently falling back to stale/empty text. */
    private String captureImageDescription(VoiceRegistrationPage voicePage) {
        page.evaluate("() => { window.__lastImageDescription = null; }");
        voicePage.clickPlayImageDescription();

        long deadline = System.currentTimeMillis() + 3000;
        Object description = null;
        while (System.currentTimeMillis() < deadline) {
            description = page.evaluate("() => window.__lastImageDescription");
            if (description != null) break;
            page.waitForTimeout(100);
        }
        if (description == null) {
            throw new RuntimeException("Failed to capture image description via "
                    + "speechSynthesis.speak() interception — see setUpBrowser()'s init script");
        }
        return description.toString();
    }

    /** Same take/retry loop as {@link UI11_VoiceRegistrationAuthTest#recordAcceptedTake}. */
    private void recordAcceptedTake(VoiceRegistrationPage voicePage, int rep) throws Exception {
        for (int take = 1; take <= MAX_TAKES_PER_REP; take++) {
            System.out.println("[VoiceVariation] Enrollment attempt " + rep + " of " + ENROLLMENT_REPS
                    + " (take " + take + " of " + MAX_TAKES_PER_REP + ")...");

            voicePage.tapMicAndRecord();

            if (voicePage.waitForRecordingAccepted(5000)) return;

            System.out.println("[VoiceVariation] Recording not accepted — re-recording...");
            voicePage.clickRerecord();
        }
        throw new RuntimeException("Recording rejected " + MAX_TAKES_PER_REP
                + " times in a row for enrollment attempt " + rep + " — giving up");
    }

    private static final Pattern GENERIC_GREETING = Pattern.compile("Welcome.*How can I help you today");
    private static final Pattern CONTEXT_LOST_FALLBACK =
            Pattern.compile("(?i)didn.t understand that.*what would you like to do");

    /** Every query in this class asks {@link VoiceQueries.English#ACCOUNT_BALANCE} — a
     * transcription missing "balance" means STT misheard the word (observed live: a small
     * +3%/-5Hz shift got heard as "Bats?"), not that voice-auth rejected anything. Retried the
     * same way a generic-greeting/context-lost fallback is, so a pitch/rate shift that's fine for
     * voice-matching doesn't fail the test over an unrelated STT hiccup. */
    private static final Pattern STT_HEARD_BALANCE = Pattern.compile("(?i)balance");

    private static final int MAX_REASK_ATTEMPTS = 3;

    /** Balance query spoken in {@code voice} (always {@link EdgeTtsEngine#VOICE} — the registered
     * voice — in every test here; a different edge-tts voice is never used, see class javadoc)
     * with the given rate/pitch offset applied on top. Otherwise mirrors {@link
     * UI11_VoiceRegistrationAuthTest#askBalanceWithVoice}. */
    private String askBalanceWithVoice(HomePage homePage, String voice, String rate, String pitch)
            throws Exception {
        System.out.println("[VoiceVariation] Query audio — voice=" + voice + " (unchanged), rate="
                + rate + ", pitch=" + pitch);
        String queryWavPath = TtsUtil.generateWavWithVariation(
                VoiceQueries.English.ACCOUNT_BALANCE, voice, rate, pitch);
        return sendPreparedQueryAudio(homePage, queryWavPath);
    }

    /** Same query text and send/verify/retry flow as {@link #askBalanceWithVoice}, but for a
     * caller that has already produced its own WAV — e.g. one of {@link AudioEffects}'s
     * post-processing filters (band-pass, tempo, volume, tone) applied on top of a plain baseline
     * synthesis, simulating a channel/device/delivery difference rather than a rate/pitch offset
     * at the synthesis stage. Takes ownership of {@code queryWavPath} (deletes it once copied). */
    private String sendPreparedQueryAudio(HomePage homePage, String queryWavPath) throws Exception {
        Files.copy(Path.of(queryWavPath), Path.of(generatedWavPath), StandardCopyOption.REPLACE_EXISTING);
        int holdMs = (int) TtsUtil.getWavDurationMs(generatedWavPath);
        TtsUtil.deleteWav(queryWavPath);
        homePage.reacquireMicrophoneForFollowUp();
        homePage.holdToSpeakWithRetry(holdMs, 3, 8000);
        homePage.waitForVoiceResponse(15000);

        String transcribed = homePage.getLastTranscribedText();
        String botResponse = homePage.getLastBotResponse();
        System.out.println("[VoiceVariation] Transcribed  : " + transcribed);
        System.out.println("[VoiceVariation] Bot response : " + botResponse);

        for (int reaskNum = 1;
             reaskNum <= MAX_REASK_ATTEMPTS
                     && (GENERIC_GREETING.matcher(botResponse).find()
                        || CONTEXT_LOST_FALLBACK.matcher(botResponse).find()
                        || botResponse.isBlank()
                        || !STT_HEARD_BALANCE.matcher(transcribed).find());
             reaskNum++) {
            if (botResponse.isBlank()) {
                NoResponseTracker.recordOccurrence("VoiceVariation: query re-ask");
            }
            String reason = !STT_HEARD_BALANCE.matcher(transcribed).find()
                    ? "STT mis-transcribed the query (\"" + transcribed + "\" — no \"balance\" heard)"
                    : "generic greeting/fallback/empty response instead of an answer (stuck "
                            + "Processing, or post-reconnect)";
            System.out.println("[VoiceVariation] WARN — got a " + reason + " — re-asking ("
                    + reaskNum + " of " + MAX_REASK_ATTEMPTS + ")...");
            homePage.reacquireMicrophoneForFollowUp();
            homePage.holdToSpeakWithRetry(holdMs, 3, 8000);
            homePage.waitForVoiceResponse(15000);
            transcribed = homePage.getLastTranscribedText();
            botResponse = homePage.getLastBotResponse();
            System.out.println("[VoiceVariation] Re-ask " + reaskNum + " Transcribed : " + transcribed);
            System.out.println("[VoiceVariation] Re-ask " + reaskNum + " Bot response: " + botResponse);
        }

        return botResponse;
    }

    /** Sends an arbitrary {@code text}/{@code voice} query and returns the bot's response — unlike
     * {@link #askBalanceWithVoice}, this does <b>not</b> retry on a missing "balance" in the
     * transcription, since that check only makes sense for the English balance phrase. Still
     * retries on a generic greeting/context-lost fallback/blank response, since those indicate a
     * stuck session regardless of query language. Used for the cross-language exploratory tests,
     * where there's no established "correct" transcription to check against. */
    private String sendCustomLanguageQuery(HomePage homePage, String text, String voice) throws Exception {
        String queryWavPath = TtsUtil.generateWav(text, voice);
        Files.copy(Path.of(queryWavPath), Path.of(generatedWavPath), StandardCopyOption.REPLACE_EXISTING);
        int holdMs = (int) TtsUtil.getWavDurationMs(generatedWavPath);
        TtsUtil.deleteWav(queryWavPath);
        homePage.reacquireMicrophoneForFollowUp();
        homePage.holdToSpeakWithRetry(holdMs, 3, 8000);
        homePage.waitForVoiceResponse(15000);

        String transcribed = homePage.getLastTranscribedText();
        String botResponse = homePage.getLastBotResponse();
        System.out.println("[VoiceVariation] Query voice  : " + voice);
        System.out.println("[VoiceVariation] Query text   : " + text);
        System.out.println("[VoiceVariation] Transcribed  : " + transcribed);
        System.out.println("[VoiceVariation] Bot response : " + botResponse);

        for (int reaskNum = 1;
             reaskNum <= MAX_REASK_ATTEMPTS
                     && (GENERIC_GREETING.matcher(botResponse).find()
                        || CONTEXT_LOST_FALLBACK.matcher(botResponse).find()
                        || botResponse.isBlank());
             reaskNum++) {
            if (botResponse.isBlank()) {
                NoResponseTracker.recordOccurrence("VoiceVariation: cross-language re-ask");
            }
            System.out.println("[VoiceVariation] WARN — got a generic greeting/fallback/empty "
                    + "response — re-asking (" + reaskNum + " of " + MAX_REASK_ATTEMPTS + ")...");
            homePage.reacquireMicrophoneForFollowUp();
            homePage.holdToSpeakWithRetry(holdMs, 3, 8000);
            homePage.waitForVoiceResponse(15000);
            transcribed = homePage.getLastTranscribedText();
            botResponse = homePage.getLastBotResponse();
            System.out.println("[VoiceVariation] Re-ask " + reaskNum + " Transcribed : " + transcribed);
            System.out.println("[VoiceVariation] Re-ask " + reaskNum + " Bot response: " + botResponse);
        }

        return botResponse;
    }

    @Test(groups = {"ui", "regression", "smoke", "botverification"},
            description = "Should authorize a balance query spoken with a small natural pitch/pace "
                    + "variation of the registered voice")
    public void testPositiveVoiceMatchIsAuthorizedWithNaturalVariation() throws Exception {
        HomePage homePage = sharedHomePage;

        String rate = VoiceVariation.randomRate(SMALL_RATE_RANGE_PERCENT);
        String pitch = VoiceVariation.randomPitch(SMALL_PITCH_RANGE_HZ);
        String botResponse = askBalanceWithVoice(homePage, EdgeTtsEngine.VOICE, rate, pitch);

        Assert.assertTrue(
                Pattern.compile(BotResponsePatterns.Balance.ANY).matcher(botResponse).find(),
                "[VoiceVariation] Expected a balance response for a small natural pitch/pace "
                + "variation (rate=" + rate + ", pitch=" + pitch + ") of the registered voice.\n"
                + "  Pattern : " + BotResponsePatterns.Balance.ANY + "\n"
                + "  Got     : " + botResponse);
    }

    @Test(groups = {"ui", "regression", "negative", "botverification"},
            description = "Should reject a balance query spoken with a large pitch shift (+17Hz) of "
                    + "the registered voice, even though it's the same underlying speaker — no "
                    + "\"smoke\" here since this documents a currently-disproven hypothesis (the app "
                    + "consistently authorizes this instead of rejecting it), not stable behavior")
    public void testNegativeVoiceMismatchIsRejectedWithNaturalVariation() throws Exception {
        HomePage homePage = sharedHomePage;

        String botResponse = askBalanceWithVoice(
                homePage, EdgeTtsEngine.VOICE, REJECTION_RATE_PERCENT, REJECTION_PITCH_HZ);

        Assert.assertFalse(
                Pattern.compile(BotResponsePatterns.Balance.ANY).matcher(botResponse).find(),
                "[VoiceVariation] A " + REJECTION_PITCH_HZ + " pitch-shifted query should NOT "
                + "receive the account balance.\n  Got: " + botResponse);

        Assert.assertTrue(
                Pattern.compile(BotResponsePatterns.Authorization.VOICE_NOT_RECOGNIZED)
                        .matcher(botResponse).find(),
                "[VoiceVariation] Expected an authorization-rejected response for the "
                + REJECTION_PITCH_HZ + " pitch-shifted query.\n  Pattern : "
                + BotResponsePatterns.Authorization.VOICE_NOT_RECOGNIZED + "\n  Got     : "
                + botResponse);
    }

    /** Generates the baseline balance-query WAV (registered voice, no rate/pitch offset) that
     * each acoustic-dimension test below runs its own {@link AudioEffects} filter on top of —
     * these tests are isolating one channel/delivery dimension at a time, not combining it with a
     * synthesis-level rate/pitch shift too. */
    private String generateBaselineQueryWav() throws Exception {
        return TtsUtil.generateWav(VoiceQueries.English.ACCOUNT_BALANCE, EdgeTtsEngine.VOICE);
    }

    @Test(groups = {"ui", "regression", "negative", "botverification"},
            description = "Frequency-related patterns: should authorize a balance query pitch-"
                    + "shifted +25Hz from the registered voice — no \"smoke\" here since this "
                    + "boundary case has shown inconsistent outcomes across live runs, not the "
                    + "stable/fast signal a smoke tier needs")
    public void testPitchShift25HzQueryStillAuthorized() throws Exception {
        HomePage homePage = sharedHomePage;
        String queryWavPath = TtsUtil.generateWavWithVariation(VoiceQueries.English.ACCOUNT_BALANCE,
                EdgeTtsEngine.VOICE, REJECTION_RATE_PERCENT, WIDE_PITCH_SHIFT_25HZ);
        String botResponse = sendPreparedQueryAudio(homePage, queryWavPath);

        Assert.assertTrue(
                Pattern.compile(BotResponsePatterns.Balance.ANY).matcher(botResponse).find(),
                "[VoiceVariation] Expected a balance response for a " + WIDE_PITCH_SHIFT_25HZ
                + " pitch-shifted query of the registered voice.\n  Got: " + botResponse);
    }

    @Test(groups = {"ui", "regression", "negative", "botverification"},
            description = "Frequency-related patterns: should authorize a balance query pitch-"
                    + "shifted +35Hz from the registered voice — no \"smoke\" here since this "
                    + "consistently contradicts the assertion (genuinely rejected instead of "
                    + "authorized) rather than being stable expected behavior")
    public void testPitchShift35HzQueryStillAuthorized() throws Exception {
        HomePage homePage = sharedHomePage;
        String queryWavPath = TtsUtil.generateWavWithVariation(VoiceQueries.English.ACCOUNT_BALANCE,
                EdgeTtsEngine.VOICE, REJECTION_RATE_PERCENT, WIDE_PITCH_SHIFT_35HZ);
        String botResponse = sendPreparedQueryAudio(homePage, queryWavPath);

        Assert.assertTrue(
                Pattern.compile(BotResponsePatterns.Balance.ANY).matcher(botResponse).find(),
                "[VoiceVariation] Expected a balance response for a " + WIDE_PITCH_SHIFT_35HZ
                + " pitch-shifted query of the registered voice.\n  Got: " + botResponse);
    }

    @Test(groups = {"ui", "regression", "smoke", "botverification"},
            description = "Vocal characteristics / timbre: should authorize a noticeably quieter "
                    + "delivery of the registered voice")
    public void testQuieterVolumeQueryStillAuthorized() throws Exception {
        HomePage homePage = sharedHomePage;
        String baselineWav = generateBaselineQueryWav();
        String quieterWav = AudioEffects.applyVolume(baselineWav, VOLUME_QUIETER_FACTOR);
        TtsUtil.deleteWav(baselineWav);
        String botResponse = sendPreparedQueryAudio(homePage, quieterWav);

        Assert.assertTrue(
                Pattern.compile(BotResponsePatterns.Balance.ANY).matcher(botResponse).find(),
                "[VoiceVariation] Expected a balance response for a " + VOLUME_QUIETER_FACTOR
                + "x-volume query of the registered voice.\n  Got: " + botResponse);
    }

    @Test(groups = {"ui", "regression", "botverification"},
            description = "Vocal characteristics / timbre: should authorize a noticeably louder "
                    + "delivery of the registered voice")
    public void testLouderVolumeQueryStillAuthorized() throws Exception {
        HomePage homePage = sharedHomePage;
        String baselineWav = generateBaselineQueryWav();
        String louderWav = AudioEffects.applyVolume(baselineWav, VOLUME_LOUDER_FACTOR);
        TtsUtil.deleteWav(baselineWav);
        String botResponse = sendPreparedQueryAudio(homePage, louderWav);

        Assert.assertTrue(
                Pattern.compile(BotResponsePatterns.Balance.ANY).matcher(botResponse).find(),
                "[VoiceVariation] Expected a balance response for a " + VOLUME_LOUDER_FACTOR
                + "x-volume query of the registered voice.\n  Got: " + botResponse);
    }

    @Test(groups = {"ui", "regression", "smoke", "botverification"},
            description = "Spectral characteristics: should authorize a balance query band-limited "
                    + "to telephone quality (300Hz-3400Hz) instead of full-bandwidth audio")
    public void testTelephoneBandpassQueryStillAuthorized() throws Exception {
        HomePage homePage = sharedHomePage;
        String baselineWav = generateBaselineQueryWav();
        String bandpassWav = AudioEffects.applyTelephoneBandpass(baselineWav);
        TtsUtil.deleteWav(baselineWav);
        String botResponse = sendPreparedQueryAudio(homePage, bandpassWav);

        Assert.assertTrue(
                Pattern.compile(BotResponsePatterns.Balance.ANY).matcher(botResponse).find(),
                "[VoiceVariation] Expected a balance response for a telephone-band-limited "
                + "query of the registered voice.\n  Got: " + botResponse);
    }

    @Test(groups = {"ui", "regression", "botverification"},
            description = "Temporal behavior: should authorize a balance query played back "
                    + "15% faster (pure tempo change, independent of TTS rate)")
    public void testFasterTempoQueryStillAuthorized() throws Exception {
        HomePage homePage = sharedHomePage;
        String baselineWav = generateBaselineQueryWav();
        String fasterWav = AudioEffects.applyTempo(baselineWav, TEMPO_FASTER_FACTOR);
        TtsUtil.deleteWav(baselineWav);
        String botResponse = sendPreparedQueryAudio(homePage, fasterWav);

        Assert.assertTrue(
                Pattern.compile(BotResponsePatterns.Balance.ANY).matcher(botResponse).find(),
                "[VoiceVariation] Expected a balance response for a " + TEMPO_FASTER_FACTOR
                + "x-tempo query of the registered voice.\n  Got: " + botResponse);
    }

    @Test(groups = {"ui", "regression", "botverification"},
            description = "Voice timbre/quality: should authorize a duller/muffled-sounding "
                    + "delivery of the registered voice (bass boosted, treble cut)")
    public void testMuffledToneQueryStillAuthorized() throws Exception {
        HomePage homePage = sharedHomePage;
        String baselineWav = generateBaselineQueryWav();
        String muffledWav = AudioEffects.applyToneShift(
                baselineWav, MUFFLED_BASS_GAIN_DB, MUFFLED_TREBLE_GAIN_DB);
        TtsUtil.deleteWav(baselineWav);
        String botResponse = sendPreparedQueryAudio(homePage, muffledWav);

        Assert.assertTrue(
                Pattern.compile(BotResponsePatterns.Balance.ANY).matcher(botResponse).find(),
                "[VoiceVariation] Expected a balance response for a muffled-tone query of the "
                + "registered voice.\n  Got: " + botResponse);
    }

    @Test(groups = {"ui", "regression", "botverification"},
            description = "Should authorize a balance query spoken with mild background noise "
                    + "mixed in, simulating a real room instead of a dead-silent recording")
    public void testBackgroundNoiseQueryStillAuthorized() throws Exception {
        HomePage homePage = sharedHomePage;
        String baselineWav = generateBaselineQueryWav();
        String noisyWav = AudioEffects.applyBackgroundNoise(baselineWav, BACKGROUND_NOISE_AMPLITUDE);
        TtsUtil.deleteWav(baselineWav);
        String botResponse = sendPreparedQueryAudio(homePage, noisyWav);

        Assert.assertTrue(
                Pattern.compile(BotResponsePatterns.Balance.ANY).matcher(botResponse).find(),
                "[VoiceVariation] Expected a balance response for a query with mild background "
                + "noise of the registered voice.\n  Got: " + botResponse);
    }

    @Test(groups = {"ui", "regression", "smoke", "negative", "botverification"},
            description = "Should reject a balance query spoken by a completely different (male) "
                    + "voice, not just a shifted version of the registered one")
    public void testDifferentMaleVoiceQueryIsRejected() throws Exception {
        HomePage homePage = sharedHomePage;
        String botResponse = askBalanceWithVoice(homePage, EdgeTtsEngine.VOICE_EN_ALTERNATE, "+0%", "+0Hz");

        Assert.assertFalse(
                Pattern.compile(BotResponsePatterns.Balance.ANY).matcher(botResponse).find(),
                "[VoiceVariation] A completely different (male) voice should NOT receive the "
                + "account balance.\n  Got: " + botResponse);

        Assert.assertTrue(
                Pattern.compile(BotResponsePatterns.Authorization.VOICE_NOT_RECOGNIZED)
                        .matcher(botResponse).find(),
                "[VoiceVariation] Expected an authorization-rejected response for a completely "
                + "different voice.\n  Pattern : " + BotResponsePatterns.Authorization.VOICE_NOT_RECOGNIZED
                + "\n  Got     : " + botResponse);
    }

    @Test(groups = {"ui", "regression", "multilingual"},
            description = "Exploratory: observes how the app handles a Hindi-language balance "
                    + "query against an English-registered voice — no established expectation yet "
                    + "for a cross-language query, so the authorized/rejected outcome itself isn't "
                    + "asserted, only that the app actually responded at all")
    public void testHindiQueryOutcomeObserved() throws Exception {
        HomePage homePage = sharedHomePage;
        String botResponse = sendCustomLanguageQuery(
                homePage, MultilingualVoiceQueries.Hindi.SAVINGS_BALANCE, EdgeTtsEngine.VOICE_HINDI);

        Assert.assertFalse(botResponse.isBlank(),
                "[VoiceVariation] Expected SOME response (authorized, rejected, or a "
                + "language-related fallback) to a Hindi query — got a blank/no response, which "
                + "suggests the query was never actually processed.");
    }

    @Test(groups = {"ui", "regression", "multilingual"},
            description = "Exploratory: observes how the app handles a Marathi-language balance "
                    + "query against an English-registered voice — no established expectation yet "
                    + "for a cross-language query, so the authorized/rejected outcome itself isn't "
                    + "asserted, only that the app actually responded at all. NOTE: this is the "
                    + "test that surfaced a real bug — Marathi consistently gets authorized and "
                    + "leaks the real balance despite using a completely unregistered voice/language.")
    public void testMarathiQueryOutcomeObserved() throws Exception {
        HomePage homePage = sharedHomePage;
        String botResponse = sendCustomLanguageQuery(
                homePage, MultilingualVoiceQueries.Marathi.SAVINGS_BALANCE, EdgeTtsEngine.VOICE_MARATHI);

        Assert.assertFalse(botResponse.isBlank(),
                "[VoiceVariation] Expected SOME response (authorized, rejected, or a "
                + "language-related fallback) to a Marathi query — got a blank/no response, which "
                + "suggests the query was never actually processed.");
    }

    /** Removes the voiceprint exactly once, after every {@code @Test} method in the class has
     * run, then closes the one shared browser/page/context/playwright that every test used (see
     * {@link #sharedHomePage}, {@link #setUpBrowser()}) — nothing else does, since {@link
     * #tearDown} deliberately skips it per-method to keep the session alive across tests. Reuses
     * the still-live {@link #sharedHomePage} session directly rather than logging in again. */
    @AfterClass(alwaysRun = true)
    public void removeVoiceRegistrationAfterAllTests() {
        try {
            if (sharedHomePage != null && sharedHomePage.isVoiceRegistered()) {
                sharedHomePage.removeRegisteredVoice();
                System.out.println("[VoiceVariation] Final cleanup: voice registration removed "
                        + "after all tests in this class.");
            } else {
                System.out.println("[VoiceVariation] Final cleanup: account was already "
                        + "unregistered.");
            }
        } catch (Exception e) {
            System.out.println("[VoiceVariation] WARN — final cleanup (remove voice) failed: "
                    + e.getMessage());
        } finally {
            if (page != null) page.close();
            if (context != null) context.close();
            if (browser != null) browser.close();
            if (playwright != null) playwright.close();
        }
    }
}
