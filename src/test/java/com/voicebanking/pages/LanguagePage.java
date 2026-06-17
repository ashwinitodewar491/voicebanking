package com.voicebanking.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

public class LanguagePage {

    private final Page page;

    private static final String LANG_EN   = "[data-testid='language-select-btn-en']";
    private static final String LANG_HI   = "[data-testid='language-select-btn-hi']";
    private static final String LANG_TA   = "[data-testid='language-select-btn-ta']";
    private static final String LANG_KN   = "[data-testid='language-select-btn-kn']";
    private static final String LANG_TE   = "[data-testid='language-select-btn-te']";
    private static final String LANG_ML   = "[data-testid='language-select-btn-ml']";
    private static final String LANG_BN   = "[data-testid='language-select-btn-bn']";
    private static final String LANG_MR   = "[data-testid='language-select-btn-mr']";
    private static final String LANG_GU   = "[data-testid='language-select-btn-gu']";
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

    public void selectEnglish()    { page.locator(LANG_EN).click(); }
    public void selectHindi()      { page.locator(LANG_HI).click(); }
    public void selectTamil()      { page.locator(LANG_TA).click(); }
    public void selectKannada()    { page.locator(LANG_KN).click(); }
    public void selectTelugu()     { page.locator(LANG_TE).click(); }
    public void selectMalayalam()  { page.locator(LANG_ML).click(); }
    public void selectBengali()    { page.locator(LANG_BN).click(); }
    public void selectMarathi()    { page.locator(LANG_MR).click(); }
    public void selectGujarati()   { page.locator(LANG_GU).click(); }

    public void clickContinue() {
        page.locator(CONTINUE_BTN).click();
    }
}
