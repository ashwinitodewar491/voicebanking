package com.voicebanking.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

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

    public String getLastTranscribedText() {
        Locator items = page.locator(USER_BUBBLE);
        return items.count() > 0 ? items.last().textContent().trim() : "";
    }

    public String getLastBotResponse() {
        Locator items = page.locator(BOT_BUBBLE);
        if (items.count() > 0) return items.last().textContent().trim();
        Locator wider = page.locator(BOT_CONTAINER);
        return wider.count() > 0 ? wider.last().textContent().trim() : "";
    }
}
