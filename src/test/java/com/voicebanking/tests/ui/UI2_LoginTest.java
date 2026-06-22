package com.voicebanking.tests.ui;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.microsoft.playwright.options.LoadState;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.pages.BasePage;
import com.voicebanking.pages.WelcomePage;

public class UI2_LoginTest extends BasePage {

    private WelcomePage openWelcomePage() {
        WelcomePage welcomePage = new WelcomePage(page, Endpoints.getUiBaseUrl());
        welcomePage.navigate();
        welcomePage.dismissPwaPopupIfPresent();
        return welcomePage;
    }

    @Test(groups = {"ui", "regression"},
            description = "Should accept a 10-digit phone number in the mobile input")
    public void testPhoneNumberInput() {
        WelcomePage welcomePage = openWelcomePage();
        String phone = WelcomePage.generateRandomPhone();

        welcomePage.enterPhoneNumber(phone);

        Assert.assertEquals(
                welcomePage.getPhoneInputValue(),
                phone,
                "Phone input should contain the entered number");
    }

    @Test(groups = {"ui", "regression"},
            description = "Send OTP button should be enabled after entering a phone number")
    public void testSendOtpButtonEnabled() {
        WelcomePage welcomePage = openWelcomePage();

        welcomePage.enterPhoneNumber(WelcomePage.generateRandomPhone());

        Assert.assertTrue(
                welcomePage.isSendOtpButtonEnabled(),
                "Send OTP button should be enabled when phone number is entered");
    }

    @Test(groups = {"ui", "regression"},
            description = "Should show validation error when phone number is blank")
    public void testBlankPhoneShowsValidationError() {
        WelcomePage welcomePage = openWelcomePage();

        // Leave phone blank and click Send OTP to trigger validation
        welcomePage.clickSendOtp();

        Assert.assertTrue(
                welcomePage.isPhoneErrorVisible(),
                "Validation error should appear when phone number is blank");
        Assert.assertEquals(
                welcomePage.getPhoneErrorText(),
                "Please enter a valid 10-digit mobile number",
                "Correct validation message should be shown for blank phone");
    }

    @Test(groups = {"ui", "regression"},
            description = "Should show validation error when phone number has less than 10 digits")
    public void testShortPhoneShowsValidationError() {
        WelcomePage welcomePage = openWelcomePage();

        // Enter only 8 digits (less than required 10)
        welcomePage.enterPhoneNumber("98765432");
        welcomePage.clickSendOtp();

        Assert.assertTrue(
                welcomePage.isPhoneErrorVisible(),
                "Validation error should appear when phone number has fewer than 10 digits");
        Assert.assertEquals(
                welcomePage.getPhoneErrorText(),
                "Please enter a valid 10-digit mobile number",
                "Correct validation message should be shown for short phone number");
    }

    @Test(groups = {"ui", "regression"},
            description = "Should display Terms & Conditions link on the login/welcome page")
    public void testLoginPageTermsAndConditions() {
        WelcomePage welcomePage = openWelcomePage();

        Assert.assertTrue(welcomePage.isTermsLinkVisible(),
                "Terms & Conditions link should be visible on the login page");
        Assert.assertEquals(welcomePage.getTermsLinkHref(), "/terms",
                "Terms & Conditions link should point to /terms");
    }

    @Test(groups = {"ui", "regression"},
            description = "Should navigate away from welcome page after clicking Send OTP")
    public void testSendOtpClickNavigation() {
        WelcomePage welcomePage = openWelcomePage();
        String phone = WelcomePage.generateRandomPhone();

        welcomePage.enterPhoneNumber(phone);
        welcomePage.clickSendOtp();

        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        String currentUrl = page.url();
        Assert.assertFalse(
                currentUrl.endsWith("/welcome"),
                "Should navigate away from /welcome after Send OTP. URL: " + currentUrl);
    }
}
