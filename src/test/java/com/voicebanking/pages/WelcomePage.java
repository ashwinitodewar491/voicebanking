package com.voicebanking.pages;

import java.util.concurrent.ThreadLocalRandom;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;

public class WelcomePage {

    private final Page page;
    private final String baseUrl;

    private static final String PWA_NOT_NOW_BTN = "[data-testid='pwa-not-now-btn']";
    private static final String PWA_INSTALL_BTN  = "[data-testid='pwa-install-btn']";

    private static final String PHONE_INPUT   = "[data-testid='welcome-phone-input']";
    private static final String SEND_OTP_BTN  = "[data-testid='welcome-send-otp-btn']";

    public WelcomePage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    public void navigate() {
        page.navigate(baseUrl + "/welcome");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    public boolean isPwaPopupVisible() {
        try {
            page.locator(PWA_NOT_NOW_BTN).waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(5000));
            return true;
        } catch (PlaywrightException e) {
            return false;
        }
    }

    public void clickNotNow() {
        page.locator(PWA_NOT_NOW_BTN).click();
    }

    public void clickInstall() {
        page.locator(PWA_INSTALL_BTN).click();
    }

    public void dismissPwaPopupIfPresent() {
        try {
            Locator btn = page.locator(PWA_NOT_NOW_BTN);
            btn.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(3000));
            btn.click();
        } catch (PlaywrightException e) {
            // popup not present, continue
        }
    }

    public void enterPhoneNumber(String phone) {
        page.locator(PHONE_INPUT).fill(phone);
    }

    public String getPhoneInputValue() {
        return page.locator(PHONE_INPUT).inputValue();
    }

    public boolean isSendOtpButtonEnabled() {
        return page.locator(SEND_OTP_BTN).isEnabled();
    }

    public void clickSendOtp() {
        page.locator(SEND_OTP_BTN).click();
    }

    public static String generateRandomPhone() {
        int[] starts = {6, 7, 8, 9};
        int start = starts[ThreadLocalRandom.current().nextInt(4)];
        String rest = String.format("%09d", ThreadLocalRandom.current().nextInt(0, 1_000_000_000));
        return start + rest;
    }
}
