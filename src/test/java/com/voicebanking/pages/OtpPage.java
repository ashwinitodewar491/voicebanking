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

    public OtpPage(Page page) {
        this.page = page;
    }

    public void waitForPageLoad() {
        page.locator(OTP_INPUT_1).waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(10000));
    }

    public boolean isOtpPageVisible() {
        return page.locator(OTP_INPUT_1).isVisible();
    }

    public void enterOtp(String otp) {
        page.locator(OTP_INPUT_1).fill(String.valueOf(otp.charAt(0)));
        page.locator(OTP_INPUT_2).fill(String.valueOf(otp.charAt(1)));
        page.locator(OTP_INPUT_3).fill(String.valueOf(otp.charAt(2)));
        page.locator(OTP_INPUT_4).fill(String.valueOf(otp.charAt(3)));
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
}
