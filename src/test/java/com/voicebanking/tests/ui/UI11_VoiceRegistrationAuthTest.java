package com.voicebanking.tests.ui;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Playwright;
import com.voicebanking.DataText.BotResponsePatterns;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.DataText.VoiceQueries;
import com.voicebanking.pages.BasePage;
import com.voicebanking.pages.HomePage;
import com.voicebanking.pages.LanguagePage;
import com.voicebanking.pages.OtpPage;
import com.voicebanking.pages.VoiceRegistrationPage;
import com.voicebanking.pages.WelcomePage;
import com.voicebanking.utils.NoResponseTracker;
import com.voicebanking.utils.TtsUtil;
import com.voicebanking.utils.tts.EdgeTtsEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Covers voice-based authentication: enroll a voiceprint via the 3-repetition registration flow,
 * then verify a later balance query is authorized when spoken in the same voice and rejected
 * when spoken in a different one.
 *
 * <p>Logs in as Leena Kamat (9812341042) rather than a fresh random-phone registration — stage
 * currently rejects unregistered numbers before the OTP screen even loads, which this whole flow
 * depends on. Each test method enrolls her voice fresh via {@link #registerVoiceAndReachHome},
 * then removes it again in a {@code finally} block ({@link #removeVoiceRegistration}) via the
 * app's own "Remove your voice" menu action, so the next method/run starts from an unregistered
 * state again. {@link #registerVoiceAndReachHome} still has a returning-user fallback in case
 * cleanup itself ever fails to run (e.g. a crash before the {@code finally} block) — if a run's
 * own recording gets treated as "already registered" instead of walking the fresh-enrollment
 * screens, that's the first thing to check: a prior run's cleanup didn't complete.
 *
 * <p>Re-record/Submit are always both present after a take completes regardless of quality — the
 * app additionally shows a "Recording not accepted" dialog on top of that bar, but only when a
 * take genuinely fails its quality check, so that dialog's heading (not Re-record's mere presence)
 * is what {@link VoiceRegistrationPage#waitForRecordingAccepted(int)} polls for.
 */
public class UI11_VoiceRegistrationAuthTest extends BasePage {

    private static final int ENROLLMENT_REPS = 3;
    private static final int MAX_TAKES_PER_REP = 3;

    private String generatedWavPath;

    @BeforeClass(alwaysRun = true)
    public void setAudioFile() throws Exception {
        generatedWavPath = TtsUtil.generateWav(
                VoiceQueries.English.VOICE_ENROLLMENT_PHRASE, EdgeTtsEngine.VOICE);
    }

    @AfterClass(alwaysRun = true)
    public void clearAudioFile() {
        TtsUtil.deleteWav(generatedWavPath);
    }

    /**
     * Overrides BasePage's launcher: that one is missing {@code --use-fake-ui-for-media-stream}
     * (it was only ever exercised with a JS-level getUserMedia mock, never real fake-audio-capture
     * — see UI6_HomePageTest's speechMockScript usage), so a genuine getUserMedia() call here hits
     * Chromium's native permission flow and fails with "Could not access the microphone" even
     * though the context already grants the "microphone" permission. This mirrors
     * BaseVoiceTest.setUpBrowserWithAudio's fuller arg list, plus the same window.__micStreams
     * tracker {@code reacquireMicrophoneForFollowUp()} relies on to force a fresh stream when the
     * on-disk WAV is swapped from the enrollment phrase to the post-registration balance query.
     */
    @Override
    @BeforeMethod(alwaysRun = true)
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

        // Headless Chromium's Web Audio API doesn't propagate the --use-file-for-fake-audio-capture
        // stream into AnalyserNode-based analysis (a known Chromium headless bug), even though the
        // same synthetic signal correctly reaches WebRTC/the backend. The frontend's useMicLevel
        // hook uses AnalyserNode.getByteFrequencyData() to locally gate whether a voice signal is
        // present (threshold 0.018) before accepting a recording, so it always reads zeros and
        // rejects every take. This forces getByteFrequencyData to report full-volume signal so
        // useMicLevel always detects a voice, matching what the backend already receives for real.
        page.addInitScript(
                "(function() {"
                + "  window.AnalyserNode.prototype.getByteFrequencyData = function(array) {"
                + "    for (var i = 0; i < array.length; i++) { array[i] = 255; }"
                + "  };"
                + "})();");
    }

    /**
     * Logs in as Leena Kamat (CIF202602260042, 9812341042) and completes the 3x
     * voice-registration enrollment flow (consent → Start Registration → tap mic/speak/Submit
     * x3), then clicks Start Banking to land on Home. Was previously a fresh random-phone
     * registration, but stage no longer accepts unregistered numbers at the OTP step (confirmed
     * live: "Send OTP" never surfaces the OTP screen for a random number) — Leena Kamat is used
     * instead as a real, known account with no beneficiary/loan history to interfere. NOTE: this
     * permanently enrolls her voiceprint on stage; there's currently no known way (API or UI) to
     * remove/reset it afterward, so repeat runs against her account may behave like a returning,
     * already-registered user rather than re-enrolling — see the class Javadoc.
     *
     * <p>Regenerates the enrollment phrase into {@code generatedWavPath} first — that path is a
     * single class-level file shared with {@link #askBalanceWithVoice}, which overwrites it
     * in-place with a balance-query recording. Since both test methods in this class share that
     * one file, whichever method runs second would otherwise launch enrollment against the first
     * method's leftover balance-query audio instead of the enrollment phrase.
     */
    private HomePage registerVoiceAndReachHome() throws Exception {
        String enrollWavPath = TtsUtil.generateWav(
                VoiceQueries.English.VOICE_ENROLLMENT_PHRASE, EdgeTtsEngine.VOICE);
        Files.copy(Path.of(enrollWavPath), Path.of(generatedWavPath), StandardCopyOption.REPLACE_EXISTING);
        TtsUtil.deleteWav(enrollWavPath);

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
        } catch (PlaywrightException alreadyRegistered) {
            // Leena Kamat already has a voiceprint enrolled from an earlier run on this account
            // (see class Javadoc) — the app skips straight past registration for her, the same
            // way LanguagePage is skipped for any other returning user above.
            HomePage returningHomePage = new HomePage(page);
            returningHomePage.waitForPageLoad();
            return returningHomePage;
        }

        voicePage.checkConsent();
        voicePage.clickStartRegistration();

        for (int rep = 1; rep <= ENROLLMENT_REPS; rep++) {
            recordAcceptedTake(voicePage, rep);
            voicePage.clickSubmit();
        }

        voicePage.clickStartBanking();

        HomePage homePage = new HomePage(page);
        homePage.waitForPageLoad();
        return homePage;
    }

    /**
     * Records one enrollment repetition. Reuses the single class-level WAV for every tap rather
     * than swapping in fresh audio per take — this screen opens the microphone once and keeps
     * that same stream alive across all 3 recording attempts (unlike the hold-to-speak
     * balance-query flow, which opens a fresh stream per press). If a take is rejected, Re-record
     * is clicked and retried against the same stream.
     */
    private void recordAcceptedTake(VoiceRegistrationPage voicePage, int rep) throws Exception {
        for (int take = 1; take <= MAX_TAKES_PER_REP; take++) {
            System.out.println("[VoiceRegistration] Enrollment attempt " + rep + " of " + ENROLLMENT_REPS
                    + " (take " + take + " of " + MAX_TAKES_PER_REP + ")...");

            voicePage.tapMicAndRecord();

            if (voicePage.waitForRecordingAccepted(5000)) return;

            System.out.println("[VoiceRegistration] Recording not accepted — re-recording...");
            voicePage.clickRerecord();
        }
        throw new RuntimeException("Recording rejected " + MAX_TAKES_PER_REP
                + " times in a row for enrollment attempt " + rep + " — giving up");
    }

    /** Matches the bot's generic session-start greeting, e.g. "Good afternoon, Welcome Karan
     * Malhotra, I can help you review your recent transactions or check your account
     * balances.How can I help you today?" — mirrors BaseVoiceTest#isGenericGreeting (not reusable
     * here since this class extends BasePage, not BaseVoiceTest — see this class's own doc comment
     * on {@link #setUpBrowser} for why). Observed live replacing the real answer in
     * {@link #askBalanceWithVoice} the same way BaseVoiceTest's re-ask loop was built to recover
     * from: a reconnect re-establishes the session but the query spoken right after it gets
     * swallowed by the bot's own session-start greeting instead of being answered. */
    private static final Pattern GENERIC_GREETING = Pattern.compile("Welcome.*How can I help you today");

    /** Matches the bot's generic "I didn't understand" capability-reset fallback — mirrors
     * BaseVoiceTest#isContextLostFallback, same reason this can't just call that method directly. */
    private static final Pattern CONTEXT_LOST_FALLBACK =
            Pattern.compile("(?i)didn.t understand that.*what would you like to do");

    private static final int MAX_REASK_ATTEMPTS = 3;

    /**
     * Swaps the on-disk WAV to "What is my account balance" spoken in {@code voice}, forces the
     * app to request a fresh mic stream so it actually picks up the swapped file, then holds to
     * speak and returns the bot's response. Re-asks with the same audio, up to
     * {@link #MAX_REASK_ATTEMPTS} times, on a generic greeting/context-lost fallback/blank
     * response — same recovery BaseVoiceTest#runVoiceQuery uses, needed here because this single
     * shot had none: confirmed live via testPositiveVoiceMatchIsAuthorized failing on a stuck
     * session-start greeting ("Good afternoon, Welcome Leena Kamat...How can I help you today?")
     * instead of the balance, which a same-audio retry recovers from.
     */
    private String askBalanceWithVoice(HomePage homePage, String voice) throws Exception {
        String queryWavPath = TtsUtil.generateWav(VoiceQueries.English.ACCOUNT_BALANCE, voice);
        Files.copy(Path.of(queryWavPath), Path.of(generatedWavPath), StandardCopyOption.REPLACE_EXISTING);
        int holdMs = (int) TtsUtil.getWavDurationMs(generatedWavPath);
        TtsUtil.deleteWav(queryWavPath);

        homePage.reacquireMicrophoneForFollowUp();
        homePage.holdToSpeakWithRetry(holdMs, 3, 8000);
        homePage.waitForVoiceResponse(15000);

        String transcribed = homePage.getLastTranscribedText();
        String botResponse = homePage.getLastBotResponse();
        System.out.println("[VoiceAuth] Voice used   : " + voice);
        System.out.println("[VoiceAuth] Transcribed  : " + transcribed);
        System.out.println("[VoiceAuth] Bot response : " + botResponse);

        for (int reaskNum = 1;
             reaskNum <= MAX_REASK_ATTEMPTS
                     && (GENERIC_GREETING.matcher(botResponse).find()
                        || CONTEXT_LOST_FALLBACK.matcher(botResponse).find()
                        || botResponse.isBlank());
             reaskNum++) {
            if (botResponse.isBlank()) {
                NoResponseTracker.recordOccurrence("VoiceAuth: " + voice);
            }
            System.out.println("[VoiceAuth] WARN — got a generic greeting/fallback/empty response"
                    + " instead of an answer (stuck Processing, or post-reconnect) — re-asking ("
                    + reaskNum + " of " + MAX_REASK_ATTEMPTS + ")...");
            homePage.reacquireMicrophoneForFollowUp();
            homePage.holdToSpeakWithRetry(holdMs, 3, 8000);
            homePage.waitForVoiceResponse(15000);
            transcribed = homePage.getLastTranscribedText();
            botResponse = homePage.getLastBotResponse();
            System.out.println("[VoiceAuth] Re-ask " + reaskNum + " Transcribed : " + transcribed);
            System.out.println("[VoiceAuth] Re-ask " + reaskNum + " Bot response: " + botResponse);
        }

        return botResponse;
    }

    @Test(groups = {"ui", "regression", "smoke"},
            description = "Should authorize a balance query spoken in the same voice used to register")
    public void testPositiveVoiceMatchIsAuthorized() throws Exception {
        HomePage homePage = registerVoiceAndReachHome();

        try {
            String botResponse = askBalanceWithVoice(homePage, EdgeTtsEngine.VOICE);

            Assert.assertTrue(
                    Pattern.compile(BotResponsePatterns.Balance.ANY).matcher(botResponse).find(),
                    "[VoiceAuth] Expected a balance response for the registered voice.\n"
                    + "  Pattern : " + BotResponsePatterns.Balance.ANY + "\n"
                    + "  Got     : " + botResponse);
        } finally {
            removeVoiceRegistration(homePage);
        }
    }

    @Test(groups = {"ui", "regression", "smoke"},
            description = "Should reject a balance query spoken in a voice different from the one used to register")
    public void testNegativeVoiceMismatchIsRejected() throws Exception {
        HomePage homePage = registerVoiceAndReachHome();

        try {
            String botResponse = askBalanceWithVoice(homePage, EdgeTtsEngine.VOICE_EN_ALTERNATE);

            Assert.assertFalse(
                    Pattern.compile(BotResponsePatterns.Balance.ANY).matcher(botResponse).find(),
                    "[VoiceAuth] A voice that does not match the registered voice should NOT receive "
                    + "the account balance.\n  Got: " + botResponse);

            Assert.assertTrue(
                    Pattern.compile(BotResponsePatterns.Authorization.VOICE_NOT_RECOGNIZED)
                            .matcher(botResponse).find(),
                    "[VoiceAuth] Expected an authorization-rejected response for the mismatched voice.\n"
                    + "  Pattern : " + BotResponsePatterns.Authorization.VOICE_NOT_RECOGNIZED + "\n"
                    + "  Got     : " + botResponse);
        } finally {
            removeVoiceRegistration(homePage);
        }
    }

    /**
     * Cleans up Leena Kamat's voiceprint via the "Remove your voice" flow (user menu → Remove
     * your voice → confirm Remove) so the next run of this class re-enrolls fresh instead of
     * hitting the returning-user branch in {@link #registerVoiceAndReachHome}. Runs in a
     * {@code finally} block so it still fires when the test's own assertions fail — otherwise a
     * failing run would leave the account registered and break every subsequent run. Swallows its
     * own failures rather than masking the real test result with an unrelated cleanup error.
     */
    private void removeVoiceRegistration(HomePage homePage) {
        try {
            homePage.removeRegisteredVoice();
        } catch (Exception e) {
            System.out.println("[VoiceRegistration] WARN — cleanup (remove voice) failed: " + e.getMessage());
        }
    }
}
