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
import com.voicebanking.DataText.Constants;
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
import com.voicebanking.utils.TtsUtil;
import com.voicebanking.utils.VoiceEnrollment;
import com.voicebanking.utils.tts.EdgeTtsEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Every existing voice-match test ({@link UI11_VoiceRegistrationAuthTest}, {@link
 * UI13_VoiceRegistrationNaturalVariationTest}) gates a single intent: a read-only balance query.
 * None of them ever check whether voice-print authorization applies to <b>transferring money</b> —
 * the one action on this app that actually moves funds, and the one a voice-auth bypass would
 * matter most for. Separately, {@link UI13_VoiceRegistrationNaturalVariationTest#testMarathiQueryOutcomeObserved}
 * found that a Marathi-language balance query bypasses voice matching entirely and leaks the
 * balance despite using a completely unregistered voice/language. This class checks whether that
 * same bypass reaches transfer-money initiation too, which would be a materially more severe
 * finding than a read-only leak.
 *
 * <p>Uses Leena Kamat (Customer B, {@link Constants#CUSTOMER_B_PHONE}) — the same account {@link
 * UI11_VoiceRegistrationAuthTest} and {@link UI13_VoiceRegistrationNaturalVariationTest} already
 * use for voice-print testing, chosen there specifically for having no beneficiary/loan history to
 * interfere (see their javadocs). Her having no beneficiary means a transfer can never actually
 * complete here — deliberately fine, since every test below only cares whether the app's
 * <b>authorization gate</b> fires on the very first response to a generic, no-beneficiary-named
 * transfer query ({@link VoiceQueries.English#CAN_TRANSFER_MONEY}), not whether a real transfer
 * goes through. No real money ever moves in this class.
 */
public class UI15_VoicePrintTransferAuthTest extends BasePage {

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

    /** Same browser/mic setup as {@link UI11_VoiceRegistrationAuthTest#setUpBrowser()} — see that
     * method's javadoc for why the extra Chromium args and the two init scripts are needed. */
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

        page.addInitScript(
                "(function() {"
                + "  window.AnalyserNode.prototype.getByteFrequencyData = function(array) {"
                + "    for (var i = 0; i < array.length; i++) { array[i] = 255; }"
                + "  };"
                + "})();");
    }

    /** Same as {@link UI11_VoiceRegistrationAuthTest#registerVoiceAndReachHome()} — logs in as
     * Leena Kamat and completes the voice-registration enrollment flow (every image/question
     * step, via {@link VoiceEnrollment}), then lands on Home. */
    private HomePage registerVoiceAndReachHome() throws Exception {
        String enrollWavPath = TtsUtil.generateWav(
                VoiceQueries.English.VOICE_ENROLLMENT_PHRASE, EdgeTtsEngine.VOICE);
        Files.copy(Path.of(enrollWavPath), Path.of(generatedWavPath), StandardCopyOption.REPLACE_EXISTING);
        TtsUtil.deleteWav(enrollWavPath);

        WelcomePage welcomePage = new WelcomePage(page, Endpoints.getUiBaseUrl());
        welcomePage.navigate();
        welcomePage.dismissPwaPopupIfPresent();
        welcomePage.enterPhoneNumber(Constants.CUSTOMER_B_PHONE);
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
            HomePage homePage = new HomePage(page);
            homePage.waitForPageLoad();

            if (homePage.isVoiceRegistered()) {
                return homePage;
            }

            System.out.println("[TransferAuth] Registration screen didn't auto-appear but the "
                    + "account isn't actually registered — forcing it via the user menu.");
            homePage.clickRegisterVoiceFromMenu();
            voicePage.waitForPageLoad();
        }

        voicePage.checkConsent();
        voicePage.clickStartRegistration();

        // Image steps then question steps, until Start Banking — see VoiceEnrollment.
        VoiceEnrollment.completeAllSteps(voicePage, generatedWavPath, EdgeTtsEngine.VOICE, "TransferAuth");

        voicePage.clickStartBanking();

        HomePage homePage = new HomePage(page);
        homePage.waitForPageLoad();
        return homePage;
    }

    private static final Pattern GENERIC_GREETING = Pattern.compile("Welcome.*How can I help you today");
    private static final Pattern CONTEXT_LOST_FALLBACK =
            Pattern.compile("(?i)didn.t understand that.*what would you like to do");
    private static final int MAX_REASK_ATTEMPTS = 3;

    /** Speaks {@code queryText} in {@code voice} and returns the bot's first response, retrying on
     * a generic greeting/context-lost fallback/blank response the same way {@link
     * UI11_VoiceRegistrationAuthTest#askBalanceWithVoice} does. Generalized to an arbitrary query
     * text (not just the balance phrase) since this class probes transfer-money initiation in both
     * English and Marathi. */
    private String askWithVoice(HomePage homePage, String queryText, String voice) throws Exception {
        String queryWavPath = TtsUtil.generateWav(queryText, voice);
        Files.copy(Path.of(queryWavPath), Path.of(generatedWavPath), StandardCopyOption.REPLACE_EXISTING);
        int holdMs = (int) TtsUtil.getWavDurationMs(generatedWavPath);
        TtsUtil.deleteWav(queryWavPath);

        homePage.reacquireMicrophoneForFollowUp();
        homePage.holdToSpeakWithRetry(holdMs, 3, 8000);
        homePage.waitForVoiceResponse(15000);

        String transcribed = homePage.getLastTranscribedText();
        String botResponse = homePage.getLastBotResponse();
        System.out.println("[TransferAuth] Voice used   : " + voice);
        System.out.println("[TransferAuth] Query text   : " + queryText);
        System.out.println("[TransferAuth] Transcribed  : " + transcribed);
        System.out.println("[TransferAuth] Bot response : " + botResponse);

        for (int reaskNum = 1;
             reaskNum <= MAX_REASK_ATTEMPTS
                     && (GENERIC_GREETING.matcher(botResponse).find()
                        || CONTEXT_LOST_FALLBACK.matcher(botResponse).find()
                        || botResponse.isBlank());
             reaskNum++) {
            if (botResponse.isBlank()) {
                NoResponseTracker.recordOccurrence("TransferAuth: " + voice);
            }
            System.out.println("[TransferAuth] WARN — got a generic greeting/fallback/empty response"
                    + " instead of an answer — re-asking (" + reaskNum + " of " + MAX_REASK_ATTEMPTS + ")...");
            homePage.reacquireMicrophoneForFollowUp();
            homePage.holdToSpeakWithRetry(holdMs, 3, 8000);
            homePage.waitForVoiceResponse(15000);
            transcribed = homePage.getLastTranscribedText();
            botResponse = homePage.getLastBotResponse();
            System.out.println("[TransferAuth] Re-ask " + reaskNum + " Transcribed : " + transcribed);
            System.out.println("[TransferAuth] Re-ask " + reaskNum + " Bot response: " + botResponse);
        }

        return botResponse;
    }

    @Test(groups = {"ui", "regression", "smoke", "security"},
            description = "A transfer-money query spoken in the registered voice should not be "
                    + "rejected as an authorization failure — the voice-auth gate should let a "
                    + "legitimate caller's own voice through to the transfer flow, same as it "
                    + "does for balance queries")
    public void testPositiveVoiceMatchIsNotRejectedForTransferInitiation() throws Exception {
        HomePage homePage = registerVoiceAndReachHome();

        try {
            String botResponse = askWithVoice(homePage, VoiceQueries.English.CAN_TRANSFER_MONEY, EdgeTtsEngine.VOICE);

            Assert.assertFalse(
                    Pattern.compile(BotResponsePatterns.Authorization.VOICE_NOT_RECOGNIZED)
                            .matcher(botResponse).find(),
                    "[TransferAuth] A transfer-money query in the registered voice must not be "
                    + "rejected as an authorization failure.\n  Got: " + botResponse);
            Assert.assertFalse(botResponse.isBlank(),
                    "[TransferAuth] Expected the bot to engage with the transfer request "
                    + "(beneficiary prompt, no-beneficiaries message, etc.), not stay silent.");
        } finally {
            removeVoiceRegistration(homePage);
        }
    }

    @Test(groups = {"ui", "regression", "smoke", "negative", "security"},
            description = "A transfer-money query spoken in a voice different from the one "
                    + "registered should be rejected the same way a mismatched balance query is — "
                    + "voice-auth gating must not be balance-only")
    public void testNegativeVoiceMismatchRejectsTransferInitiation() throws Exception {
        HomePage homePage = registerVoiceAndReachHome();

        try {
            String botResponse = askWithVoice(
                    homePage, VoiceQueries.English.CAN_TRANSFER_MONEY, EdgeTtsEngine.VOICE_EN_ALTERNATE);

            Assert.assertTrue(
                    Pattern.compile(BotResponsePatterns.Authorization.VOICE_NOT_RECOGNIZED)
                            .matcher(botResponse).find(),
                    "[TransferAuth] Expected an authorization-rejected response for a transfer-money "
                    + "query spoken in a voice different from the registered one.\n  Pattern : "
                    + BotResponsePatterns.Authorization.VOICE_NOT_RECOGNIZED + "\n  Got     : " + botResponse);
        } finally {
            removeVoiceRegistration(homePage);
        }
    }

    @Test(groups = {"ui", "regression", "negative", "multilingual", "security"},
            description = "Escalates the voice-auth bypass UI13's testMarathiQueryOutcomeObserved "
                    + "found for balance queries: does a Marathi transfer-money query against an "
                    + "English-registered voice also bypass authorization, not just leak a "
                    + "read-only balance? Asserts the correct security behavior (rejected) rather "
                    + "than the observed one, so this fails loudly if the bypass reaches transfer "
                    + "initiation too, instead of silently documenting it as accepted.")
    public void testMarathiTransferInitiationBypassEscalation() throws Exception {
        HomePage homePage = registerVoiceAndReachHome();

        try {
            String botResponse = askWithVoice(
                    homePage, MultilingualVoiceQueries.Marathi.TRANSFER_MONEY, EdgeTtsEngine.VOICE_MARATHI);

            Assert.assertTrue(
                    Pattern.compile(BotResponsePatterns.Authorization.VOICE_NOT_RECOGNIZED)
                            .matcher(botResponse).find(),
                    "[TransferAuth] A Marathi transfer-money query against an English-registered "
                    + "voice should be rejected by voice-auth, same as any other unregistered "
                    + "voice/language — if this fails, the balance-leak bypass UI13 found also "
                    + "reaches transfer-money initiation.\n  Pattern : "
                    + BotResponsePatterns.Authorization.VOICE_NOT_RECOGNIZED + "\n  Got     : " + botResponse);
        } finally {
            removeVoiceRegistration(homePage);
        }
    }

    private void removeVoiceRegistration(HomePage homePage) {
        try {
            homePage.removeRegisteredVoice();
        } catch (Exception e) {
            System.out.println("[TransferAuth] WARN — cleanup (remove voice) failed: " + e.getMessage());
        }
    }
}
