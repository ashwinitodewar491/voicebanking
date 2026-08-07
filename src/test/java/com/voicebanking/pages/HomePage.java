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
    private static final String HOLD_TO_RECONNECT_BTN = "[data-testid='listening-reconnect-btn']";
    private static final String LISTENING_CLOSE_BTN     = "[data-testid='listening-close-btn']";
    private static final String TRANSACTIONS_COLLAPSE_BTN
            = "[data-testid='home-transactions-collapse-btn']";

    // No data-testid is exposed on the balance figure or the transactions list itself —
    // these fall back to the class combinations from the markup, scoped to avoid collision
    // with the .mobile-scroll chat-bubble containers (BOT_BUBBLE/USER_BUBBLE below), which
    // reuse the same "mobile-scroll" utility class for an unrelated scroll region.
    private static final String BALANCE_VALUE       = "div.mt-1.flex.flex-col.gap-1 > div:first-child";
    private static final String END_SESSION_CONFIRM_BTN = "button:has-text('End Session')";
    private static final String TRANSACTIONS_LIST
            = "div.max-h-52.space-y-2.overflow-y-auto.mobile-scroll";
    private static final String TRANSACTION_ITEM    = TRANSACTIONS_LIST + " > div";

    private static final String USER_BUBBLE    = ".mobile-scroll div.justify-end .whitespace-pre-line";
    private static final String BOT_BUBBLE     = ".mobile-scroll div.justify-start .whitespace-pre-line";
    private static final String BOT_CONTAINER  = ".mobile-scroll div.justify-start";

    private int botBubblesBefore = 0;
    private int containersBefore = 0;

    /** Which voice query is currently in flight — set by the test layer (see {@link
     * #setCurrentQueryName}) purely so a "Session Ended" occurrence can be logged against the
     * query that was running when it happened, for {@link SessionEndedTracker}'s detail log. */
    private String currentQueryName = "unknown";

    public HomePage(Page page) {
        this.page = page;
    }

    public void setCurrentQueryName(String queryName) {
        this.currentQueryName = queryName;
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

    public void clickLanguageButton() {
        page.locator(LANGUAGE_BTN).click();
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

    public String getBalanceValueText() {
        return page.locator(BALANCE_VALUE).textContent().trim();
    }

    private static final java.util.regex.Pattern CURRENCY_AMOUNT
            = java.util.regex.Pattern.compile("^₹[\\d,]+(?:\\.\\d+)?$");

    /**
     * Checked as "does not look like a real amount" rather than for a specific mask glyph
     * (e.g. bullet dots) — the app's masking character isn't guaranteed and this avoids
     * hardcoding one.
     */
    public boolean isBalanceMasked() {
        return !CURRENCY_AMOUNT.matcher(getBalanceValueText()).matches();
    }

    /** Revealing the balance briefly shows "Loading..." while the amount is fetched. */
    public void waitForBalanceToSettle(int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (!"Loading...".equalsIgnoreCase(getBalanceValueText())) return;
            page.waitForTimeout(300);
        }
    }

    public String getBalanceToggleAriaLabel() {
        return page.locator(BALANCE_TOGGLE_BTN).getAttribute("aria-label");
    }

    /**
     * Viewing Recent Transactions requires ending the active voice-listening session first:
     * clicking the transactions button reveals a close ("X") icon, which opens an "End Session"
     * confirmation — only after confirming does the transaction list actually render.
     */
    public void openRecentTransactions() {
        clickRecentTransactions();

        page.locator(LISTENING_CLOSE_BTN).waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(10000));
        page.locator(LISTENING_CLOSE_BTN).click();

        page.locator(END_SESSION_CONFIRM_BTN).waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(5000));
        page.locator(END_SESSION_CONFIRM_BTN).click();

        page.locator(TRANSACTIONS_LIST).waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(10000));
    }

    public boolean isTransactionsListVisible() {
        return page.locator(TRANSACTIONS_LIST).isVisible();
    }

    public int getTransactionItemCount() {
        return page.locator(TRANSACTION_ITEM).count();
    }

    public String getTransactionDescription(int index) {
        return page.locator(TRANSACTION_ITEM).nth(index)
                .locator("div.truncate").textContent().trim();
    }

    public String getTransactionMeta(int index) {
        return page.locator(TRANSACTION_ITEM).nth(index)
                .locator("div.min-w-0 > div").nth(1).textContent().trim();
    }

    public String getTransactionAmount(int index) {
        return page.locator(TRANSACTION_ITEM).nth(index)
                .locator("div.shrink-0").textContent().trim();
    }

    public void collapseRecentTransactions() {
        page.locator(TRANSACTIONS_COLLAPSE_BTN).click();
    }

    public void holdToSpeak(int durationMs) {
        pressAndHold(durationMs);
    }

    /**
     * Waits for the bot's welcome bubble, then holds the mic button for {@code maxHoldMs} ms
     * and polls for a user-bubble after release. Retries up to {@code maxAttempts} times,
     * skipping any attempt where the bot is still in "Speaking" state after a 3-second wait.
     * <p>
     * Recovers from a "Session Ended" state, if one is already present, before doing anything
     * else — {@link #pressAndHold} also does this recovery, but only later in this method's own
     * flow, after {@code botBubblesBefore}/{@code containersBefore}/{@code bubblesBefore} would
     * already have been snapshotted below. A recovery can reset the chat area to a fresh
     * conversation, so a snapshot taken before it reflects a state that no longer exists —
     * {@link #waitForVoiceResponse} would then be comparing against the wrong baseline,
     * either never seeing the count rise past a now-irrelevant pre-recovery number (missing a
     * genuine reply entirely) or crediting this turn with content left over from the reconnect
     * itself. Recovering first means the snapshot below is always taken against whatever
     * conversation state this turn will actually run in.
     */
    public void holdToSpeakWithRetry(int maxHoldMs, int maxAttempts, int sttPollMs) {
        recoverFromSessionEndedIfPresent();

        page.locator(BOT_BUBBLE).first().waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(15000));

        waitForButtonReady(10000, 6000);

        botBubblesBefore = page.locator(BOT_BUBBLE).count();
        containersBefore = page.locator(BOT_CONTAINER).count();
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

    /**
     * Waits for a new bot response — plain-text bubble or structured card — to appear after this
     * turn's query <em>and actually have content</em>, by polling until either {@link #BOT_BUBBLE}'s
     * or {@link #BOT_CONTAINER}'s count exceeds its pre-turn snapshot ({@code botBubblesBefore} /
     * {@code containersBefore}) AND {@link #getLastBotResponse()} returns non-blank text.
     * <p>
     * An earlier version indexed a specific element via {@code Locator.nth(count).waitFor(...)}.
     * That turned out to be unreliable: a single structured-card reply (e.g. a transaction list)
     * can itself contain nested {@code div}s that also match the {@code BOT_CONTAINER} selector
     * (each transaction line's own flex wrapper, for instance), so the count doesn't necessarily
     * advance by exactly one per turn. {@code nth(containersBefore)} could then point past where
     * the genuinely new reply actually landed, timing out even though the reply had rendered
     * correctly and was visible on screen. Polling for "did the count increase at all" fixed that,
     * but on its own was still too eager: the app appears to insert an empty reply container as
     * soon as it starts responding and stream the actual text into it afterward, so a
     * count-only check could return the instant that empty placeholder appears — moments before
     * its text streams in — leaving {@link #getLastBotResponse()} to read blank. That looked like
     * "no response" in the test even though the response was genuinely on screen a second later
     * (visible in the failure screenshot, taken immediately after). Requiring non-blank content
     * before returning closes that gap.
     * <p>
     * A short settle pause ({@link #RESPONSE_SETTLE_MS}) runs after non-blank content is first
     * detected, before this method returns control to the caller. Non-blank isn't necessarily
     * final — a streamed reply can still be mid-stream at that instant — and without a pause the
     * very next query could start (and its own response get captured) while this turn's text is
     * still being appended to, letting one query's response bleed into the next one's read.
     */
    private static final int RESPONSE_SETTLE_MS = 1200;

    public void waitForVoiceResponse(int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            boolean newReplyStarted = page.locator(BOT_BUBBLE).count() > botBubblesBefore
                    || page.locator(BOT_CONTAINER).count() > containersBefore;
            if (newReplyStarted && !getLastBotResponse().isBlank()) {
                page.waitForTimeout(RESPONSE_SETTLE_MS);
                return;
            }
            page.waitForTimeout(300);
        }
        // Timing out here isn't itself treated as fatal — the caller's own assertions (bot
        // response non-empty, pattern match) will surface a genuine failure. A response that
        // finishes rendering moments after this deadline shouldn't be double-penalized here too.
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
     * <p>
     * Recovers from a "Session Ended" state first, before snapshotting counts — see {@link
     * #holdToSpeakWithRetry}'s javadoc for why taking the snapshot before a recovery (which
     * {@link #pressAndHold} would otherwise only run later in this method's own flow) leaves it
     * comparing against a baseline that no longer reflects the actual conversation.
     */
    public void speakFollowUp(int preWaitMs, int holdMs, int sttPollMs) {
        recoverFromSessionEndedIfPresent();

        botBubblesBefore = page.locator(BOT_BUBBLE).count();
        containersBefore = page.locator(BOT_CONTAINER).count();
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
     * <p>
     * Public (not just used internally by {@link #pressAndHold}) so callers can also recover
     * right after reading a response, not only right before speaking the next query. A session can
     * end during or immediately after the current turn's answer — by the time the caller checks
     * {@link #isPageVisible()}, the ordinary hold-to-speak button may already be gone, replaced by
     * this screen's own "Hold to reconnect" control. {@code pressAndHold}'s recovery only runs when
     * the *next* query tries to speak, too late to save that check.
     */
    public void recoverFromSessionEndedIfPresent() {
        if (!isSessionEnded()) return;

        SessionEndedTracker.recordOccurrence(currentQueryName);

        for (int attempt = 1; attempt <= SESSION_RECOVERY_ATTEMPTS; attempt++) {
            System.out.println("[Voice] 'Session Ended' detected — reconnect attempt " + attempt
                    + " of " + SESSION_RECOVERY_ATTEMPTS + "...");

            if (!clickButtonWithRetry(HOLD_TO_RECONNECT_BTN, 3, 500)) {
                System.out.println("[Voice] Reconnect button not found — retrying...");
                page.waitForTimeout(1000);
                continue;
            }

            waitThroughConnecting(15000);

            System.out.println("[Voice] Reconnected — clicking hold-to-speak again before speaking...");
            if (!clickButtonWithRetry(HOLD_TO_SPEAK_BTN, 3, 500)) {
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

    /** Retries {@link #trustedClickButton(String)} a few times with a short wait between
     * attempts — the button can be transiently absent from the DOM mid-re-render (e.g. while the
     * "Session Ended" sheet is being torn down) rather than genuinely gone, so a single miss
     * isn't reliable evidence that it never comes back. */
    private boolean clickButtonWithRetry(String testIdSelector, int maxAttempts, int waitMs) {
        for (int i = 1; i <= maxAttempts; i++) {
            if (trustedClickButton(testIdSelector)) return true;
            page.waitForTimeout(waitMs);
        }
        return false;
    }

    /**
     * Clicks the hold-to-speak button via a real, OS-trusted mouse click at its on-screen
     * coordinates, obtained through a raw (non-blocking) {@code getBoundingClientRect()} call
     * rather than Playwright's {@code Locator.click()}/{@code boundingBox()} — both of those wait
     * on Playwright's normal actionability resolution (attached/visible/stable/unobscured), and
     * that wait was silently hanging for the full default timeout (~30 s) against this button
     * while the "Session Ended" sheet's animated waveform overlay is up, even though the button is
     * visibly rendered and DOM-queryable.
     * <p>
     * An earlier version dispatched a synthetic {@code MouseEvent} via {@code page.evaluate()} to
     * sidestep that hang. That resolved instantly, but a script-dispatched DOM event has {@code
     * isTrusted: false} — Chromium's WebRTC/microphone reinitialization on reconnect needs a
     * trusted user gesture to actually take effect, so the click could visually register (button
     * state changes, a "Connecting" indicator appears) without the audio pipeline actually coming
     * back, leaving the run stuck in a state that looked recovered but wasn't — matching a case
     * where automation hung mid-session and only a real manual click on the button un-stuck it.
     * {@link Page#mouse()} dispatches genuine trusted input through the browser's DevTools
     * protocol, the same as real user input, without going through {@code Locator}'s actionability
     * checks — avoiding both the hang and the trust problem.
     * <p>
     * Returns false if the button isn't in the DOM, or has zero size, at the moment checked.
     * <p>
     * Takes a CSS selector rather than being hardcoded to one button — the "Session Ended" screen's
     * reconnect control is a genuinely different element ({@code data-testid="listening-reconnect-btn"},
     * text "Hold to reconnect") from the ordinary hold-to-speak button
     * ({@code data-testid="listening-hold-to-speak-btn"}), confirmed against the live DOM. It only
     * relabels back to "Hold to speak" — same testid as the ordinary button — once actually
     * reconnected, so the recovery sequence must click the reconnect-specific selector first.
     */
    private boolean trustedClickButton(String selector) {
        try {
            Object point = page.evaluate(
                    "(sel) => {" +
                    "  const btn = document.querySelector(sel);" +
                    "  if (!btn) return null;" +
                    "  const r = btn.getBoundingClientRect();" +
                    "  if (r.width === 0 || r.height === 0) return null;" +
                    "  return {x: r.x + r.width / 2, y: r.y + r.height / 2};" +
                    "}", selector);
            if (point == null) return false;

            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> center = (java.util.Map<String, Object>) point;
            double x = ((Number) center.get("x")).doubleValue();
            double y = ((Number) center.get("y")).doubleValue();

            page.mouse().move(x, y);
            page.mouse().down();
            page.mouse().up();
            return true;
        } catch (Exception e) {
            System.out.println("[Voice] Trusted click on '" + selector + "' failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns true if the chat screen is showing a "Session Ended" message.
     * <p>
     * Checks {@code document.body.innerText} (the page's rendered, visible text) for the phrase,
     * rather than requiring a single leaf element's own text to contain it in full. The latter
     * missed a real occurrence live — the heading's text was apparently split across sibling
     * elements (e.g. separate spans), so no individual leaf's {@code textContent} contained
     * "session ended" as one contiguous substring even though it read that way on screen,
     * silently skipping recovery for an actual "Session Ended" state.
     */
    private boolean isSessionEnded() {
        try {
            return (Boolean) page.evaluate(
                    "() => document.body.innerText.toLowerCase().includes('session ended')");
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
