package com.voicebanking.pages;

import java.util.concurrent.ThreadLocalRandom;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

public class OtpPage {

    private final Page page;

    private static final String BACK_BTN       = "[data-testid='otp-back-btn']";
    private static final String OTP_INPUT_1    = "[data-testid='otp-digit-input-1']";
    private static final String OTP_INPUT_2    = "[data-testid='otp-digit-input-2']";
    private static final String OTP_INPUT_3    = "[data-testid='otp-digit-input-3']";
    private static final String OTP_INPUT_4    = "[data-testid='otp-digit-input-4']";
    private static final String CONTINUE_BTN   = "[data-testid='otp-verify-btn']";

    private static final String HEADING        = "text=Verify OTP";
    private static final String SUBHEADING     = "text=Enter the 4-digit code sent to your mobile";
    private static final String OTP_LABEL      = "text=Enter OTP";
    private static final String OTP_ERROR_MSG  = "p.text-red-300";
    private static final String TERMS_LINK     = "[data-testid='welcome-terms-link']";

    public OtpPage(Page page) {
        this.page = page;
    }

    public void waitForPageLoad() {
        page.locator(OTP_INPUT_1).waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(10000));
    }

    public boolean isOtpPageVisible()    { return page.locator(OTP_INPUT_1).isVisible(); }
    public boolean isHeadingVisible()    { return page.locator(HEADING).isVisible(); }
    public boolean isSubheadingVisible() { return page.locator(SUBHEADING).isVisible(); }
    public boolean isOtpLabelVisible()   { return page.locator(OTP_LABEL).isVisible(); }
    public boolean isOtpErrorVisible()   { return page.locator(OTP_ERROR_MSG).isVisible(); }
    public String  getOtpErrorText()     { return page.locator(OTP_ERROR_MSG).textContent().trim(); }
    public boolean isTermsLinkVisible()  { return page.locator(TERMS_LINK).isVisible(); }
    public String  getTermsLinkHref()    { return page.locator(TERMS_LINK).getAttribute("href"); }

    public void enterOtp(String otp) {
        page.locator(OTP_INPUT_1).fill(String.valueOf(otp.charAt(0)));
        page.locator(OTP_INPUT_2).fill(String.valueOf(otp.charAt(1)));
        page.locator(OTP_INPUT_3).fill(String.valueOf(otp.charAt(2)));
        page.locator(OTP_INPUT_4).fill(String.valueOf(otp.charAt(3)));
    }

    public void enterPartialOtp(String partial) {
        String[] inputs = {OTP_INPUT_1, OTP_INPUT_2, OTP_INPUT_3, OTP_INPUT_4};
        for (int i = 0; i < partial.length() && i < 4; i++) {
            page.locator(inputs[i]).fill(String.valueOf(partial.charAt(i)));
        }
    }

    public boolean isContinueButtonEnabled() {
        return page.locator(CONTINUE_BTN).isEnabled();
    }

    public void clickContinue() {
        page.locator(CONTINUE_BTN).click();
    }

    public void clickBack() {
        page.locator(BACK_BTN).click();
    }

    public static String generateRandomOtp() {
        return String.format("%04d", ThreadLocalRandom.current().nextInt(0, 10000));
    }

    public static String getTestOtp() {
        return System.getProperty("testOtp", generateRandomOtp());
    }
}
