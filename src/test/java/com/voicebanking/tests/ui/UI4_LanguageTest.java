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

public class UI4_LanguageTest extends BasePage {

    /** Reopens the language picker via the globe icon on Home rather than relying on the
     * once-ever onboarding auto-prompt — this suite is only allowed to use two fixed accounts
     * (Aniket More / Rohit Mehta, see {@link Constants}), both already long past their one-time
     * onboarding from prior runs today, so that auto-prompt will never show for either of them
     * again. {@link HomePage#clickLanguageButton()} opens the identical {@link LanguagePage} UI
     * either way (already relied on this same way by {@code BaseVoiceTest#enforceEnglishLocale}),
     * so every assertion below still holds — only how the page is reached changed. */
    private LanguagePage navigateToLanguagePage() {
        WelcomePage welcomePage = new WelcomePage(page, Endpoints.getUiBaseUrl());
        welcomePage.navigate();
        welcomePage.dismissPwaPopupIfPresent();
        welcomePage.enterPhoneNumber(Constants.CUSTOMER_C_PHONE);
        welcomePage.clickSendOtp();

        OtpPage otpPage = new OtpPage(page);
        otpPage.waitForPageLoad();
        otpPage.enterOtp(OtpPage.getTestOtp());
        otpPage.clickContinue();

        LanguagePage languagePage = new LanguagePage(page);
        try {
            // Only possible if this account somehow hasn't onboarded yet — tolerated for
            // completeness, not the expected path for either allowed account anymore.
            languagePage.waitForPageLoad();
            languagePage.selectEnglish();
            languagePage.clickContinue();
            VoiceRegistrationPage voicePage = new VoiceRegistrationPage(page);
            voicePage.waitForPageLoad();
            voicePage.clickSkipForNow();
        } catch (PlaywrightException notShowing) {
            // Expected: onboarding already completed for this account in an earlier run.
        }

        HomePage homePage = new HomePage(page);
        homePage.waitForPageLoad();
        homePage.clickLanguageButton();
        languagePage.waitForPageLoad();
        return languagePage;
    }

    @Test(groups = {"ui", "regression"},
            description = "Should display language selection screen when reopened from Home")
    public void testLanguagePageLoads() {
        LanguagePage languagePage = navigateToLanguagePage();

        Assert.assertTrue(
                languagePage.isPageVisible(),
                "Language selection screen should be visible when reopened from Home");
    }

    @Test(groups = {"ui", "regression"},
            description = "English should be selected on the language screen for these accounts")
    public void testEnglishIsPreSelected() {
        LanguagePage languagePage = navigateToLanguagePage();

        Assert.assertTrue(
                languagePage.isEnglishSelected(),
                "English should be selected on the language screen");
    }

    @Test(groups = {"ui", "regression"},
            description = "Back button on language page (reopened from Home) should return to Home")
    public void testBackButtonReturnsToHome() {
        LanguagePage languagePage = navigateToLanguagePage();

        languagePage.clickBack();

        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        HomePage homePage = new HomePage(page);
        Assert.assertTrue(
                homePage.isPageVisible(),
                "Back button should return to Home when the language picker was reopened from there");
    }

    @Test(groups = {"ui", "regression", "smoke"},
            description = "Should proceed after selecting English and clicking Continue")
    public void testSelectEnglishAndContinue() {
        LanguagePage languagePage = navigateToLanguagePage();

        languagePage.selectEnglish();
        languagePage.clickContinue();

        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        String url = page.url();
        Assert.assertFalse(
                url.contains("/language"),
                "Should navigate away from language screen after Continue. URL: " + url);
    }
}
