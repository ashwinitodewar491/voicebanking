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
 * <p>Known limitation: the app's enrollment-recording quality check currently rejects every take
 * this suite has thrown at it — synthetic edge-tts audio and a real human-recorded WAV alike,
 * across many runs, with real non-zero mic signal confirmed reaching the browser each time. That
 * rules out audio content as the cause; it looks like the check may be sensitive to something
 * about the fake-audio-capture device itself, independent of what plays through it. Needs
 * verification outside test automation (manual registration on stage, or checking with the dev
 * team what the quality check actually inspects) before the enrollment step here can be expected
 * to pass reliably.
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
    }

    /**
     * Logs in as a fresh random-phone user and completes the 3x voice-registration enrollment
     * flow (consent → Start Registration → tap mic/speak/Submit x3), then clicks Start Banking
     * to land on Home.
     */
    private HomePage registerVoiceAndReachHome() throws Exception {
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
            languagePage.selectEnglish();
            languagePage.clickContinue();
        } catch (PlaywrightException ignored) {
            // language page absent for returning users
        }

        VoiceRegistrationPage voicePage = new VoiceRegistrationPage(page);
        voicePage.waitForPageLoad();
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

    /**
     * Swaps the on-disk WAV to "What is my account balance" spoken in {@code voice}, forces the
     * app to request a fresh mic stream so it actually picks up the swapped file, then holds to
     * speak and returns the bot's response.
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
        return botResponse;
    }

    @Test(groups = {"ui", "regression"},
            description = "Should authorize a balance query spoken in the same voice used to register")
    public void testPositiveVoiceMatchIsAuthorized() throws Exception {
        HomePage homePage = registerVoiceAndReachHome();

        String botResponse = askBalanceWithVoice(homePage, EdgeTtsEngine.VOICE);

        Assert.assertTrue(
                Pattern.compile(BotResponsePatterns.Balance.ANY).matcher(botResponse).find(),
                "[VoiceAuth] Expected a balance response for the registered voice.\n"
                + "  Pattern : " + BotResponsePatterns.Balance.ANY + "\n"
                + "  Got     : " + botResponse);
    }

    @Test(groups = {"ui", "regression"},
            description = "Should reject a balance query spoken in a voice different from the one used to register")
    public void testNegativeVoiceMismatchIsRejected() throws Exception {
        HomePage homePage = registerVoiceAndReachHome();

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
    }
}
