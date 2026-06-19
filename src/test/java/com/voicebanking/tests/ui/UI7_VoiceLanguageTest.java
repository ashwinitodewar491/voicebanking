package com.voicebanking.tests.ui;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.DataText.VoiceQueries;
import com.voicebanking.pages.HomePage;
import com.voicebanking.pages.LanguagePage;
import com.voicebanking.pages.OtpPage;
import com.voicebanking.pages.VoiceRegistrationPage;
import com.voicebanking.pages.WelcomePage;
import com.voicebanking.utils.TtsUtil;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

public class UI7_VoiceLanguageTest {

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;
    private String currentAudioPath;

    @DataProvider(name = "voiceQueries")
    public Object[][] voiceQueries() {
        return new Object[][]{
            { "Account Balance",     VoiceQueries.English.ACCOUNT_BALANCE,     new String[]{"balance", "account", "your"} },
            { "Recent Transactions", VoiceQueries.English.RECENT_TRANSACTIONS, new String[]{"transaction", "recent", "your", "history", "last"} },
            { "Transfer Money",      VoiceQueries.English.TRANSFER_MONEY,      new String[]{"choose", "options", "available", "please"} },
        };
    }

    @BeforeMethod(alwaysRun = true)
    public void setUpBrowserWithAudio(Object[] params) throws Exception {
        String query = (String) params[1];

        currentAudioPath = TtsUtil.generateWav(query);

        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));
        double slowMo = headless ? 0 : 800;

        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setSlowMo(slowMo)
                        .setArgs(List.of(
                                "--use-fake-device-for-media-stream",
                                "--use-file-for-fake-audio-capture=" + currentAudioPath)));
        context = browser.newContext(
                new Browser.NewContextOptions()
                        .setPermissions(List.of("microphone")));
        page = context.newPage();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (page != null)       page.close();
        if (context != null)    context.close();
        if (browser != null)    browser.close();
        if (playwright != null) playwright.close();
        TtsUtil.deleteWav(currentAudioPath);
    }

    @Test(dataProvider = "voiceQueries", groups = {"ui", "regression"},
            description = "Should process English voice query and verify bot response")
    public void testVoiceQuery(String queryName, String query, String[] expectedBotKeywords) {
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
            languagePage.selectByLocale(VoiceQueries.English.LOCALE);
            languagePage.clickContinue();
        }

        VoiceRegistrationPage voicePage = new VoiceRegistrationPage(page);
        voicePage.waitForPageLoad();
        voicePage.clickSkipForNow();

        HomePage homePage = new HomePage(page);
        homePage.waitForPageLoad();

        homePage.holdToSpeakWithRetry(5000, 3, 5000);
        homePage.waitForVoiceResponse(15000);

        String transcribed = homePage.getLastTranscribedText();
        String botResponse = homePage.getLastBotResponse();

        Assert.assertFalse(transcribed.isEmpty(),
                "[" + queryName + "] Voice not recognised — no user message appeared in chat");
        Assert.assertFalse(botResponse.isEmpty(),
                "[" + queryName + "] Bot did not respond after voice query");
        Assert.assertTrue(homePage.isPageVisible(),
                "[" + queryName + "] Home page should remain visible after voice query");
        Assert.assertTrue(containsAnyKeyword(transcribed, query.split("\\s+")),
                "[" + queryName + "] Transcription mismatch.\n  Expected (query): " + query
                + "\n  Actual (chat)   : " + transcribed);
        Assert.assertTrue(containsAnyKeyword(botResponse, expectedBotKeywords),
                "[" + queryName + "] Bot response not relevant.\n  Expected keywords: "
                + String.join(", ", expectedBotKeywords)
                + "\n  Actual response : " + botResponse);

        System.out.println("[" + queryName + "] Transcribed    : " + transcribed);
        System.out.println("[" + queryName + "] Bot response   : " + botResponse);
        System.out.println("[" + queryName + "] Query match    : PASS");
        System.out.println("[" + queryName + "] Response match : PASS");
    }

    private boolean containsAnyKeyword(String text, String[] keywords) {
        String lower = text.toLowerCase();
        for (String kw : keywords) {
            if (kw.length() > 1 && lower.contains(kw.toLowerCase())) return true;
        }
        return false;
    }
}
