package com.voicebanking.tests.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.testng.annotations.Test;

import com.voicebanking.DataText.Constants;
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
        String phone = Constants.CUSTOMER_C_PHONE;

        welcomePage.enterPhoneNumber(phone);

        assertThat(welcomePage.phoneInput()).hasValue(phone);
    }

    @Test(groups = {"ui", "regression"},
            description = "Send OTP button should be enabled after entering a phone number")
    public void testSendOtpButtonEnabled() {
        WelcomePage welcomePage = openWelcomePage();

        welcomePage.enterPhoneNumber(Constants.CUSTOMER_C_PHONE);

        assertThat(welcomePage.sendOtpButton()).isEnabled();
    }

    @Test(groups = {"ui", "regression"},
            description = "Should show validation error when phone number is blank")
    public void testBlankPhoneShowsValidationError() {
        WelcomePage welcomePage = openWelcomePage();

        // Leave phone blank and click Send OTP to trigger validation
        welcomePage.clickSendOtp();

        assertThat(welcomePage.phoneError()).isVisible();
        assertThat(welcomePage.phoneError()).hasText("Please enter a valid 10-digit mobile number");
    }

    @Test(groups = {"ui", "regression"},
            description = "Should show validation error when phone number has less than 10 digits")
    public void testShortPhoneShowsValidationError() {
        WelcomePage welcomePage = openWelcomePage();

        // Enter only 8 digits (less than required 10)
        welcomePage.enterPhoneNumber("98765432");
        welcomePage.clickSendOtp();

        assertThat(welcomePage.phoneError()).isVisible();
        assertThat(welcomePage.phoneError()).hasText("Please enter a valid 10-digit mobile number");
    }

    @Test(groups = {"ui", "regression"},
            description = "Should display Terms & Conditions link on the login/welcome page")
    public void testLoginPageTermsAndConditions() {
        WelcomePage welcomePage = openWelcomePage();

        assertThat(welcomePage.termsLink()).isVisible();
        assertThat(welcomePage.termsLink()).hasAttribute("href", "/terms");
    }

    @Test(groups = {"ui", "regression", "smoke"},
            description = "Should navigate away from welcome page after clicking Send OTP")
    public void testSendOtpClickNavigation() {
        WelcomePage welcomePage = openWelcomePage();
        String phone = Constants.CUSTOMER_C_PHONE;

        welcomePage.enterPhoneNumber(phone);
        welcomePage.clickSendOtp();

        // Polls (default timeout) until the URL genuinely moves off /welcome, rather than reading
        // page.url() once right after navigation starts — a single-shot read here was the actual
        // cause of a flaky failure seen in a real dev-env run (navigation hadn't committed yet at
        // the instant checked). A plain lambda predicate avoids needing a regex for a simple
        // suffix check. A timeout here throws, which fails the test the same as a failed assertion.
        page.waitForURL(url -> !url.endsWith("/welcome"));
    }
}
