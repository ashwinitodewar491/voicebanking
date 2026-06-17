package com.voicebanking.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

public class HomePage {

    private final Page page;

    private static final String LANGUAGE_BTN      = "[data-testid='home-language-btn']";
    private static final String USER_MENU_BTN     = "[data-testid='home-user-menu-btn']";
    private static final String BALANCE_TOGGLE_BTN = "[data-testid='home-balance-toggle-btn']";
    private static final String TRANSACTIONS_BTN  = "[data-testid='home-transactions-btn']";
    private static final String HOLD_TO_SPEAK_BTN = "[data-testid='listening-hold-to-speak-btn']";
    private static final String MUTE_TOGGLE_BTN   = "[data-testid='listening-mute-toggle-btn']";

    public HomePage(Page page) {
        this.page = page;
    }

    public void waitForPageLoad() {
        page.waitForTimeout(50000);
        page.locator(HOLD_TO_SPEAK_BTN).waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(15000));
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

    public void clickLanguage() {
        page.locator(LANGUAGE_BTN).click();
    }

    public void clickUserMenu() {
        page.locator(USER_MENU_BTN).click();
    }

    public void clickHoldToSpeak() {
        page.locator(HOLD_TO_SPEAK_BTN).click();
    }

    public void holdToSpeak(int durationMs) {
        var box = page.locator(HOLD_TO_SPEAK_BTN).boundingBox();
        page.mouse().move(box.x + box.width / 2, box.y + box.height / 2);
        page.mouse().down();
        page.waitForTimeout(durationMs);
        page.mouse().up();
    }

    public void clickMuteToggle() {
        page.locator(MUTE_TOGGLE_BTN).click();
    }
}
