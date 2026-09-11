package com.voicebanking.tests.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.testng.annotations.Test;

import com.voicebanking.DataText.Constants;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.pages.BasePage;
import com.voicebanking.pages.OtpPage;
import com.voicebanking.pages.WelcomePage;

public class UI3_OtpTest extends BasePage {

    private OtpPage navigateToOtpPage() {
        WelcomePage welcomePage = new WelcomePage(page, Endpoints.getUiBaseUrl());
        welcomePage.navigate();
        welcomePage.dismissPwaPopupIfPresent();
        welcomePage.enterPhoneNumber(Constants.CUSTOMER_C_PHONE);
        welcomePage.clickSendOtp();

        OtpPage otpPage = new OtpPage(page);
        otpPage.waitForPageLoad();
        return otpPage;
    }

    @Test(groups = {"ui", "regression"},
            description = "Should display OTP input screen after sending OTP")
    public void testOtpPageLoads() {
        OtpPage otpPage = navigateToOtpPage();

        assertThat(otpPage.otpPage()).isVisible();
    }

    @Test(groups = {"ui", "regression"},
            description = "Should accept a 4-digit OTP across the four input boxes")
    public void testOtpInputAcceptsDigits() {
        OtpPage otpPage = navigateToOtpPage();
        String otp = OtpPage.generateRandomOtp();

        otpPage.enterOtp(otp);

        assertThat(otpPage.continueButton()).isEnabled();
    }

    @Test(groups = {"ui", "regression", "smoke"},
            description = "Should proceed after clicking Continue with entered OTP")
    public void testContinueAfterOtpEntry() {
        OtpPage otpPage = navigateToOtpPage();

        otpPage.enterOtp(OtpPage.generateRandomOtp());
        otpPage.clickContinue();

        // Polls (via a plain predicate, no regex needed) until navigation actually moves past the
        // OTP screen; a timeout here throws and fails the test, same as a failed assertion.
        page.waitForURL(url -> !url.contains("/otp"));
    }

    @Test(groups = {"ui", "regression"},
            description = "Back button on OTP page should return to welcome screen")
    public void testBackButtonReturnsToWelcome() {
        OtpPage otpPage = navigateToOtpPage();

        otpPage.clickBack();

        page.waitForURL(url -> url.contains("/welcome"));
    }

    @Test(groups = {"ui", "regression"},
            description = "Should display correct heading, subheading and label on OTP page")
    public void testOtpPageContent() {
        OtpPage otpPage = navigateToOtpPage();

        assertThat(otpPage.heading()).isVisible();
        assertThat(otpPage.subheading()).isVisible();
        assertThat(otpPage.otpLabel()).isVisible();
    }

    @Test(groups = {"ui", "regression"},
            description = "Should show validation error when OTP is blank on Continue")
    public void testBlankOtpShowsValidationError() {
        OtpPage otpPage = navigateToOtpPage();

        otpPage.clickContinue();

        assertThat(otpPage.otpError()).isVisible();
        assertThat(otpPage.otpError()).hasText("Please enter all 4 digits of your OTP");
    }

    @Test(groups = {"ui", "regression"},
            description = "Should show validation error when only 3 OTP digits are entered")
    public void testShortOtpShowsValidationError() {
        OtpPage otpPage = navigateToOtpPage();

        otpPage.enterPartialOtp("123");
        otpPage.clickContinue();

        assertThat(otpPage.otpError()).isVisible();
        assertThat(otpPage.otpError()).hasText("Please enter all 4 digits of your OTP");
    }

    @Test(groups = {"ui", "regression"},
            description = "Should display Terms & Conditions link on OTP page")
    public void testOtpPageTermsAndConditions() {
        OtpPage otpPage = navigateToOtpPage();

        assertThat(otpPage.termsLink()).isVisible();
        assertThat(otpPage.termsLink()).hasAttribute("href", "/terms");
    }
}
