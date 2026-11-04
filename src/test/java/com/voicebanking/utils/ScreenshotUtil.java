package com.voicebanking.utils;

import com.microsoft.playwright.Page;
import org.testng.ITestResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ScreenshotUtil {

    private ScreenshotUtil() {
    }

    /** Saves a screenshot to target/screenshots/ on failure; no-op otherwise. */
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

    /** Saves a screenshot immediately (suffixed "_atFailure"), before the app can self-recover. */
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
