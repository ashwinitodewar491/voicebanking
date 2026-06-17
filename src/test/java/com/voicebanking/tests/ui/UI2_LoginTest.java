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
