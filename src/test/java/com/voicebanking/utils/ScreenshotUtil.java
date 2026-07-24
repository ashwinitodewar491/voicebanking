package com.voicebanking.utils;

import com.microsoft.playwright.Page;
import org.testng.ITestResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ScreenshotUtil {

    private ScreenshotUtil() {
    }

    /** Captures a screenshot to target/screenshots/ when {@code result} is a failure, naming it
     * after the test class, its first parameter (or method name), and a timestamp. No-op on
     * success, a null/closed page, or if the capture itself throws. */
    public static void captureOnFailure(Page page, ITestResult result) {
        if (result.getStatus() != ITestResult.FAILURE || page == null || page.isClosed()) return;
        try {
            Files.createDirectories(Paths.get("target/screenshots"));
            String label = result.getParameters().length > 0
                    ? result.getParameters()[0].toString().replaceAll("[^a-zA-Z0-9_-]", "_")
                    : result.getMethod().getMethodName();
            String screenshotPath = "target/screenshots/"
                    + result.getTestClass().getRealClass().getSimpleName()
                    + "_" + label + "_" + System.currentTimeMillis() + ".png";
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of(screenshotPath)));
            System.out.println("[Screenshot] " + screenshotPath);
        } catch (Exception e) {
            System.out.println("[Screenshot] Failed to capture: " + e.getMessage());
        }
    }

    /** Captures a screenshot right now, unconditionally — for the exact instant an assertion is
     * about to fail, rather than waiting for @AfterMethod teardown. By teardown time the app may
     * have already self-recovered from a transient state (e.g. a dropped-then-reconnected voice
     * session), making a teardown-only screenshot show a healthy page even though the assertion
     * genuinely failed moments earlier. Filenamed the same way as captureOnFailure but suffixed
     * "_atFailure" so the two are easy to tell apart when both exist for the same failure. */
    public static void captureNow(Page page, String testClassSimpleName, String label) {
        if (page == null || page.isClosed()) return;
        try {
            Files.createDirectories(Paths.get("target/screenshots"));
            String sanitizedLabel = label.replaceAll("[^a-zA-Z0-9_-]", "_");
            String screenshotPath = "target/screenshots/"
                    + testClassSimpleName + "_" + sanitizedLabel + "_" + System.currentTimeMillis() + "_atFailure.png";
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of(screenshotPath)));
            System.out.println("[Screenshot] " + screenshotPath);
        } catch (Exception e) {
            System.out.println("[Screenshot] Failed to capture: " + e.getMessage());
        }
    }
}
