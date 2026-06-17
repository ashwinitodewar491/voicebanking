package com.voicebanking.tests.ui;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.microsoft.playwright.options.LoadState;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.pages.BasePage;
import com.voicebanking.pages.OtpPage;
import com.voicebanking.pages.WelcomePage;

public class UI3_OtpTest extends BasePage {

    private OtpPage navigateToOtpPage() {
        WelcomePage welcomePage = new WelcomePage(page, Endpoints.getUiBaseUrl());
        welcomePage.navigate();
        welcomePage.dismissPwaPopupIfPresent();
        welcomePage.enterPhoneNumber(WelcomePage.generateRandomPhone());
        welcomePage.clickSendOtp();

        OtpPage otpPage = new OtpPage(page);
        otpPage.waitForPageLoad();
        return otpPage;
    }

    @Test(groups = {"ui", "regression"},
            description = "Should display OTP input screen after sending OTP")
    public void testOtpPageLoads() {
        OtpPage otpPage = navigateToOtpPage();

        Assert.assertTrue(
                otpPage.isOtpPageVisible(),
                "OTP input screen should be visible after clicking Send OTP");
    }

    @Test(groups = {"ui", "regression"},
            description = "Should accept a 4-digit OTP across the four input boxes")
    public void testOtpInputAcceptsDigits() {
        OtpPage otpPage = navigateToOtpPage();
        String otp = OtpPage.generateRandomOtp();

        otpPage.enterOtp(otp);

        Assert.assertTrue(
                otpPage.isContinueButtonEnabled(),
                "Continue button should be enabled after entering OTP");
    }

    @Test(groups = {"ui", "regression"},
            description = "Should proceed after clicking Continue with entered OTP")
    public void testContinueAfterOtpEntry() {
        OtpPage otpPage = navigateToOtpPage();

        otpPage.enterOtp(OtpPage.generateRandomOtp());
        otpPage.clickContinue();

        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        String url = page.url();
        Assert.assertFalse(
                url.contains("/otp"),
                "Should navigate away from OTP screen after clicking Continue. URL: " + url);
    }

    @Test(groups = {"ui", "regression"},
            description = "Back button on OTP page should return to welcome screen")
    public void testBackButtonReturnsToWelcome() {
        OtpPage otpPage = navigateToOtpPage();

        otpPage.clickBack();

        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        Assert.assertTrue(
                page.url().contains("/welcome"),
                "Back button should return to /welcome page");
    }
}
