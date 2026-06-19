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

    // Chat message selectors derived from app HTML
    private static final String USER_BUBBLE = ".mobile-scroll div.justify-end .whitespace-pre-line";
    private static final String BOT_BUBBLE  = ".mobile-scroll div.justify-start .whitespace-pre-line";

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
        var box = page.locator(HOLD_TO_SPEAK_BTN).boundingBox();
        page.mouse().move(box.x + box.width / 2, box.y + box.height / 2);
        page.mouse().down();
        page.waitForTimeout(durationMs);
        page.mouse().up();
    }

    /**
     * Speaks and polls to confirm speech-to-text appeared in chat.
     * If the user bubble doesn't appear within sttPollMs, releases the button and retries.
     * Throws if all attempts fail.
     */
    public void holdToSpeakWithRetry(int durationMs, int maxAttempts, int sttPollMs) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            System.out.println("[Voice] Attempt " + attempt + " of " + maxAttempts + " — speaking...");
            holdToSpeak(durationMs);
            try {
                page.locator(USER_BUBBLE).first()
                        .waitFor(new Locator.WaitForOptions()
                                .setState(WaitForSelectorState.VISIBLE)
                                .setTimeout(sttPollMs));
                System.out.println("[Voice] Speech-to-text confirmed on attempt " + attempt);
                page.waitForTimeout(5000);  // small delay to ensure bot response is ready
                return;
            } catch (Exception e) {
                System.out.println("[Voice] Attempt " + attempt + " — no transcription detected, retrying...");
            }
        }
        throw new RuntimeException(
                "Speech-to-text not detected after " + maxAttempts + " attempts");
    }

    public void waitForVoiceResponse(int timeoutMs) {
        // Wait for transcribed user message to appear (confirms audio was heard)
        page.locator(USER_BUBBLE).first()
                .waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(timeoutMs));
        // nth(0) = bot welcome message, nth(1) = response to the voice query
        page.locator(BOT_BUBBLE).nth(1)
                .waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(timeoutMs));
    }

    public String getLastTranscribedText() {
        Locator items = page.locator(USER_BUBBLE);
        return items.count() > 0 ? items.last().textContent().trim() : "";
    }

    public String getLastBotResponse() {
        Locator items = page.locator(BOT_BUBBLE);
        return items.count() > 0 ? items.last().textContent().trim() : "";
    }

}
