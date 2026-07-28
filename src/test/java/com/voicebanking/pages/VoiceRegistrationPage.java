package com.voicebanking.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

public class VoiceRegistrationPage {

    private final Page page;

    private static final String CONSENT_CHECKBOX  = "[data-testid='voice-registration-consent-checkbox']";
    private static final String START_BTN         = "[data-testid='voice-registration-start-btn']";
    private static final String SKIP_BTN          = "[data-testid='voice-registration-skip-btn']";
    private static final String MIC_BTN           = "[data-testid='voice-registration-mic-btn']";
    private static final String SUBMIT_BTN        = "[data-testid='voice-registration-submit-btn']";
    private static final String START_BANKING_BTN = "[data-testid='voice-registration-start-banking-btn']";

    // No data-testid on the recording-progress readout itself ("Recording...66%") — matched by
    // its leading text instead.
    private static final String RECORDING_PROGRESS = "p:has-text('Recording')";

    // No data-testid on the "Recording not accepted" quality-check dialog's retry button either.
    private static final String RERECORD_BTN = "button:has-text('Re-record')";

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

    /**
     * One recording take: tap the mic once (no hold), wait for the "Recording...N%" readout to
     * appear (covers the app's ~3s get-ready delay before it actually starts capturing), then
     * wait for it to run its fixed ~15s course. Does not decide whether the take was accepted —
     * see {@link #waitForRecordingAccepted(int)} — since retrying a rejected take requires
     * swapping in freshly generated audio first, which this page object has no TTS access to do.
     */
    public void tapMicAndRecord() {
        page.locator(MIC_BTN).waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(30000));
        page.locator(MIC_BTN).click();

        page.locator(RECORDING_PROGRESS).waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(8000));

        waitForRecordingToComplete(20000);
    }

    /** Polls until the "Recording...N%" readout disappears or reports 100%. */
    private void waitForRecordingToComplete(int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Locator progress = page.locator(RECORDING_PROGRESS);
            if (progress.count() == 0) return;
            String text = progress.first().textContent();
            if (text != null && text.contains("100%")) return;
            page.waitForTimeout(300);
        }
    }

    /** The app validates the recording asynchronously after the progress bar completes — polls
     * briefly for the "Recording not accepted" dialog. Returns false if it appears within
     * {@code timeoutMs}, true (accepted) otherwise. */
    public boolean waitForRecordingAccepted(int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (page.locator(RERECORD_BTN).isVisible()) return false;
            page.waitForTimeout(300);
        }
        return true;
    }

    public void clickRerecord() {
        page.locator(RERECORD_BTN).click();
        page.locator(RERECORD_BTN).waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.HIDDEN)
                        .setTimeout(20000));
    }

    /**
     * Clicks Submit, then waits for the Submit button itself to disappear before returning —
     * confirming the current screen has actually torn down rather than just clicking and moving
     * on. Without this, the next step's wait for the mic button (or Start Banking) could resolve
     * against a stale, about-to-be-replaced element still momentarily present in the DOM during
     * the transition, matching the transient-element behavior this app shows elsewhere (see
     * HomePage's Session-Ended recovery comments for the same class of issue).
     */
    public void clickSubmit() {
        page.locator(SUBMIT_BTN).click();
        page.locator(SUBMIT_BTN).waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.HIDDEN)
                        .setTimeout(20000));
    }

    public boolean isStartBankingVisible() {
        return page.locator(START_BANKING_BTN).isVisible();
    }

    public void clickStartBanking() {
        page.locator(START_BANKING_BTN).waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(30000));
        page.locator(START_BANKING_BTN).click();
    }
}
