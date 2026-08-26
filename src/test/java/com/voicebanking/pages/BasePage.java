package com.voicebanking.pages;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.voicebanking.listeners.TestListener;
import com.voicebanking.utils.ScreenshotUtil;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** {@code @Listeners} registers {@link TestListener} directly on the class, not just via
 * testng.xml's suite-level {@code <listener>} tag — that tag is silently skipped by Maven
 * Surefire whenever a run is scoped with {@code -Dtest=SomeClass}, since that flag makes Surefire
 * build its own ad-hoc suite instead of using testng.xml at all. Without this, every non-voice UI
 * test (UI1-6, UI11) would never write to ExtentReports or any of the SessionEndedTracker /
 * NoResponseTracker / WelcomeMessageTracker dashboard files when run standalone — see
 * {@link com.voicebanking.tests.ui.base.BaseVoiceTest} for the original diagnosis of this gap. */
@Listeners(TestListener.class)
public class BasePage {

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;

    /**
     * Returns a JS init script that replaces webkitSpeechRecognition with a mock.
     * The mock fires onresult with the given transcript ~500 ms after start() is called.
     * Use page.addInitScript(speechMockScript(...)) before page.navigate() when headless.
     */
    public static String speechMockScript(String transcript) {
        String safe = transcript.replace("\\", "\\\\").replace("'", "\\'");
        return "(function(){" +
               "console.log('[MOCK] installing speech mock for: " + safe + "');" +
               "if(navigator.mediaDevices){" +
               "  navigator.mediaDevices.getUserMedia=function(){" +
               "    console.log('[MOCK] getUserMedia intercepted — returning fake stream');" +
               "    return Promise.resolve(new MediaStream());" +
               "  };" +
               "}" +
               "var T='" + safe + "';" +
               "function M(){this.continuous=false;this.interimResults=false;" +
               "this.lang='en-US';this.onstart=null;this.onresult=null;" +
               "this.onend=null;this.onerror=null;}" +
               "M.prototype.start=function(){" +
               "  console.log('[MOCK] recognition.start() called');" +
               "  var s=this;" +
               "  setTimeout(function(){if(s.onstart)s.onstart({});},50);" +
               "  setTimeout(function(){" +
               "    console.log('[MOCK] firing onresult: '+T);" +
               "    if(s.onresult){var a={transcript:T,confidence:0.95};" +
               "    var r=[a];r.isFinal=true;var rs=[r];rs.length=1;" +
               "    s.onresult({results:rs,resultIndex:0});}" +
               "    else{console.log('[MOCK] onresult is null — app never set it');}" +
               "    setTimeout(function(){if(s.onend)s.onend({});},50);" +
               "  },300);" +
               "};" +
               "M.prototype.stop=function(){};" +
               "M.prototype.abort=function(){};" +
               "window.SpeechRecognition=M;" +
               "window.webkitSpeechRecognition=M;" +
               "console.log('[MOCK] webkitSpeechRecognition replaced');" +
               "})();";
    }

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
            ScreenshotUtil.captureOnFailure(page, result);
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
