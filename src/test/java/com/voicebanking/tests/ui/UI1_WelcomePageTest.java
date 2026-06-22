package com.voicebanking.tests.ui;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.voicebanking.DataText.Endpoints;
import com.voicebanking.pages.BasePage;
import com.voicebanking.pages.WelcomePage;

public class UI1_WelcomePageTest extends BasePage {

    @Test(groups = {"ui", "regression"},
            description = "Should display correct heading, subheading and phone label on welcome page")
    public void testWelcomePageContent() {
        WelcomePage welcomePage = new WelcomePage(page, Endpoints.getUiBaseUrl());
        welcomePage.navigate();
        welcomePage.dismissPwaPopupIfPresent();

        Assert.assertTrue(welcomePage.isHeadingVisible(),
                "Heading 'VoiceBank' should be visible on the welcome page");
        Assert.assertTrue(welcomePage.isSubheadingVisible(),
                "Subheading 'Bank with Your Voice' should be visible on the welcome page");
        Assert.assertTrue(welcomePage.isPhoneLabelVisible(),
                "Label 'Mobile Number' should be visible on the welcome page");
    }

    @Test(groups = {"ui", "regression"},
            description = "Should display PWA install popup on first visit")
    public void testPwaPopupVisibleOnFirstVisit() {
        WelcomePage welcomePage = new WelcomePage(page, Endpoints.getUiBaseUrl());
        welcomePage.navigate();

        Assert.assertTrue(
                welcomePage.isPwaPopupVisible(),
                "PWA install popup should be visible when visiting the site for the first time");
    }

    @Test(groups = {"ui", "regression"},
            description = "Should dismiss PWA popup when clicking Not Now")
    public void testDismissPwaPopupWithNotNow() {
        WelcomePage welcomePage = new WelcomePage(page, Endpoints.getUiBaseUrl());
        welcomePage.navigate();

        Assert.assertTrue(
                welcomePage.isPwaPopupVisible(),
                "PWA popup should be visible before dismissal");

        welcomePage.clickNotNow();

        Assert.assertFalse(
                welcomePage.isPwaPopupVisible(),
                "PWA popup should be dismissed after clicking Not Now");
    }

    @Test(groups = {"ui", "regression"},
            description = "Install button should trigger native browser prompt and keep page functional")
    public void testInstallButtonKeepsPageFunctional() {
        WelcomePage welcomePage = new WelcomePage(page, Endpoints.getUiBaseUrl());
        welcomePage.navigate();

        Assert.assertTrue(
                welcomePage.isPwaPopupVisible(),
                "PWA popup should be visible before clicking Install");

        // Clicking Install triggers the browser's native PWA install dialog (OS-level).
        // That dialog cannot be automated — the custom popup stays open waiting for it.
        // We verify the Install button is clickable and the page does not break.
        welcomePage.clickInstall();

        Assert.assertTrue(
                welcomePage.isPwaPopupVisible(),
                "PWA popup should remain visible while waiting for native install dialog");

        // Dismiss the popup so the rest of the page is accessible
        welcomePage.clickNotNow();

        Assert.assertTrue(
                welcomePage.isSendOtpButtonEnabled(),
                "Send OTP button should be accessible after dismissing the PWA popup");
    }
}
