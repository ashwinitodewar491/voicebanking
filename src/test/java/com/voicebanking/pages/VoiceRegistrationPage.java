package com.voicebanking.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

public class VoiceRegistrationPage {

    private final Page page;

    private static final String CONSENT_CHECKBOX  = "[data-testid='voice-registration-consent-checkbox']";
    private static final String START_BTN         = "[data-testid='voice-registration-start-btn']";
    private static final String SKIP_BTN          = "[data-testid='voice-registration-skip-btn']";

    public VoiceRegistrationPage(Page page) {
        this.page = page;
    }

    public void waitForPageLoad() {
        page.locator(SKIP_BTN).waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(10000));
    }

    public boolean isPageVisible() {
        return page.locator(SKIP_BTN).isVisible();
    }

    public boolean isStartButtonDisabled() {
        return !page.locator(START_BTN).isEnabled();
    }

    public void checkConsent() {
        page.locator(CONSENT_CHECKBOX).check();
    }

    public void uncheckConsent() {
        page.locator(CONSENT_CHECKBOX).uncheck();
    }

    public boolean isConsentChecked() {
        return page.locator(CONSENT_CHECKBOX).isChecked();
    }

    public void clickStartRegistration() {
        page.locator(START_BTN).click();
    }

    public void clickSkipForNow() {
        page.locator(SKIP_BTN).click();
    }
}
