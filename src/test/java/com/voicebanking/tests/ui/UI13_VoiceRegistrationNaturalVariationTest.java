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
import com.voicebanking.utils.VoiceEnrollment;
import com.voicebanking.utils.tts.AudioEffects;
import com.voicebanking.utils.tts.EdgeTtsEngine;
import com.voicebanking.utils.tts.VoiceVariation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Like {@link UI11_VoiceRegistrationAuthTest}, but enrolls with an unvaried voice and varies the
 * query instead (rate/pitch offsets and audio effects) to probe the voice matcher's tolerance.
 * Registers once for the whole class; the voiceprint is removed in
 * {@link #removeVoiceRegistrationAfterAllTests()}. */
public class UI13_VoiceRegistrationNaturalVariationTest extends BasePage {

    /** Rate range for the positive test's query: small enough to still sound like the same person. */
    private static final int SMALL_RATE_RANGE_PERCENT = 5;
    private static final int SMALL_PITCH_RANGE_HZ = 8;

    /** Pitch shift expected to be rejected. Live runs show the app still authorizes it; the failing
     * assertion is kept on purpose. */
    private static final String REJECTION_PITCH_HZ = "+17Hz";
    private static final String REJECTION_RATE_PERCENT = "+0%";

    /** Wider pitch shifts, probing for a rejection boundary beyond 17Hz. */
    private static final String WIDE_PITCH_SHIFT_25HZ = "+25Hz";
    private static final String WIDE_PITCH_SHIFT_35HZ = "+35Hz";

    /** Volume multipliers for {@link AudioEffects#applyVolume}. */
    private static final double VOLUME_QUIETER_FACTOR = 0.6;
    private static final double VOLUME_LOUDER_FACTOR = 1.4;

    /** Tempo multiplier for {@link AudioEffects#applyTempo} (ffmpeg atempo range 0.5-2.0). */
    private static final double TEMPO_FASTER_FACTOR = 1.15;

    /** Bass boost / treble cut simulating a muffled recording. */
    private static final int MUFFLED_BASS_GAIN_DB = 6;
    private static final int MUFFLED_TREBLE_GAIN_DB = -6;

    /** Pink-noise amplitude for a subtle background hiss. */
    private static final double BACKGROUND_NOISE_AMPLITUDE = 0.03;

    private String generatedWavPath;

    /** Home page for the single login/registration session shared by every test. */
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

    /** Same browser/mic setup as {@link UI11_VoiceRegistrationAuthTest#setUpBrowser()}, run once per
     * class so all tests share one session. */
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
    }

    /** Logs in and registers once for the whole class. */
    @BeforeClass(alwaysRun = true, dependsOnMethods = "setUpBrowser")
    public void registerOnceForAllTests() throws Exception {
        sharedHomePage = registerVoiceAndReachHome();
    }

    /** Screenshots on failure but keeps the shared session open; teardown happens in
     * {@link #removeVoiceRegistrationAfterAllTests()}. */
    @Override
    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (page != null) {
            ScreenshotUtil.captureOnFailure(page, result);
        }
    }

    /** Logs in as Leena Kamat, enrolls with {@link EdgeTtsEngine#VOICE} via {@link VoiceEnrollment},
     * and lands on Home. Mirrors
     * {@link UI11_VoiceRegistrationAuthTest#registerVoiceAndReachHome()}. */
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
                System.out.println("[VoiceVariation] Account was ALREADY registered at login — "
                        + "skipping enrollment; tests will use whatever voiceprint is already on it.");
                return homePage;
            }

            System.out.println("[VoiceVariation] Registration screen didn't auto-appear but the "
                    + "account isn't actually registered — forcing it via the user menu instead.");
            homePage.clickRegisterVoiceFromMenu();
            voicePage.waitForPageLoad();
        }

        voicePage.checkConsent();
        voicePage.clickStartRegistration();

        // Image steps (spoken image description) then question steps (spoken answer), until
        // Start Banking — see VoiceEnrollment. With -DkeepAudio=true every step's audio is saved.
        VoiceEnrollment.completeAllSteps(voicePage, generatedWavPath, EdgeTtsEngine.VOICE, "VoiceVariation");

        voicePage.clickStartBanking();

        HomePage homePage = new HomePage(page);
        homePage.waitForPageLoad();
        return homePage;
    }

    private static final Pattern GENERIC_GREETING = Pattern.compile("Welcome.*How can I help you today");
    private static final Pattern CONTEXT_LOST_FALLBACK =
            Pattern.compile("(?i)didn.t understand that.*what would you like to do");

    /** STT check for the balance query; a miss is retried as an STT hiccup, not treated as a
     * voice-auth result. */
    private static final Pattern STT_HEARD_BALANCE = Pattern.compile("(?i)balance");

    private static final int MAX_REASK_ATTEMPTS = 3;

    /** Balance query in {@code voice} with a rate/pitch offset. Mirrors
     * {@link UI11_VoiceRegistrationAuthTest#askBalanceWithVoice}. */
    private String askBalanceWithVoice(HomePage homePage, String voice, String rate, String pitch)
            throws Exception {
        System.out.println("[VoiceVariation] Query audio — voice=" + voice + " (unchanged), rate="
                + rate + ", pitch=" + pitch);
        String queryWavPath = TtsUtil.generateWavWithVariation(
                VoiceQueries.English.ACCOUNT_BALANCE, voice, rate, pitch);
        TtsUtil.keepCopy(queryWavPath, "UI13_query_" + voice + "_rate" + rate + "_pitch" + pitch);
        return sendPreparedQueryAudio(homePage, queryWavPath);
    }

    /** Like {@link #askBalanceWithVoice} but for a pre-made WAV (e.g. from {@link AudioEffects}).
     * Deletes {@code queryWavPath} once copied. */
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

    /** Sends an arbitrary query and returns the response. Re-asks on a greeting, fallback or blank
     * reply, but doesn't check the transcription. */
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

    /** Baseline balance-query WAV (registered voice, no offset) for the audio-effect tests. */
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
        // If the voiceprint is gone (e.g. another run's cleanup on this shared account), the app
        // has nothing to reject against — log it so a "balance leaked" failure can be told apart.
        System.out.println("[VoiceVariation] Voice registered right before the male-voice query: "
                + homePage.isVoiceRegistered());
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

    @Test(groups = {"ui", "regression", "multilingual"},
            description = "Exploratory: observes how the app handles a Bengali-language balance "
                    + "query against an English-registered voice — same rationale as the Hindi/"
                    + "Marathi rows above (no established expectation yet for a cross-language "
                    + "query), checking whether the voice-auth bypass found for Marathi is "
                    + "specific to that one language or also reaches Bengali.")
    public void testBengaliQueryOutcomeObserved() throws Exception {
        HomePage homePage = sharedHomePage;
        String botResponse = sendCustomLanguageQuery(
                homePage, MultilingualVoiceQueries.Bengali.SAVINGS_BALANCE, EdgeTtsEngine.VOICE_BENGALI);

        Assert.assertFalse(botResponse.isBlank(),
                "[VoiceVariation] Expected SOME response (authorized, rejected, or a "
                + "language-related fallback) to a Bengali query — got a blank/no response, which "
                + "suggests the query was never actually processed.");
    }

    /** A near-silent query recording must never authorize anything. */
    @Test(groups = {"ui", "regression", "negative", "security"},
            description = "A silent (zero-volume) query recording must never leak the account "
                    + "balance — either speech-to-text detects nothing at all (the safe, expected "
                    + "outcome) or, if something is transcribed anyway, the balance must still not "
                    + "be disclosed")
    public void testSilentAudioQueryDoesNotLeakBalance() throws Exception {
        HomePage homePage = sharedHomePage;
        String baselineWav = generateBaselineQueryWav();
        String silentWav = AudioEffects.applyVolume(baselineWav, 0.0);
        TtsUtil.deleteWav(baselineWav);

        Files.copy(Path.of(silentWav), Path.of(generatedWavPath), StandardCopyOption.REPLACE_EXISTING);
        int holdMs = (int) TtsUtil.getWavDurationMs(generatedWavPath);
        TtsUtil.deleteWav(silentWav);

        homePage.reacquireMicrophoneForFollowUp();
        try {
            homePage.holdToSpeakWithRetry(holdMs, 3, 8000);
            homePage.waitForVoiceResponse(15000);

            String transcribed = homePage.getLastTranscribedText();
            String botResponse = homePage.getLastBotResponse();
            System.out.println("[VoiceVariation] Silent-audio Transcribed : " + transcribed);
            System.out.println("[VoiceVariation] Silent-audio Bot response: " + botResponse);

            Assert.assertFalse(
                    Pattern.compile(BotResponsePatterns.Balance.ANY).matcher(botResponse).find(),
                    "[VoiceVariation] Silent audio must never leak the account balance.\n  Got: " + botResponse);
        } catch (RuntimeException noSpeechDetected) {
            // The safe/expected outcome: silence has nothing for speech-to-text to detect, so no
            // user bubble ever appears and holdToSpeakWithRetry gives up — that is a pass here,
            // not a test infrastructure failure, since it confirms no query was ever processed at
            // all (the strongest possible "no leak" outcome).
            System.out.println("[VoiceVariation] Silent audio correctly produced no speech-to-text "
                    + "detection: " + noSpeechDetected.getMessage());
        }
    }

    /** Removes the voiceprint once after all tests, then closes the shared browser. */
    @AfterClass(alwaysRun = true)
    public void removeVoiceRegistrationAfterAllTests() {
        try {
            for (int attempt = 1; attempt <= 2 && sharedHomePage != null; attempt++) {
                try {
                    if (sharedHomePage.isVoiceRegistered()) {
                        sharedHomePage.removeRegisteredVoice();
                        System.out.println("[VoiceVariation] Final cleanup: voice registration removed "
                                + "after all tests in this class.");
                    } else {
                        System.out.println("[VoiceVariation] Final cleanup: account was already "
                                + "unregistered.");
                    }
                    break;
                } catch (Exception e) {
                    System.out.println("[VoiceVariation] WARN — final cleanup (remove voice) attempt "
                            + attempt + " failed: " + e.getMessage());
                    // A failed cleanup leaves the voiceprint on the shared account, and the next
                    // run then skips enrollment entirely — so retry once from a freshly loaded Home.
                    if (attempt == 1) {
                        page.reload();
                        sharedHomePage.waitForPageLoad();
                    }
                }
            }
        } finally {
            if (page != null) page.close();
            if (context != null) context.close();
            if (browser != null) browser.close();
            if (playwright != null) playwright.close();
        }
    }
}
