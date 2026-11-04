package com.voicebanking.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

public class VoiceRegistrationPage {

    private final Page page;

    private static final String CONSENT_CHECKBOX  = "[data-testid='voice-registration-consent-checkbox']";
    private static final String START_BTN         = "[data-testid='voice-registration-start-btn']";
    private static final String SKIP_BTN          = "[data-testid='voice-registration-skip-btn']";
    private static final String MIC_BTN           = "[data-testid='voice-registration-mic-btn']";
    private static final String SUBMIT_BTN        = "[data-testid='voice-registration-submit-btn']";
    private static final String START_BANKING_BTN = "[data-testid='voice-registration-start-banking-btn']";
    private static final String PLAY_AUDIO_BTN    = "[data-testid='voice-registration-play-audio-btn']";
    // Question steps (added 2026-10: steps 3-6 of 6) show a random question to answer instead of
    // an image to describe.
    private static final String QUESTION_TEXT     = "[data-testid='voice-registration-question']";

    // No data-testid on the recording-progress readout itself ("Recording...66%") — matched by
    // its leading text instead.
    private static final String RECORDING_PROGRESS = "p:has-text('Recording')";

    // No data-testid on the quality-check rejection dialog either. It only appears when a take
    // genuinely fails ("Recording not accepted" / "The audio did not pass the required checks.
    // Kindly re-record and try again.") — the normal Re-record/Submit bar underneath is always
    // present regardless of take quality, so that pair alone can't be used to detect rejection.
    private static final String NOT_ACCEPTED_HEADING = "Recording not accepted";

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

    // Locator accessors — for callers using Playwright's own assertThat(Locator)...(), which
    // polls/retries until the assertion holds or times out, instead of a one-shot boolean read.
    public Locator skipButton()       { return page.locator(SKIP_BTN); }
    public Locator startButton()      { return page.locator(START_BTN); }
    public Locator consentCheckbox()  { return page.locator(CONSENT_CHECKBOX); }

    public void clickStartRegistration() {
        page.locator(START_BTN).click();
    }

    public void clickSkipForNow() {
        page.locator(SKIP_BTN).click();
    }

    /** Taps the mic and waits for one ~15s recording take to finish. Use
     * {@link #waitForRecordingAccepted(int)} to check the result. */
    public void tapMicAndRecord() {
        page.locator(MIC_BTN).waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(30000));
        page.locator(MIC_BTN).click();

