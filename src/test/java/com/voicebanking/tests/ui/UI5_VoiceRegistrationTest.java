package com.voicebanking.tests.ui;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.LoadState;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.pages.BasePage;
import com.voicebanking.pages.LanguagePage;
import com.voicebanking.pages.OtpPage;
import com.voicebanking.pages.VoiceRegistrationPage;
import com.voicebanking.pages.WelcomePage;

public class UI5_VoiceRegistrationTest extends BasePage {

    private VoiceRegistrationPage navigateToVoiceRegistrationPage() {
        WelcomePage welcomePage = new WelcomePage(page, Endpoints.getUiBaseUrl());
        welcomePage.navigate();
        welcomePage.dismissPwaPopupIfPresent();
        welcomePage.enterPhoneNumber(WelcomePage.generateRandomPhone());
        welcomePage.clickSendOtp();

        OtpPage otpPage = new OtpPage(page);
        otpPage.waitForPageLoad();
        otpPage.enterOtp(OtpPage.getTestOtp());
        otpPage.clickContinue();

        // Language page only appears on first login; wait up to 10s, skip if absent
        LanguagePage languagePage = new LanguagePage(page);
        try {
            languagePage.waitForPageLoad();
            languagePage.selectEnglish();
            languagePage.clickContinue();
        } catch (PlaywrightException ignored) {
            // language page not present (returning user), continue
        }

        VoiceRegistrationPage voicePage = new VoiceRegistrationPage(page);
        voicePage.waitForPageLoad();
        return voicePage;
    }

    @Test(groups = {"ui", "regression"},
            description = "Should display voice registration screen after language selection")
    public void testVoiceRegistrationPageLoads() {
        VoiceRegistrationPage voicePage = navigateToVoiceRegistrationPage();

        Assert.assertTrue(
                voicePage.isPageVisible(),
                "Voice registration screen should be visible after language selection");
    }

    @Test(groups = {"ui", "regression"},
            description = "Start Registration button should be disabled until consent is checked")
    public void testStartButtonDisabledWithoutConsent() {
        VoiceRegistrationPage voicePage = navigateToVoiceRegistrationPage();

        Assert.assertTrue(
                voicePage.isStartButtonDisabled(),
                "Start Registration button should be disabled when consent is not checked");
    }

    @Test(groups = {"ui", "regression"},
            description = "Start Registration button should be enabled after checking consent")
    public void testStartButtonEnabledAfterConsent() {
        VoiceRegistrationPage voicePage = navigateToVoiceRegistrationPage();

        voicePage.checkConsent();

        Assert.assertTrue(
                voicePage.isConsentChecked(),
                "Consent checkbox should be checked");

        Assert.assertFalse(
                voicePage.isStartButtonDisabled(),
                "Start Registration button should be enabled after consent is checked");
    }

    @Test(groups = {"ui", "regression"},
            description = "Start Registration button should be disabled again after unchecking consent")
    public void testStartButtonDisabledAfterUncheckingConsent() {
        VoiceRegistrationPage voicePage = navigateToVoiceRegistrationPage();

        voicePage.checkConsent();
        Assert.assertFalse(
                voicePage.isStartButtonDisabled(),
                "Start Registration button should be enabled after checking consent");

        voicePage.uncheckConsent();
        Assert.assertTrue(
                voicePage.isStartButtonDisabled(),
                "Start Registration button should be disabled again after unchecking consent");
    }

    @Test(groups = {"ui", "regression"},
            description = "Should navigate away when clicking Skip for Now")
    public void testSkipForNow() {
        VoiceRegistrationPage voicePage = navigateToVoiceRegistrationPage();

        voicePage.clickSkipForNow();

        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        Assert.assertFalse(
                voicePage.isPageVisible(),
                "Voice registration screen should be dismissed after clicking Skip for Now");
    }
}
