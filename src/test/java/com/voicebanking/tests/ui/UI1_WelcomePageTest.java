package com.voicebanking.tests.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.testng.annotations.Test;

import com.voicebanking.DataText.Endpoints;
import com.voicebanking.pages.BasePage;
import com.voicebanking.pages.WelcomePage;

public class UI1_WelcomePageTest extends BasePage {

    @Test(groups = {"ui", "regression", "smoke"},
            description = "Should display correct heading, subheading and phone label on welcome page")
    public void testWelcomePageContent() {
        WelcomePage welcomePage = new WelcomePage(page, Endpoints.getUiBaseUrl());
        welcomePage.navigate();
        welcomePage.dismissPwaPopupIfPresent();

        assertThat(welcomePage.heading()).isVisible();
        assertThat(welcomePage.subheading()).isVisible();
        assertThat(welcomePage.phoneLabel()).isVisible();
    }

    @Test(groups = {"ui", "regression"},
            description = "Should display PWA install popup on first visit")
    public void testPwaPopupVisibleOnFirstVisit() {
        WelcomePage welcomePage = new WelcomePage(page, Endpoints.getUiBaseUrl());
        welcomePage.navigate();

        assertThat(welcomePage.pwaNotNowButton()).isVisible();
    }

    @Test(groups = {"ui", "regression"},
            description = "Should dismiss PWA popup when clicking Not Now")
    public void testDismissPwaPopupWithNotNow() {
        WelcomePage welcomePage = new WelcomePage(page, Endpoints.getUiBaseUrl());
        welcomePage.navigate();

        assertThat(welcomePage.pwaNotNowButton()).isVisible();

        welcomePage.clickNotNow();

        assertThat(welcomePage.pwaNotNowButton()).isHidden();
    }

    @Test(groups = {"ui", "regression"},
            description = "Install button should trigger native browser prompt and keep page functional")
    public void testInstallButtonKeepsPageFunctional() {
        WelcomePage welcomePage = new WelcomePage(page, Endpoints.getUiBaseUrl());
        welcomePage.navigate();

        assertThat(welcomePage.pwaNotNowButton()).isVisible();

        // Clicking Install triggers the browser's native PWA install dialog (OS-level).
        // That dialog cannot be automated — the custom popup stays open waiting for it.
        // We verify the Install button is clickable and the page does not break.
        welcomePage.clickInstall();

        assertThat(welcomePage.pwaNotNowButton()).isVisible();

        // Dismiss the popup so the rest of the page is accessible
        welcomePage.clickNotNow();

        assertThat(welcomePage.sendOtpButton()).isEnabled();
    }
}
