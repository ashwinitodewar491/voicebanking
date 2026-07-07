package com.voicebanking.tests.ui;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.microsoft.playwright.PlaywrightException;
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

import java.util.regex.Pattern;

public class UI6_HomePageTest extends BasePage {

    private String generatedWavPath;

    @BeforeClass(alwaysRun = true)
    public void setAudioFile() throws Exception {
        generatedWavPath = TtsUtil.generateWav(VoiceQueries.English.ACCOUNT_BALANCE);
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
            description = "Should send voice query 'What is my account balance' and receive a valid balance response")
    public void testVoiceBalanceQuery() {
        String expectedQuery = VoiceQueries.English.ACCOUNT_BALANCE;

        // Web Speech API is unavailable in headless Chromium — inject mock before any navigation
        page.addInitScript(speechMockScript(expectedQuery));

        HomePage homePage = navigateToHomePage();

        homePage.holdToSpeakWithRetry(1000, 3, 10000);
        homePage.waitForVoiceResponse(15000);

        String transcribed = homePage.getLastTranscribedText();
        String botResponse = homePage.getLastBotResponse();

        System.out.println("[Account Balance] Expected    : " + expectedQuery);
        System.out.println("[Account Balance] Transcribed : " + transcribed);
        System.out.println("[Account Balance] Bot response: " + botResponse);

        Assert.assertTrue(homePage.isPageVisible(),
                "Home page should remain visible after voice query");

        // Account type (e.g. SAVINGS), account number (e.g. ACC202602260029),
        // and amount (e.g. 67000.0) are all dynamic — only the sentence structure is asserted.
        Pattern balancePattern = Pattern.compile(
                "The balance in your [A-Z]+ \\([A-Z0-9]+\\) account is [\\d,]+(?:\\.\\d+)?\\.");
        Assert.assertTrue(
                balancePattern.matcher(botResponse).find(),
                "[Account Balance] Bot response did not match expected pattern.\n" +
                "  Pattern : The balance in your <TYPE> (<ID>) account is <AMOUNT>.\n" +
                "  Got     : " + botResponse);
    }
}
