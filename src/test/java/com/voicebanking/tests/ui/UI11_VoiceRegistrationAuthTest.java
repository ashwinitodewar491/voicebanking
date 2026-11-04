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
import com.voicebanking.utils.VoiceEnrollment;
import com.voicebanking.utils.tts.EdgeTtsEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Voice authentication: enrolls Leena Kamat's voiceprint, then checks a balance query is authorized
 * in the same voice and rejected in a different one. The voiceprint is removed after each test. */
public class UI11_VoiceRegistrationAuthTest extends BasePage {

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

    /** Adds the Chromium fake-media args and mic-stream tracker needed for real fake-audio capture,
     * which BasePage's launcher lacks. */
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

    /** Logs in as Leena Kamat, enrolls her voice (via the menu if the app doesn't auto-prompt), and
     * lands on Home. Regenerates the enrollment WAV first since the file is shared with
     * {@link #askBalanceWithVoice}. */
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
        } catch (PlaywrightException notAutoPrompted) {
            // The app no longer auto-prompts for registration on every login once a user has
            // skipped it once — confirmed as an intentional app change, not a bug: registering
            // later requires going through the user menu's own "Register your voice" option
            // instead. So "the screen didn't show up" no longer means "already registered" the
            // way it used to for Leena Kamat's known-enrolled account (see class Javadoc) —
            // check the menu directly instead of assuming, and force registration through it if
            // the account genuinely has no voiceprint yet.
            HomePage homePage = new HomePage(page);
            homePage.waitForPageLoad();

            if (homePage.isVoiceRegistered()) {
                return homePage;
            }

            System.out.println("[VoiceRegistration] Registration screen didn't auto-appear but "
                    + "the account isn't actually registered — forcing it via the user menu.");
            homePage.clickRegisterVoiceFromMenu();
            voicePage.waitForPageLoad();
        }

        voicePage.checkConsent();
        voicePage.clickStartRegistration();

        // Image steps then question steps, until Start Banking — see VoiceEnrollment.
        VoiceEnrollment.completeAllSteps(voicePage, generatedWavPath, EdgeTtsEngine.VOICE, "VoiceRegistration");

        voicePage.clickStartBanking();

        HomePage homePage = new HomePage(page);
        homePage.waitForPageLoad();
        return homePage;
    }

    /** The bot's generic session-start greeting (copy of BaseVoiceTest's; this class extends
     * BasePage). */
    private static final Pattern GENERIC_GREETING = Pattern.compile("Welcome.*How can I help you today");

    /** The bot's "didn't understand" fallback (copy of BaseVoiceTest's). */
    private static final Pattern CONTEXT_LOST_FALLBACK =
            Pattern.compile("(?i)didn.t understand that.*what would you like to do");

    private static final int MAX_REASK_ATTEMPTS = 3;

    /** Speaks the balance query in {@code voice} and returns the response, re-asking up to
     * {@link #MAX_REASK_ATTEMPTS} times on a greeting, fallback or blank reply. */
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

    /** Removes the voiceprint so the next run enrolls fresh. Swallows its own errors so they don't
     * mask the test result. */
    private void removeVoiceRegistration(HomePage homePage) {
        try {
            homePage.removeRegisteredVoice();
        } catch (Exception e) {
            System.out.println("[VoiceRegistration] WARN — cleanup (remove voice) failed: " + e.getMessage());
        }
    }
}
