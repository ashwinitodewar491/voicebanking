package com.voicebanking.tests.ui;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.voicebanking.DataText.Endpoints;
import com.voicebanking.pages.BasePage;
import com.voicebanking.pages.WelcomePage;

public class UI1_WelcomePageTest extends BasePage {

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
}
