package com.voicebanking.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.voicebanking.utils.SessionEndedTracker;

public class HomePage {

    private final Page page;

    private static final String LANGUAGE_BTN        = "[data-testid='home-language-btn']";
    private static final String USER_MENU_BTN       = "[data-testid='home-user-menu-btn']";
    private static final String UNREGISTER_VOICE_BTN = "[data-testid='home-menu-unregister-voice']";
    private static final String REGISTER_VOICE_BTN   = "[data-testid='home-menu-register-voice']";
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

    /** Query in flight, used only to label Session Ended occurrences in {@link SessionEndedTracker}. */
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

    // Locator accessors — for callers using Playwright's own assertThat(Locator)...(), which
    // polls/retries until the assertion holds or times out, instead of a one-shot boolean read.
    public Locator holdToSpeakButton()  { return page.locator(HOLD_TO_SPEAK_BTN); }
    public Locator balanceToggleButton(){ return page.locator(BALANCE_TOGGLE_BTN); }
    public Locator transactionsButton() { return page.locator(TRANSACTIONS_BTN); }
    public Locator languageButton()     { return page.locator(LANGUAGE_BTN); }
    public Locator userMenuButton()     { return page.locator(USER_MENU_BTN); }
    public Locator transactionsList()   { return page.locator(TRANSACTIONS_LIST); }
    public Locator transactionItems()   { return page.locator(TRANSACTION_ITEM); }

    /** True if any bot message is present. Can legitimately be false right after a locale switch. */
    public boolean isWelcomeMessageVisible() {
        return page.locator(BOT_BUBBLE).count() > 0 || page.locator(BOT_CONTAINER).count() > 0;
    }

    /** Polls {@link #isWelcomeMessageVisible()} for up to {@code timeoutMs}; a one-shot check right
     * after load is almost always too early. */
    public boolean waitForWelcomeMessage(int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (isWelcomeMessageVisible()) return true;
            page.waitForTimeout(300);
        }
        return false;
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

    /** Scrolls to the top first, since a long chat can push the user-menu button out of reach. */
    public void clickUserMenu() {
        page.evaluate("() => window.scrollTo(0, 0)");
        page.locator(USER_MENU_BTN).click(new Locator.ClickOptions().setTimeout(10000));
    }

    /** Removes the enrolled voiceprint via the user menu, then verifies the menu flips to "Register
     * your voice". Throws if the removal didn't take effect. */
    public void removeRegisteredVoice() {
        clickUserMenu();

        Locator unregisterBtn = page.locator(UNREGISTER_VOICE_BTN);
        unregisterBtn.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(5000));
        unregisterBtn.click();

