package com.voicebanking.tests.ui;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.LoadState;
import com.voicebanking.DataText.Constants;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.pages.BasePage;
import com.voicebanking.pages.HomePage;
import com.voicebanking.pages.LanguagePage;
import com.voicebanking.pages.OtpPage;
import com.voicebanking.pages.VoiceRegistrationPage;
import com.voicebanking.pages.WelcomePage;

public class UI5_VoiceRegistrationTest extends BasePage {

    /** Reaches the voice-registration screen via the user menu's "Register your voice" option
     * rather than relying on the once-ever onboarding auto-prompt — this suite is only allowed to
     * use two fixed accounts (Aniket More / Rohit Mehta, see {@link Constants}), both already
     * long past their one-time onboarding from prior runs today, so that auto-prompt will never
     * show for either of them again. {@link HomePage#clickRegisterVoiceFromMenu()} opens the
     * identical consent-checkbox/Start-Registration/Skip-for-Now UI either way (confirmed live —
     * this is the same screen {@link com.voicebanking.tests.ui.UI11_VoiceRegistrationAuthTest} and
     * {@link com.voicebanking.tests.ui.UI13_VoiceRegistrationNaturalVariationTest} force it open
     * through for the same reason), so every assertion below still holds — only how the page is
     * reached changed. */
    private VoiceRegistrationPage navigateToVoiceRegistrationPage() {
        WelcomePage welcomePage = new WelcomePage(page, Endpoints.getUiBaseUrl());
        welcomePage.navigate();
        welcomePage.dismissPwaPopupIfPresent();
        welcomePage.enterPhoneNumber(Constants.CUSTOMER_C_PHONE);
        welcomePage.clickSendOtp();

        OtpPage otpPage = new OtpPage(page);
        otpPage.waitForPageLoad();
        otpPage.enterOtp(OtpPage.getTestOtp());
        otpPage.clickContinue();

        VoiceRegistrationPage voicePage = new VoiceRegistrationPage(page);
        try {
            // Only possible if this account somehow hasn't onboarded yet — tolerated for
            // completeness, not the expected path for either allowed account anymore.
            LanguagePage languagePage = new LanguagePage(page);
            languagePage.waitForPageLoad();
            languagePage.selectEnglish();
            languagePage.clickContinue();
            voicePage.waitForPageLoad();
            voicePage.clickSkipForNow();
        } catch (PlaywrightException notShowing) {
            // Expected: onboarding already completed for this account in an earlier run.
        }

        HomePage homePage = new HomePage(page);
        homePage.waitForPageLoad();
        homePage.clickRegisterVoiceFromMenu();
        voicePage.waitForPageLoad();
        return voicePage;
    }

    @Test(groups = {"ui", "regression", "smoke"},
            description = "Should display voice registration screen when opened via the user menu")
    public void testVoiceRegistrationPageLoads() {
        VoiceRegistrationPage voicePage = navigateToVoiceRegistrationPage();

        Assert.assertTrue(
                voicePage.isPageVisible(),
                "Voice registration screen should be visible when opened via the user menu");
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
                "Start Registration button should be enabled after checking consent");
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
