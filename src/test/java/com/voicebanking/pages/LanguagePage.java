package com.voicebanking.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

public class LanguagePage {

    private final Page page;

    private static final String LANG_EN      = "[data-testid='language-select-btn-en']";
    private static final String LANG_HI      = "[data-testid='language-select-btn-hi']";
    private static final String LANG_BN      = "[data-testid='language-select-btn-bn']";
    private static final String CONTINUE_BTN = "[data-testid='language-continue-btn']";
    private static final String BACK_BTN     = "[data-testid='language-back-btn']";

    public LanguagePage(Page page) {
        this.page = page;
    }

    public void waitForPageLoad() {
        page.locator(LANG_EN).waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(10000));
    }

    public boolean isPageVisible() {
        return page.locator(LANG_EN).isVisible();
    }

    public boolean isEnglishSelected() {
        return "true".equals(page.locator(LANG_EN).getAttribute("aria-pressed"));
    }

    public void selectEnglish() { page.locator(LANG_EN).click(); }
    public void selectHindi() { page.locator(LANG_HI).click(); }
    public void selectBengali() { page.locator(LANG_BN).click(); }

    public void selectByLocale(String locale) {
        switch (locale) {
            case "en" -> selectEnglish();
            case "hi" -> selectHindi();
            case "bn" -> selectBengali();
            default -> throw new IllegalArgumentException("Unsupported locale: " + locale);
        }
    }

    public void clickContinue() {
        page.locator(CONTINUE_BTN).click();
    }

    public void clickBack() {
        page.locator(BACK_BTN).click();
    }
}
