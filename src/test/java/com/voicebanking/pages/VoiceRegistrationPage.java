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

    /** Polls until the "Recording...N%" readout disappears or reports 100%. The text is read with
     * a short timeout: the readout can vanish between the count check and the read, and an
     * unbounded read then blocked for the full 30s default (seen live, UI15 2026-10-08). */
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

    /**
     * The app validates the recording asynchronously after the progress bar completes — polls
     * for the "Recording not accepted" dialog heading. Returns false if it appears within
     * {@code timeoutMs}, true (accepted) otherwise.
     */
    public boolean waitForRecordingAccepted(int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (page.getByText(NOT_ACCEPTED_HEADING).isVisible()) return false;
            page.waitForTimeout(300);
        }
        return true;
    }

    /**
     * Clicks the rejection dialog's own Re-record button — scoped to the container holding the
     * "Recording not accepted" heading rather than any bare "Re-record" match, since the normal
     * bottom action bar has its own Re-record button present at the same time.
     */
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

    /** Waits for the current enrollment step to actually be ready — at minimum the mic button
     * present — before touching anything else on that step ({@link #isImageLoaded()}, {@link
     * #clickPlayImageDescription()}). Right after Start Registration or a Submit, the screen is
     * still on its "Starting..." transition for a moment and nothing else has mounted yet. */
    public void waitForRecordingScreenReady() {
        page.locator(MIC_BTN).waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(30000));
    }

    /** Polls {@link #isImageLoaded()} instead of checking once — the image can still be mid-load
     * for a brief moment right after the mic button itself becomes visible. */
    public boolean waitForImageLoaded(int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (isImageLoaded()) return true;
            page.waitForTimeout(200);
        }
        return false;
    }

    /** Taps the speaker icon that reads the current enrollment image's description aloud via
     * {@code window.speechSynthesis.speak()} — the actual text spoken is not exposed through any
     * DOM attribute (the {@code <img>} has no alt text), so a caller that wants it must intercept
     * {@code speechSynthesis.speak} itself (e.g. via an init script) before calling this. */
    public void clickPlayImageDescription() {
        page.locator(PLAY_AUDIO_BTN).click();
    }

    /** True once the current enrollment step's image has actually finished loading (not just
     * present in the DOM) — {@code naturalWidth > 0} rules out a broken/still-loading image that
     * {@code img.complete} alone wouldn't catch for a failed load. */
    public boolean isImageLoaded() {
        Object result = page.evaluate(
                "() => { const img = document.querySelector('img'); "
                + "return !!img && img.complete && img.naturalWidth > 0; }");
        return Boolean.TRUE.equals(result);
    }

    /** Waits for the current step's content to mount and reports its type: true for a question
     * step (random question to answer), false for an image step (image to describe). Enrollment is
     * currently 6 steps — 2 image steps, then 4 question steps — but callers shouldn't assume that
     * order or count. */
    public boolean waitForStepContentIsQuestion(int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (page.locator(QUESTION_TEXT).isVisible()) return true;
            if (isImageLoaded()) return false;
            page.waitForTimeout(200);
        }
        return page.locator(QUESTION_TEXT).isVisible();
    }

    /** The question shown on a question step, e.g. "What would you cook if friends came over for
     * dinner?". */
    public String getQuestionText() {
        return page.locator(QUESTION_TEXT).innerText().trim();
    }

    /** The "Step N of M" progress label, or "" if the screen doesn't show one. */
    public String getStepLabel() {
        Locator label = page.getByText(java.util.regex.Pattern.compile("^Step \\d+ of \\d+$"));
        return label.count() > 0 ? label.first().innerText().trim() : "";
    }

    /** Taps the speaker icon and returns the text it reads aloud — the image description on an
     * image step (not exposed anywhere in the DOM; the {@code <img>} has no alt text). Captured by
     * wrapping {@code speechSynthesis.speak} at runtime, so callers need no init script. Returns
     * null if nothing was spoken within {@code timeoutMs}. */
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
