package com.voicebanking.pages;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class BasePage {

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;

    @BeforeMethod(alwaysRun = true)
    public void setUpBrowser() {
        playwright = Playwright.create();
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));
        double slowMo = headless ? 0 : 800;

        List<String> args = new ArrayList<>();
        String audioFile = System.getProperty("audioFile");
        if (audioFile != null) {
            args.add("--use-fake-device-for-media-stream");
            args.add("--use-file-for-fake-audio-capture=" + audioFile);
        }

        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setSlowMo(slowMo)
                        .setArgs(args));
        context = browser.newContext(
                new Browser.NewContextOptions()
                        .setPermissions(List.of("microphone"))
                        .setRecordVideoDir(Paths.get("target/videos")));
        page = context.newPage();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        boolean failed = result.getStatus() == ITestResult.FAILURE;
        Path videoPath = null;

        if (page != null) {
            if (failed) {
                try {
                    Files.createDirectories(Paths.get("target/screenshots"));
                    String name = result.getName() + "_" + System.currentTimeMillis() + ".png";
                    Files.write(Paths.get("target/screenshots/" + name), page.screenshot());
                } catch (IOException ignored) {}
            }
            try {
                if (page.video() != null) {
                    videoPath = page.video().path();
                }
            } catch (Exception ignored) {}
            page.close();
        }

        if (context != null) context.close(); // closing context finalizes the video file
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();

        if (videoPath != null) {
            try {
                if (failed) {
                    String name = result.getName() + "_" + System.currentTimeMillis() + ".webm";
                    Files.move(videoPath, videoPath.getParent().resolve(name),
                            StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.deleteIfExists(videoPath);
                }
            } catch (IOException ignored) {}
        }
    }
}