        // The confirm dialog's own button is plain "Remove" with no data-testid — exact-matched
        // by role/name so it isn't picked up by the "Remove your voice" menu item's own text.
        Locator confirmBtn = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Remove").setExact(true));
        confirmBtn.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(5000));
        confirmBtn.click();
        // Wait on the dialog itself, not the button. If the backend rejects the delete, the app
        // shows no error and simply leaves the dialog open (confirmed live 2026-10-07:
        // DELETE /voiceprint/... -> 503 on stage), so say that plainly instead of letting the next
        // menu click time out behind the still-open modal.
        Locator confirmDialog = page.locator("[role='dialog'][aria-labelledby='remove-voice-confirm-title']");
        try {
            confirmDialog.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.HIDDEN)
                    .setTimeout(15000));
        } catch (PlaywrightException stillOpen) {
            throw new IllegalStateException("Voice removal not confirmed: the 'Remove voice registration?' "
                    + "dialog is still open 15s after clicking Remove — the backend most likely rejected "
                    + "the DELETE /voiceprint request (check stage's voiceprint service).", stillOpen);
        }

        // Give the backend a moment to actually persist the removal before re-checking the menu
        // — the dialog closing confirms the click landed, not that the account's voiceprint
        // state has flipped yet.
        page.waitForTimeout(1500);

        clickUserMenu();
        Locator registerBtn = page.locator(REGISTER_VOICE_BTN);
        registerBtn.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(10000));
        clickUserMenu(); // close the menu again, leaving Home in its normal resting state
    }

    /** True if the account has a voiceprint (menu offers "Remove your voice"). Landing on Home after
     * login does not imply registration. */
    public boolean isVoiceRegistered() {
        clickUserMenu();
        boolean registered = page.locator(UNREGISTER_VOICE_BTN).isVisible();
        clickUserMenu();
        return registered;
    }

    /** Opens the user menu and clicks "Register your voice", for when the app doesn't auto-prompt
     * registration. */
    public void clickRegisterVoiceFromMenu() {
        clickUserMenu();
        Locator registerBtn = page.locator(REGISTER_VOICE_BTN);
        registerBtn.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(5000));
        registerBtn.click();
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

    /** Checks that the balance doesn't look like a real amount, rather than matching a specific mask
     * glyph. */
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

    /** Opens Recent Transactions, which requires ending the active voice session first (X, then End
     * Session). */
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

    /** Holds the mic to speak, retrying up to {@code maxAttempts} times. Recovers from Session Ended
     * before snapshotting bubble counts so the response baseline is correct. */
    public void holdToSpeakWithRetry(int maxHoldMs, int maxAttempts, int sttPollMs) {
        holdToSpeakWithRetry(maxHoldMs, maxAttempts, sttPollMs, true);
    }

    /** Pass {@code waitForExistingWelcomeBubble = false} right after a locale switch, which clears
     * the chat without posting a new greeting. */
    public void holdToSpeakWithRetry(int maxHoldMs, int maxAttempts, int sttPollMs, boolean waitForExistingWelcomeBubble) {
        recoverFromSessionEndedIfPresent();

        if (waitForExistingWelcomeBubble) {
            page.locator(BOT_BUBBLE).first().waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(15000));
        }

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

    /** True if the UI shows a "Speaking" state. */
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

    /** True if the UI shows a "Listening" state. */
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

    /** Polls until the bot stops Speaking or starts Listening; false on timeout. */
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

    /** Waits for the hold-to-speak button to become interactive, falling back to a fixed delay. */
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

    /** Pause after a new reply first has content, since a streamed reply may still be appending text
     * and could bleed into the next query's read. */
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

    /** Stops every tracked mic track so the app re-acquires the fake mic and picks up the swapped
     * WAV file. */
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

    /** Speaks a follow-up reply (e.g. "savings") after a disambiguation prompt. Recovers from
     * Session Ended before snapshotting counts. */
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

    /** Scrolls the button into view, then holds the mouse down for {@code holdMs} ms. */
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

    /** If the Session Ended screen is up, records it in {@link SessionEndedTracker} and reconnects
     * (click reconnect, wait through Connecting, click again). Public so callers can recover right
     * after reading a response. */
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

    /** Retries {@link #trustedClickButton(String)}, since the button can briefly vanish during a
     * re-render. */
    private boolean clickButtonWithRetry(String testIdSelector, int maxAttempts, int waitMs) {
        for (int i = 1; i <= maxAttempts; i++) {
            if (trustedClickButton(testIdSelector)) return true;
            page.waitForTimeout(waitMs);
        }
        return false;
    }

    /** Clicks the element with a trusted mouse event at its on-screen coordinates. Avoids
     * Playwright's actionability wait (which hangs under the Session Ended overlay) while still
     * counting as a user gesture for mic reinit. False if the element is missing or zero-sized. */
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

    /** True if the Session Ended screen is up, detected via the locale-independent reconnect button
     * rather than English text. */
    public boolean isSessionEnded() {
        try {
            return page.locator(HOLD_TO_RECONNECT_BTN).isVisible();
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

    /** Waits for a Connecting indicator to appear and clear; never seeing it is not an error. */
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

    /** Latest bot reply: a new plain-text bubble if one appeared since the snapshot, otherwise the
     * last structured card. */
    public String getLastBotResponse() {
        Locator items = page.locator(BOT_BUBBLE);
        if (items.count() > botBubblesBefore) return items.last().textContent().trim();
        Locator wider = page.locator(BOT_CONTAINER);
        return wider.count() > 0 ? wider.last().textContent().trim() : "";
    }
}
