package com.voicebanking.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.voicebanking.utils.SessionEndedTracker;

public class HomePage {

    private final Page page;

    private static final String LANGUAGE_BTN        = "[data-testid='home-language-btn']";
    private static final String USER_MENU_BTN       = "[data-testid='home-user-menu-btn']";
    private static final String BALANCE_TOGGLE_BTN  = "[data-testid='home-balance-toggle-btn']";
    private static final String TRANSACTIONS_BTN    = "[data-testid='home-transactions-btn']";
    private static final String HOLD_TO_SPEAK_BTN   = "[data-testid='listening-hold-to-speak-btn']";

    private static final String USER_BUBBLE    = ".mobile-scroll div.justify-end .whitespace-pre-line";
    private static final String BOT_BUBBLE     = ".mobile-scroll div.justify-start .whitespace-pre-line";
    private static final String BOT_CONTAINER  = ".mobile-scroll div.justify-start";

    private int botBubblesBefore = 0;

    public HomePage(Page page) {
        this.page = page;
    }

    public void waitForPageLoad() {
        page.locator(HOLD_TO_SPEAK_BTN).waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(60000));
    }

    public boolean isPageVisible() {
        return page.locator(HOLD_TO_SPEAK_BTN).isVisible();
    }

    public boolean isBalanceToggleVisible() {
        return page.locator(BALANCE_TOGGLE_BTN).isVisible();
    }

    public boolean isTransactionsButtonVisible() {
        return page.locator(TRANSACTIONS_BTN).isVisible();
    }

    public boolean isLanguageButtonVisible() {
        return page.locator(LANGUAGE_BTN).isVisible();
    }

    public boolean isUserMenuButtonVisible() {
        return page.locator(USER_MENU_BTN).isVisible();
    }

    public void clickBalanceToggle() {
        page.locator(BALANCE_TOGGLE_BTN).click();
    }

    public void clickRecentTransactions() {
        page.locator(TRANSACTIONS_BTN).click();
    }

    public void holdToSpeak(int durationMs) {
        pressAndHold(durationMs);
    }

    /**
     * Waits for the bot's welcome bubble, then holds the mic button for {@code maxHoldMs} ms
     * and polls for a user-bubble after release. Retries up to {@code maxAttempts} times,
     * skipping any attempt where the bot is still in "Speaking" state after a 3-second wait.
     */
    public void holdToSpeakWithRetry(int maxHoldMs, int maxAttempts, int sttPollMs) {
        page.locator(BOT_BUBBLE).first().waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(15000));

        waitForButtonReady(10000, 6000);

        botBubblesBefore = page.locator(BOT_BUBBLE).count();
        int bubblesBefore = page.locator(USER_BUBBLE).count();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            System.out.println("[Voice] Attempt " + attempt + " of " + maxAttempts + " — checking system state...");

            if (isBotSpeaking()) {
                System.out.println("[Voice] Attempt " + attempt + " — bot is Speaking, waiting up to 3 s for Listening...");
                boolean ready = waitForListeningOrTimeout(3000);
                if (!ready) {
                    System.out.println("[Voice] Attempt " + attempt + " — still Speaking after 3 s, retrying...");
                    continue;
                }
            }

            System.out.println("[Voice] Attempt " + attempt + " — system ready, holding to speak...");
            pressAndHold(maxHoldMs);
            page.waitForTimeout(1000); // settle before polling

            System.out.println("[Voice] Attempt " + attempt + " — button released, waiting for STT...");
            boolean transcribed = false;
            int elapsed = 0;
            while (elapsed < sttPollMs) {
                page.waitForTimeout(500);
                elapsed += 500;
                if (page.locator(USER_BUBBLE).count() > bubblesBefore) {
                    transcribed = true;
                    break;
                }
            }

            if (transcribed) {
                System.out.println("[Voice] Transcription detected " + elapsed + " ms after release.");
                page.waitForTimeout(3000);
                return;
            }

            System.out.println("[Voice] Attempt " + attempt + " — no transcription within " + sttPollMs + " ms, retrying...");
        }

        throw new RuntimeException("Speech-to-text not detected after " + maxAttempts + " attempts");
    }

    /**
     * Returns true if the UI is showing a "Speaking" state indicator — checked via
     * {@code data-state}, CSS class, or visible leaf-element text.
     */
    private boolean isBotSpeaking() {
        try {
            return (Boolean) page.evaluate(
                    "() => {" +
                    "  if (document.querySelector('[data-state=\"speaking\"],[data-testid*=\"speaking\"]')) return true;" +
                    "  if (document.querySelector('[class*=\"speaking\"]')) return true;" +
                    "  const els = document.querySelectorAll('span,p,div,label');" +
                    "  for (const el of els) {" +
                    "    if (el.children.length > 0) continue;" +
                    "    if (el.textContent.trim().toLowerCase() !== 'speaking') continue;" +
                    "    const r = el.getBoundingClientRect();" +
                    "    if (r.width > 0 && r.height > 0) return true;" +
                    "  }" +
                    "  return false;" +
                    "}");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns true if the UI is showing a "Listening" state indicator — checked via
     * {@code data-state}, CSS class, or visible leaf-element text.
     */
    private boolean isSystemListening() {
        try {
            return (Boolean) page.evaluate(
                    "() => {" +
                    "  if (document.querySelector('[data-state=\"listening\"],[data-testid*=\"listening-active\"]')) return true;" +
                    "  if (document.querySelector('[class*=\"listening-active\"],[class*=\"is-listening\"]')) return true;" +
                    "  const els = document.querySelectorAll('span,p,div,label');" +
                    "  for (const el of els) {" +
                    "    if (el.children.length > 0) continue;" +
                    "    if (el.textContent.trim().toLowerCase() !== 'listening') continue;" +
                    "    const r = el.getBoundingClientRect();" +
                    "    if (r.width > 0 && r.height > 0) return true;" +
                    "  }" +
                    "  return false;" +
                    "}");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Polls every 300 ms until the bot is no longer Speaking or the system is Listening.
     * Returns true if ready within {@code maxWaitMs}, false on timeout.
     */
    private boolean waitForListeningOrTimeout(int maxWaitMs) {
        long deadline = System.currentTimeMillis() + maxWaitMs;
        while (System.currentTimeMillis() < deadline) {
            if (!isBotSpeaking()) {
                System.out.println("[Voice] Bot finished speaking — proceeding.");
                return true;
            }
            if (isSystemListening()) {
                System.out.println("[Voice] System entered Listening state — proceeding.");
                return true;
            }
            page.waitForTimeout(300);
        }
        System.out.println("[Voice] Timeout: system still Speaking after " + maxWaitMs + " ms.");
        return false;
    }

    /**
     * Waits for the hold-to-speak button to be interactive by checking {@code disabled},
     * {@code aria-disabled}, and {@code pointer-events:none}. Falls back to a fixed delay
     * if the app does not expose those attributes.
     */
    private void waitForButtonReady(int maxWaitMs, int fallbackMs) {
        try {
            page.waitForFunction(
                    "() => { " +
                    "  const btn = document.querySelector('[data-testid=\"listening-hold-to-speak-btn\"]'); " +
                    "  if (!btn) return false; " +
                    "  if (btn.disabled) return false; " +
                    "  if (btn.getAttribute('aria-disabled') === 'true') return false; " +
                    "  const s = window.getComputedStyle(btn); " +
                    "  if (s.pointerEvents === 'none') return false; " +
                    "  return true; " +
                    "}",
                    null,
                    new Page.WaitForFunctionOptions().setTimeout(maxWaitMs));
        } catch (Exception ignored) {
            page.waitForTimeout(fallbackMs);
        }
    }

    /** Waits for the bot response bubble for this specific query to appear. */
    public void waitForVoiceResponse(int timeoutMs) {
        try {
            page.locator(BOT_BUBBLE).nth(botBubblesBefore)
                    .waitFor(new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(timeoutMs));
        } catch (Exception e) {
            page.locator(BOT_CONTAINER).nth(botBubblesBefore)
                    .waitFor(new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(5000));
        }
    }

    /**
     * Stops every MediaStreamTrack handed out via getUserMedia so far (tracked by the init
     * script installed in BaseVoiceTest.setUpBrowserWithAudio). If the app opens the mic once
     * and reuses that same stream for every hold-to-speak press, overwriting the WAV file for a
     * follow-up utterance has no effect — Chromium keeps replaying whatever the *original*
     * stream buffered. Stopping the tracks here forces the app to request a fresh stream on the
     * next press, which should make Chromium's fake audio device pick up the file as it
     * currently is on disk. Safe to call even if the app never re-requests the mic — the ended
     * track just means the button won't be able to record, which the following {@link
     * #speakFollowUp} call will surface as a failure rather than silently misbehaving.
     */
    public void reacquireMicrophoneForFollowUp() {
        try {
            page.evaluate(
                    "() => {" +
                    "  if (!window.__micStreams) return;" +
                    "  window.__micStreams.forEach(s => s.getTracks().forEach(t => t.stop()));" +
                    "  window.__micStreams = [];" +
                    "}");
        } catch (Exception e) {
            System.out.println("[Voice] Failed to reacquire microphone: " + e.getMessage());
        }
    }

    /**
     * Speaks a disambiguation follow-up (e.g. "savings") after the bot has asked the user
     * to choose an account. Waits for the Chromium audio loop to cycle before pressing,
     * then applies the Speaking-state guard before sending.
     */
    public void speakFollowUp(int preWaitMs, int holdMs, int sttPollMs) {
        botBubblesBefore = page.locator(BOT_BUBBLE).count();
        int bubblesBefore = page.locator(USER_BUBBLE).count();

        if (preWaitMs > 0) page.waitForTimeout(preWaitMs);

        waitForButtonReady(5000, 1000);

        if (isBotSpeaking()) {
            System.out.println("[Voice] Follow-up: bot still Speaking — waiting up to 3 s for Listening...");
            waitForListeningOrTimeout(3000);
        }

        pressAndHold(holdMs);
        page.waitForTimeout(1000);

        System.out.println("[Voice] Follow-up button released, waiting for STT...");
        int elapsed = 0;
        while (elapsed < sttPollMs) {
            page.waitForTimeout(500);
            elapsed += 500;
            if (page.locator(USER_BUBBLE).count() > bubblesBefore) {
                System.out.println("[Voice] Follow-up transcription detected " + elapsed + " ms after release.");
                page.waitForTimeout(3000);
                return;
            }
        }
        throw new RuntimeException("Follow-up speech-to-text not detected");
    }

    /**
     * Scrolls the button into view (to avoid a null bounding box when it is off-screen),
     * then holds the mouse down for {@code holdMs} ms and releases.
     */
    private void pressAndHold(int holdMs) {
        recoverFromSessionEndedIfPresent();
        Locator btn = page.locator(HOLD_TO_SPEAK_BTN);
        btn.scrollIntoViewIfNeeded();
        var box = btn.boundingBox();
        if (box == null) {
            throw new RuntimeException("Hold-to-speak button bounding box is null — element may be hidden or detached");
        }
        page.mouse().move(box.x + box.width / 2, box.y + box.height / 2);
        page.mouse().down();
        page.waitForTimeout(holdMs);
        page.mouse().up();
    }

    private static final int SESSION_RECOVERY_ATTEMPTS = 3;

    /**
     * The live bot occasionally ends the conversation session mid-flow (observed during
     * multi-turn transfer follow-ups), replacing the chat screen with a "Session Ended" message
     * and leaving the hold-to-speak button unusable — {@code scrollIntoViewIfNeeded()} then hangs
     * for its full timeout waiting for a button that won't become interactive on its own. The
     * documented recovery is a two-click sequence: click hold-to-speak once to trigger a
     * reconnect (shows a "Connecting" indicator), wait for that to clear, then click hold-to-speak
     * a second time before the normal press-and-hold gesture actually records the query.
     * <p>
     * Every detected occurrence is recorded via {@link SessionEndedTracker}, regardless of
     * whether recovery below succeeds — that count is a run-wide environment-stability signal
     * (target/session-ended-count.txt, surfaced in the dashboard), independent of whether this
     * particular test ultimately passes or fails.
     */
    private void recoverFromSessionEndedIfPresent() {
        if (!isSessionEnded()) return;

        SessionEndedTracker.recordOccurrence();

        for (int attempt = 1; attempt <= SESSION_RECOVERY_ATTEMPTS; attempt++) {
            System.out.println("[Voice] 'Session Ended' detected — reconnect attempt " + attempt
                    + " of " + SESSION_RECOVERY_ATTEMPTS + "...");

            if (!clickHoldToSpeakWithRetry(3, 500)) {
                System.out.println("[Voice] Hold-to-speak button not found for reconnect — retrying...");
                page.waitForTimeout(1000);
                continue;
            }

            waitThroughConnecting(15000);

            System.out.println("[Voice] Reconnected — clicking hold-to-speak again before speaking...");
            if (!clickHoldToSpeakWithRetry(3, 500)) {
                System.out.println("[Voice] Hold-to-speak button not found for second reconnect click — retrying...");
                page.waitForTimeout(1000);
                continue;
            }
            page.waitForTimeout(1000);

            if (!isSessionEnded()) {
                System.out.println("[Voice] Session recovered on attempt " + attempt + ".");
                return;
            }
        }

        System.out.println("[Voice] 'Session Ended' persisted after " + SESSION_RECOVERY_ATTEMPTS
                + " reconnect attempts — giving up.");
    }

    /** Retries {@link #jsClickHoldToSpeakButton()} a few times with a short wait between attempts
     * — the button can be transiently absent from the DOM mid-re-render (e.g. while the
     * "Session Ended" sheet is being torn down) rather than genuinely gone, so a single miss
     * isn't reliable evidence that it never comes back. */
    private boolean clickHoldToSpeakWithRetry(int maxAttempts, int waitMs) {
        for (int i = 1; i <= maxAttempts; i++) {
            if (jsClickHoldToSpeakButton()) return true;
            page.waitForTimeout(waitMs);
        }
        return false;
    }

    /** Clicks the hold-to-speak button by dispatching DOM events directly via
     * {@code page.evaluate()} rather than going through a Playwright {@code Locator} — both
     * {@code Locator.click()} and {@code boundingBox()} wait on Playwright's normal actionability
     * resolution (attached/visible/stable/unobscured), and that wait was silently hanging for the
     * full default timeout (~30 s) against the button while the "Session Ended" sheet's animated
     * waveform overlay is up, even though the button is visibly rendered and DOM-queryable.
     * {@code document.querySelector} plus a manual mousedown/mouseup/click dispatch resolves and
     * fires synchronously, so it can't get stuck the same way. Returns false if no element with
     * the hold-to-speak testid exists in the DOM at all. */
    private boolean jsClickHoldToSpeakButton() {
        try {
            Boolean clicked = (Boolean) page.evaluate(
                    "() => {" +
                    "  const btn = document.querySelector('[data-testid=\"listening-hold-to-speak-btn\"]');" +
                    "  if (!btn) return false;" +
                    "  const opts = {bubbles: true, cancelable: true, view: window};" +
                    "  btn.dispatchEvent(new MouseEvent('mousedown', opts));" +
                    "  btn.dispatchEvent(new MouseEvent('mouseup', opts));" +
                    "  btn.dispatchEvent(new MouseEvent('click', opts));" +
                    "  return true;" +
                    "}");
            return Boolean.TRUE.equals(clicked);
        } catch (Exception e) {
            System.out.println("[Voice] JS click on hold-to-speak failed: " + e.getMessage());
            return false;
        }
    }

    /** Returns true if the chat screen is showing a "Session Ended" message. */
    private boolean isSessionEnded() {
        try {
            return (Boolean) page.evaluate(
                    "() => {" +
                    "  const els = document.querySelectorAll('span,p,div,label,h1,h2,h3');" +
                    "  for (const el of els) {" +
                    "    if (el.children.length > 0) continue;" +
                    "    if (el.textContent.trim().toLowerCase().includes('session ended')) return true;" +
                    "  }" +
                    "  return false;" +
                    "}");
        } catch (Exception e) {
            return false;
        }
    }

    /** Returns true if the UI is showing a "Connecting" state indicator. */
    private boolean isConnecting() {
        try {
            return (Boolean) page.evaluate(
                    "() => {" +
                    "  const els = document.querySelectorAll('span,p,div,label');" +
                    "  for (const el of els) {" +
                    "    if (el.children.length > 0) continue;" +
                    "    if (el.textContent.trim().toLowerCase().includes('connecting')) return true;" +
                    "  }" +
                    "  return false;" +
                    "}");
        } catch (Exception e) {
            return false;
        }
    }

    /** Waits until a "Connecting" indicator has appeared and then cleared, or gives up after
     * {@code maxWaitMs} — some reconnects may resolve too quickly for a 300 ms poll to ever
     * observe the indicator at all, so a full timeout without ever seeing it is not an error. */
    private void waitThroughConnecting(int maxWaitMs) {
        long deadline = System.currentTimeMillis() + maxWaitMs;
        boolean sawConnecting = false;
        while (System.currentTimeMillis() < deadline) {
            boolean connecting = isConnecting();
            if (connecting) {
                sawConnecting = true;
            } else if (sawConnecting) {
                return;
            }
            page.waitForTimeout(300);
        }
    }

    public String getLastTranscribedText() {
        Locator items = page.locator(USER_BUBBLE);
        return items.count() > 0 ? items.last().textContent().trim() : "";
    }

    /**
     * Some bot replies (e.g. transaction lists) render as a structured card instead of a plain
     * .whitespace-pre-line bubble. In that case no new BOT_BUBBLE element appears, so checking
     * "any BOT_BUBBLE exists" would silently return a stale earlier bubble. Comparing against
     * botBubblesBefore detects whether a genuinely new plain-text bubble was added before trusting it.
     */
    public String getLastBotResponse() {
        Locator items = page.locator(BOT_BUBBLE);
        if (items.count() > botBubblesBefore) return items.last().textContent().trim();
        Locator wider = page.locator(BOT_CONTAINER);
        return wider.count() > 0 ? wider.last().textContent().trim() : "";
    }
}
