package com.voicebanking.tests.ui;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.microsoft.playwright.options.LoadState;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.pages.BasePage;
import com.voicebanking.pages.LanguagePage;
import com.voicebanking.pages.OtpPage;
import com.voicebanking.pages.WelcomePage;

public class UI4_LanguageTest extends BasePage {

    private LanguagePage navigateToLanguagePage() {
        WelcomePage welcomePage = new WelcomePage(page, Endpoints.getUiBaseUrl());
        welcomePage.navigate();
        welcomePage.dismissPwaPopupIfPresent();
        welcomePage.enterPhoneNumber(WelcomePage.generateRandomPhone());
        welcomePage.clickSendOtp();

        OtpPage otpPage = new OtpPage(page);
        otpPage.waitForPageLoad();
        otpPage.enterOtp(OtpPage.generateRandomOtp());
        otpPage.clickContinue();

        LanguagePage languagePage = new LanguagePage(page);
        languagePage.waitForPageLoad();
        return languagePage;
    }

    @Test(groups = {"ui", "regression"},
            description = "Should display language selection screen after OTP verification")
    public void testLanguagePageLoads() {
        LanguagePage languagePage = navigateToLanguagePage();

        Assert.assertTrue(
                languagePage.isPageVisible(),
                "Language selection screen should be visible after OTP step");
    }

    @Test(groups = {"ui", "regression"},
            description = "English should be pre-selected on the language screen")
    public void testEnglishIsPreSelected() {
        LanguagePage languagePage = navigateToLanguagePage();

        Assert.assertTrue(
                languagePage.isEnglishSelected(),
                "English should be selected by default on the language screen");
    }

    @Test(groups = {"ui", "regression"},
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
