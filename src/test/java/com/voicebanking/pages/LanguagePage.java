package com.voicebanking.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

public class LanguagePage {

    private final Page page;

    private static final String LANG_EN      = "[data-testid='language-select-btn-en']";
    private static final String CONTINUE_BTN = "[data-testid='language-continue-btn']";

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

    public void selectByLocale(String locale) {
        if ("en".equals(locale)) {
            selectEnglish();
        } else {
            throw new IllegalArgumentException("Unsupported locale: " + locale);
        }
    }

    public void clickContinue() {
        page.locator(CONTINUE_BTN).click();
    }
}