        try {
            page.locator(RECORDING_PROGRESS).waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(8000));
        } catch (PlaywrightException notStarted) {
            // Seen live (UI14, stage 2026-10-08): the first tap occasionally doesn't start
            // recording at all — the step stays on "Tap to start speaking". Tap once more.
            System.out.println("[VoiceRegistration] Recording didn't start after tapping the mic — tapping again...");
            page.locator(MIC_BTN).click();
            page.locator(RECORDING_PROGRESS).waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(8000));
        }

        waitForRecordingToComplete(20000);
    }

    /** Polls until the recording readout disappears or reaches 100%. */
    private void waitForRecordingToComplete(int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Locator progress = page.locator(RECORDING_PROGRESS);
            if (progress.count() == 0) return;
            String text;
            try {
                text = progress.first().textContent(new Locator.TextContentOptions().setTimeout(1000));
            } catch (PlaywrightException gone) {
                return;
            }
            if (text != null && text.contains("100%")) return;
            page.waitForTimeout(300);
        }
    }

    /** True if no "Recording not accepted" dialog appears within {@code timeoutMs}. */
    public boolean waitForRecordingAccepted(int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (page.getByText(NOT_ACCEPTED_HEADING).isVisible()) return false;
            page.waitForTimeout(300);
        }
        return true;
    }

    /** Clicks Re-record inside the rejection dialog, not the bottom bar's Re-record. */
    public void clickRerecord() {
        Locator dialogRerecord = page.locator("div")
                .filter(new Locator.FilterOptions().setHasText(NOT_ACCEPTED_HEADING))
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Re-record"))
                .last();
        dialogRerecord.click();
        page.getByText(NOT_ACCEPTED_HEADING).waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.HIDDEN)
                        .setTimeout(20000));
    }

    /** Clicks Submit and waits for the button to disappear, so the next step doesn't match stale
     * elements. */
    public void clickSubmit() {
        page.locator(SUBMIT_BTN).click();
        try {
            page.locator(SUBMIT_BTN).waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.HIDDEN)
                            .setTimeout(20000));
        } catch (PlaywrightException stillShowing) {
            // Seen live (UI15, stage 2026-10-08): Submit greys out while uploading, then comes back
            // as a clickable "Submit" and stays — the upload was dropped. Submit once more.
            if (!page.locator(SUBMIT_BTN).isEnabled()) throw stillShowing;
            System.out.println("[VoiceRegistration] Submit came back without moving on — submitting again...");
            page.locator(SUBMIT_BTN).click();
            page.locator(SUBMIT_BTN).waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.HIDDEN)
                            .setTimeout(20000));
        }
    }

    public boolean isStartBankingVisible() {
        return page.locator(START_BANKING_BTN).isVisible();
    }

    /** Waits for the current enrollment step's mic button. */
    public void waitForRecordingScreenReady() {
        page.locator(MIC_BTN).waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(30000));
    }

    /** Polls {@link #isImageLoaded()} for up to {@code timeoutMs}. */
    public boolean waitForImageLoaded(int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (isImageLoaded()) return true;
            page.waitForTimeout(200);
        }
        return false;
    }

    /** Taps the speaker icon that reads the image description aloud via speechSynthesis. */
    public void clickPlayImageDescription() {
        page.locator(PLAY_AUDIO_BTN).click();
    }

    /** True once the step image has actually loaded ({@code naturalWidth > 0}). */
    public boolean isImageLoaded() {
        Object result = page.evaluate(
                "() => { const img = document.querySelector('img'); "
                + "return !!img && img.complete && img.naturalWidth > 0; }");
        return Boolean.TRUE.equals(result);
    }

    /** Waits for step content; true for a question step, false for an image step. */
    public boolean waitForStepContentIsQuestion(int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (page.locator(QUESTION_TEXT).isVisible()) return true;
            if (isImageLoaded()) return false;
            page.waitForTimeout(200);
        }
        return page.locator(QUESTION_TEXT).isVisible();
    }

    /** The question shown on a question step. */
    public String getQuestionText() {
        return page.locator(QUESTION_TEXT).innerText().trim();
    }

    /** The "Step N of M" progress label, or "" if the screen doesn't show one. */
    public String getStepLabel() {
        Locator label = page.getByText(java.util.regex.Pattern.compile("^Step \\d+ of \\d+$"));
        return label.count() > 0 ? label.first().innerText().trim() : "";
    }

    /** Taps the speaker icon and returns the text spoken, captured by wrapping
     * speechSynthesis.speak. Null on timeout. */
    public String capturePlayAudioText(int timeoutMs) {
        page.evaluate("() => {"
                + "  window.__enrollSpoken = null;"
                + "  if (!window.speechSynthesis || window.__enrollSpeakPatched) return;"
                + "  const original = window.speechSynthesis.speak.bind(window.speechSynthesis);"
                + "  window.speechSynthesis.speak = u => { window.__enrollSpoken = u.text; return original(u); };"
                + "  window.__enrollSpeakPatched = true;"
                + "}");
        clickPlayImageDescription();
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Object spoken = page.evaluate("() => window.__enrollSpoken");
            if (spoken != null) {
                // Only the text is needed — stop the read-aloud so it can't hold up the mic tap
                // that follows (suspected cause of a recording that never started, UI14 2026-10-08).
                page.evaluate("() => window.speechSynthesis && window.speechSynthesis.cancel()");
                page.waitForTimeout(500);
                return spoken.toString();
            }
            page.waitForTimeout(100);
        }
        return null;
    }

    /** After a Submit, waits until either the next step's mic button or the final "Start Banking"
     * screen appears. Returns true when enrollment is finished (Start Banking showing). */
    public boolean waitForNextStepOrFinish(int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (page.locator(START_BANKING_BTN).isVisible()) return true;
            if (page.locator(MIC_BTN).isVisible()) return false;
            page.waitForTimeout(300);
        }
        throw new IllegalStateException("Neither the next enrollment step nor Start Banking appeared within "
                + timeoutMs + "ms after Submit");
    }

    public void clickStartBanking() {
        page.locator(START_BANKING_BTN).waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(30000));
        page.locator(START_BANKING_BTN).click();
    }
}
