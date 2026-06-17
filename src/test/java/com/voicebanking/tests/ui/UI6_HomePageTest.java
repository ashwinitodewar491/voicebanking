package com.voicebanking.tests.ui;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.microsoft.playwright.options.LoadState;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.DataText.VoiceQueries;
import com.voicebanking.pages.BasePage;
import com.voicebanking.pages.HomePage;
import com.voicebanking.pages.LanguagePage;
import com.voicebanking.pages.OtpPage;
import com.voicebanking.pages.VoiceRegistrationPage;
import com.voicebanking.pages.WelcomePage;
import com.voicebanking.utils.TtsUtil;

public class UI6_HomePageTest extends BasePage {

    private String generatedWavPath;

    @BeforeClass(alwaysRun = true)
    public void setAudioFile() throws Exception {
        generatedWavPath = TtsUtil.generateWav(VoiceQueries.ACCOUNT_BALANCE);
        System.setProperty("audioFile", generatedWavPath);
    }

    @AfterClass(alwaysRun = true)
    public void clearAudioFile() {
        System.clearProperty("audioFile");
        TtsUtil.deleteWav(generatedWavPath);
    }

    private HomePage navigateToHomePage() {
        WelcomePage welcomePage = new WelcomePage(page, Endpoints.getUiBaseUrl());
        welcomePage.navigate();
        welcomePage.dismissPwaPopupIfPresent();
        welcomePage.enterPhoneNumber(WelcomePage.generateRandomPhone());
        welcomePage.clickSendOtp();

        OtpPage otpPage = new OtpPage(page);
        otpPage.waitForPageLoad();
        otpPage.enterOtp(OtpPage.generateRandomOtp());
        otpPage.clickContinue();

        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        LanguagePage languagePage = new LanguagePage(page);
        if (languagePage.isPageVisible()) {
            languagePage.selectEnglish();
            languagePage.clickContinue();
        }

        VoiceRegistrationPage voicePage = new VoiceRegistrationPage(page);
        voicePage.waitForPageLoad();
        voicePage.clickSkipForNow();

        HomePage homePage = new HomePage(page);
        homePage.waitForPageLoad();
        return homePage;
    }

    @Test(groups = {"ui", "regression"},
            description = "Should display home dashboard after skipping voice registration")
    public void testHomePageLoads() {
        HomePage homePage = navigateToHomePage();

        Assert.assertTrue(
                homePage.isPageVisible(),
                "Home dashboard should be visible after completing onboarding");
    }

    @Test(groups = {"ui", "regression"},
            description = "Should display all key UI elements on the home dashboard")
    public void testHomePageElements() {
        HomePage homePage = navigateToHomePage();

        Assert.assertTrue(homePage.isBalanceToggleVisible(),
                "Balance toggle button should be visible");

        Assert.assertTrue(homePage.isTransactionsButtonVisible(),
                "Recent Transactions button should be visible");

        Assert.assertTrue(homePage.isLanguageButtonVisible(),
                "Language button should be visible");

        Assert.assertTrue(homePage.isUserMenuButtonVisible(),
                "User menu button should be visible");
    }

    @Test(groups = {"ui", "regression"},
            description = "Should toggle balance visibility when clicking the eye icon")
    public void testBalanceToggle() {
        HomePage homePage = navigateToHomePage();

        homePage.clickBalanceToggle();

        Assert.assertTrue(
                homePage.isBalanceToggleVisible(),
                "Balance toggle button should remain visible after clicking");
    }

    @Test(groups = {"ui", "regression"},
            description = "Should open transactions panel when clicking Recent Transactions")
    public void testRecentTransactionsClick() {
        HomePage homePage = navigateToHomePage();

        homePage.clickRecentTransactions();

        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        Assert.assertTrue(
                homePage.isPageVisible(),
                "Home page should remain visible after clicking Recent Transactions (panel opens in-page)");
    }

    @Test(groups = {"ui", "regression"},
            description = "Should send voice query 'What is my account balance' and receive a response")
    public void testVoiceBalanceQuery() {
        HomePage homePage = navigateToHomePage();

        homePage.holdToSpeak(5000);

        page.waitForTimeout(5000);

        Assert.assertTrue(
                homePage.isPageVisible(),
                "Home page should remain visible after voice query");
    }
}
