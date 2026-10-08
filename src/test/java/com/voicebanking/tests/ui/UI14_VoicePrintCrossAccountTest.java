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
 * Guards against a specific, more severe variant of the voice-mismatch bug space than {@link
 * UI11_VoiceRegistrationAuthTest} covers: that class only ever proves "the wrong voice is
 * rejected" using a voice that has never been enrolled by anyone. It never rules out a matcher
 * that's actually checking "does this audio match ANY account's registered voiceprint" instead of
 * "does this audio match THIS logged-in account's own voiceprint" — a bug that a single-account
 * test literally cannot expose, since there's no second enrolled voiceprint around to leak
 * through.
 *
 * <p>This class enrolls two different real accounts with two different, genuinely distinct
 * speakers (Leena Kamat with {@link EdgeTtsEngine#VOICE}, Aniket More with {@link
 * EdgeTtsEngine#VOICE_EN_ALTERNATE}), then logs in as Aniket and asks a balance query spoken in
 * Leena's registered voice — not an arbitrary unregistered voice, but one that IS a valid
 * voiceprint on file, just for the other account. If the app is properly account-scoped, this is
 * rejected exactly like any other mismatched voice; if voice-matching is actually global, this
 * would incorrectly leak Aniket's balance to a query authenticated as Leena.
 *
 * <p>Uses Leena Kamat (Customer B, {@link Constants#CUSTOMER_B_PHONE}) and Aniket More (Customer
 * C, {@link Constants#CUSTOMER_C_PHONE}) — the same two accounts {@link UI11_VoiceRegistrationAuthTest}
 * and {@link UI5_VoiceRegistrationTest} already treat as the sanctioned pair for voice-registration
 * testing (clean, minimal-interference seed data; see their own javadocs and the Constants
 * comments on {@code CUSTOMER_B_*}/{@code CUSTOMER_C_*}). Both voiceprints are removed in a {@code
 * finally} block so neither account is left registered afterward — Aniket's account in particular
 * is also used unregistered by other suites (UI7/UI8/UI9 ground-truth cross-verification via
 * {@link com.voicebanking.tests.ui.base.BaseVoiceTest}, which always speaks queries in {@link
 * EdgeTtsEngine#VOICE}/Aria); a voiceprint left behind here under {@link
 * EdgeTtsEngine#VOICE_EN_ALTERNATE}/Guy would make every one of those suites' Aria-voiced queries
 * against his account start failing with a spurious "Not Authorised" the next time they run.
 */
public class UI14_VoicePrintCrossAccountTest extends BasePage {

    private String generatedWavPath;

    @BeforeClass(alwaysRun = true)
    public void setAudioFile() throws Exception {
        generatedWavPath = TtsUtil.generateWav(
                VoiceQueries.English.VOICE_ENROLLMENT_PHRASE, EdgeTtsEngine.VOICE);
    }

    /** Same browser/mic setup as {@link UI11_VoiceRegistrationAuthTest#setUpBrowser()} — see that
     * method's javadoc for why the extra Chromium args and the two init scripts (window.__micStreams
     * tracking, AnalyserNode stub) are needed for a genuine enrollment to succeed headlessly. */
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

    @AfterClass(alwaysRun = true)
    public void clearAudioFile() {
        TtsUtil.deleteWav(generatedWavPath);
    }

    /**
     * Logs into {@code phoneNumber} (clearing any prior session first, so this can enroll a
     * second account in the same browser/page right after the first) and completes the
     * voice-registration enrollment flow (every image/question step, via {@link VoiceEnrollment})
     * using {@code enrollmentVoice}, then clicks Start Banking
     * to land on Home. Generalizes {@link UI11_VoiceRegistrationAuthTest#registerVoiceAndReachHome()}
     * to an explicit phone/voice pair instead of a single hardcoded account, since this class needs
     * to enroll two distinct accounts with two distinct speakers in the same run.
     */
    private HomePage registerVoiceAndReachHome(String phoneNumber, String enrollmentVoice) throws Exception {
        String enrollWavPath = TtsUtil.generateWav(VoiceQueries.English.VOICE_ENROLLMENT_PHRASE, enrollmentVoice);
        Files.copy(Path.of(enrollWavPath), Path.of(generatedWavPath), StandardCopyOption.REPLACE_EXISTING);
        TtsUtil.deleteWav(enrollWavPath);

        // Clears any prior login in this browser context — a no-op for the very first account,
        // but required before the second: navigating to /welcome while still authenticated as the
        // first account skips straight past the phone screen instead of prompting again (same
        // reasoning as BaseVoiceTest#login's identical clear before a repeat login).
        context.clearCookies();
        try {
            page.evaluate("() => { try { localStorage.clear(); } catch (e) {} "
                    + "try { sessionStorage.clear(); } catch (e) {} }");
        } catch (PlaywrightException ignored) {
            // nothing to clear yet on the very first login of this browser
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
            languagePage.selectEnglish();
            languagePage.clickContinue();
        } catch (PlaywrightException ignored) {
            // language page absent for returning users
        }

        VoiceRegistrationPage voicePage = new VoiceRegistrationPage(page);
        try {
            voicePage.waitForPageLoad();
        } catch (PlaywrightException notAutoPrompted) {
            // The app does not reliably auto-prompt for registration on every login — see
            // UI11_VoiceRegistrationAuthTest#registerVoiceAndReachHome's javadoc for why "the
            // screen didn't show up" is never treated as proof of already being registered here.
            HomePage homePage = new HomePage(page);
            homePage.waitForPageLoad();

            if (homePage.isVoiceRegistered()) {
                return homePage;
            }

            System.out.println("[CrossAccount] Registration screen didn't auto-appear for "
                    + phoneNumber + " but the account isn't actually registered — forcing it via "
                    + "the user menu.");
            homePage.clickRegisterVoiceFromMenu();
            voicePage.waitForPageLoad();
        }

        voicePage.checkConsent();
        voicePage.clickStartRegistration();

        // Image steps then question steps, until Start Banking — see VoiceEnrollment.
        VoiceEnrollment.completeAllSteps(voicePage, generatedWavPath, enrollmentVoice, "CrossAccount");

        voicePage.clickStartBanking();

        HomePage homePage = new HomePage(page);
        homePage.waitForPageLoad();
        return homePage;
    }

    private static final Pattern GENERIC_GREETING = Pattern.compile("Welcome.*How can I help you today");
    private static final Pattern CONTEXT_LOST_FALLBACK =
            Pattern.compile("(?i)didn.t understand that.*what would you like to do");
    private static final int MAX_REASK_ATTEMPTS = 3;

    /** Same query/re-ask flow as {@link UI11_VoiceRegistrationAuthTest#askBalanceWithVoice} — see
     * that method's javadoc for why the re-ask loop is needed (a reconnect's session-start
     * greeting or context-lost fallback can swallow the query right after it's spoken). */
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
        System.out.println("[CrossAccount] Voice used   : " + voice);
        System.out.println("[CrossAccount] Transcribed  : " + transcribed);
        System.out.println("[CrossAccount] Bot response : " + botResponse);

        for (int reaskNum = 1;
             reaskNum <= MAX_REASK_ATTEMPTS
                     && (GENERIC_GREETING.matcher(botResponse).find()
                        || CONTEXT_LOST_FALLBACK.matcher(botResponse).find()
                        || botResponse.isBlank());
             reaskNum++) {
            if (botResponse.isBlank()) {
                NoResponseTracker.recordOccurrence("CrossAccount: " + voice);
            }
            System.out.println("[CrossAccount] WARN — got a generic greeting/fallback/empty response"
                    + " instead of an answer — re-asking (" + reaskNum + " of " + MAX_REASK_ATTEMPTS + ")...");
            homePage.reacquireMicrophoneForFollowUp();
            homePage.holdToSpeakWithRetry(holdMs, 3, 8000);
            homePage.waitForVoiceResponse(15000);
            transcribed = homePage.getLastTranscribedText();
            botResponse = homePage.getLastBotResponse();
            System.out.println("[CrossAccount] Re-ask " + reaskNum + " Transcribed : " + transcribed);
            System.out.println("[CrossAccount] Re-ask " + reaskNum + " Bot response: " + botResponse);
        }

        return botResponse;
    }

    @Test(groups = {"ui", "regression", "smoke", "security"},
            description = "A voiceprint registered on one account must not authorize a balance "
                    + "query on a different account, even though it is a genuinely enrolled "
                    + "voice (just for someone else) — not merely an unregistered stranger's voice")
    public void testVoiceRegisteredForOneAccountDoesNotAuthorizeAnother() throws Exception {
        HomePage leenaHome = registerVoiceAndReachHome(Constants.CUSTOMER_B_PHONE, EdgeTtsEngine.VOICE);
        try {
            HomePage aniketHome = registerVoiceAndReachHome(
                    Constants.CUSTOMER_C_PHONE, EdgeTtsEngine.VOICE_EN_ALTERNATE);
            try {
                // Cross-account probe: logged in as Aniket, speak in Leena's registered voice.
                String crossAccountResponse = askBalanceWithVoice(aniketHome, EdgeTtsEngine.VOICE);

                Assert.assertFalse(
                        Pattern.compile(BotResponsePatterns.Balance.ANY).matcher(crossAccountResponse).find(),
                        "[CrossAccount] Leena's registered voice must NOT unlock Aniket's account "
                        + "balance — voice-matching must be account-scoped, not global.\n  Got: "
                        + crossAccountResponse);
                Assert.assertTrue(
                        Pattern.compile(BotResponsePatterns.Authorization.VOICE_NOT_RECOGNIZED)
                                .matcher(crossAccountResponse).find(),
                        "[CrossAccount] Expected an authorization-rejected response when a "
                        + "different account's registered voice is used.\n  Pattern : "
                        + BotResponsePatterns.Authorization.VOICE_NOT_RECOGNIZED
                        + "\n  Got     : " + crossAccountResponse);

                // Sanity control: Aniket's own registered voice must still work on his own
                // account — confirms the rejection above is really about account-scoping and not
                // some unrelated breakage that rejects every query regardless of voice.
                String ownVoiceResponse = askBalanceWithVoice(aniketHome, EdgeTtsEngine.VOICE_EN_ALTERNATE);
                Assert.assertTrue(
                        Pattern.compile(BotResponsePatterns.Balance.ANY).matcher(ownVoiceResponse).find(),
                        "[CrossAccount] Aniket's own registered voice should still authorize his "
                        + "own account's balance query.\n  Pattern : " + BotResponsePatterns.Balance.ANY
                        + "\n  Got     : " + ownVoiceResponse);
            } finally {
                try {
                    aniketHome.removeRegisteredVoice();
                } catch (Exception e) {
                    System.out.println("[CrossAccount] WARN — cleanup (remove Aniket's voice) failed: "
                            + e.getMessage());
                }
            }
        } finally {
            try {
                // Leena's own page/session may have been replaced by the account switch above —
                // removeRegisteredVoice only needs the current `page`, which HomePage wraps live,
                // so re-navigating to Leena's account first would be required to remove HER
                // voiceprint specifically. Re-login as Leena for cleanup rather than risk leaving
                // her voiceprint behind (see class javadoc on why that matters for other suites).
                HomePage leenaCleanupHome = reloginForCleanup(Constants.CUSTOMER_B_PHONE);
                if (leenaCleanupHome.isVoiceRegistered()) {
                    leenaCleanupHome.removeRegisteredVoice();
                }
            } catch (Exception e) {
                System.out.println("[CrossAccount] WARN — cleanup (remove Leena's voice) failed: "
                        + e.getMessage());
            }
        }
    }

    /** Logs back into {@code phoneNumber} purely for cleanup — no enrollment, no fake-audio
     * concerns, just reaching Home so {@link HomePage#removeRegisteredVoice()} has a menu to
     * click. Separate from {@link #registerVoiceAndReachHome} since cleanup never needs to record
     * anything. */
    private HomePage reloginForCleanup(String phoneNumber) throws Exception {
        context.clearCookies();
        try {
            page.evaluate("() => { try { localStorage.clear(); } catch (e) {} "
                    + "try { sessionStorage.clear(); } catch (e) {} }");
        } catch (PlaywrightException ignored) {
            // nothing to clear
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
            languagePage.selectEnglish();
            languagePage.clickContinue();
        } catch (PlaywrightException ignored) {
            // language page absent for returning users
        }

        VoiceRegistrationPage voicePage = new VoiceRegistrationPage(page);
        try {
            voicePage.waitForPageLoad();
            voicePage.clickSkipForNow();
        } catch (PlaywrightException notShowing) {
            // Already effectively on Home.
        }

        HomePage homePage = new HomePage(page);
        homePage.waitForPageLoad();
        return homePage;
    }
}
