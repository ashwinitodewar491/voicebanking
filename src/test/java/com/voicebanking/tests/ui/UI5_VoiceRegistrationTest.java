package com.voicebanking.tests.ui;

import org.testng.Assert;
import org.testng.annotations.Test;

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
        otpPage.enterOtp(OtpPage.generateRandomOtp());
        otpPage.clickContinue();

        // Language page only appears on first login; skip interaction if already set
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        LanguagePage languagePage = new LanguagePage(page);
        if (languagePage.isPageVisible()) {
            languagePage.selectEnglish();
            languagePage.clickContinue();
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
