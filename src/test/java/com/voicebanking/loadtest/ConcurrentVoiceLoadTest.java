package com.voicebanking.loadtest;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.voicebanking.DataText.BotResponsePatterns;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.DataText.VoiceQueries;
import com.voicebanking.pages.HomePage;
import com.voicebanking.pages.LanguagePage;
import com.voicebanking.pages.OtpPage;
import com.voicebanking.pages.VoiceRegistrationPage;
import com.voicebanking.pages.WelcomePage;
import com.voicebanking.utils.TtsUtil;
import com.voicebanking.utils.tts.EdgeTtsEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Frontend concurrent-user load test — NOT an API load test. Each "virtual user" here is a
 * genuinely separate Playwright browser instance driving the real UI end to end: login, OTP,
 * voice-print registration (if not already registered), then N repeated spoken balance queries
 * through the actual mic-capture / STT / bot-response pipeline (the same fake-audio-capture
 * Chromium flags {@link com.voicebanking.tests.ui.UI11_VoiceRegistrationAuthTest} and friends
 * already use). Run N of these in parallel to see how the app behaves — response latency, error
 * rate — under real concurrent frontend traffic, as opposed to k6/Gatling/JMeter style protocol
 * load testing, which never touches the browser/voice pipeline at all.
 *
 * <p><b>Critical limitation — READ BEFORE RAISING -Dvus:</b> each virtual user logs in as a real,
 * distinct customer phone number. This app appears to be single-session-per-account (see how
 * {@code registerVoiceAndReachHome} in the UI test suites always clears cookies/storage before a
 * second login as a different account in the same browser). Running more concurrent virtual users
 * than distinct phone numbers in the pool means two threads will log into the SAME account at the
 * same time — one login can invalidate the other's session mid-run, producing failures that look
 * like a concurrency bug in the app but are actually just a test-data shortage. {@link
 * #KNOWN_TEST_ACCOUNTS} currently holds 44 provisioned numbers, so that's today's ceiling for a
 * collision-free run; pass {@code -Dphones=} with your own list to use a different/larger pool.
 *
 * <p>Usage (from the project root):
 * <pre>
 *   mvn test-compile exec:java -Dexec.classpathScope=test \
 *       -Dexec.mainClass=com.voicebanking.loadtest.ConcurrentVoiceLoadTest \
 *       -Dvus=10 -Diterations=5 -Denv=stage -Dheadless=true
 *
 *   # up to the full 44-account pool
 *   mvn test-compile exec:java -Dexec.classpathScope=test \
 *       -Dexec.mainClass=com.voicebanking.loadtest.ConcurrentVoiceLoadTest \
 *       -Dvus=44 -Diterations=5
 *
 *   # with your own pool of provisioned test accounts (comma-separated phone numbers)
 *   mvn test-compile exec:java -Dexec.classpathScope=test \
 *       -Dexec.mainClass=com.voicebanking.loadtest.ConcurrentVoiceLoadTest \
 *       -Dvus=10 -Diterations=5 -Dphones=9812341042,9812341041,9898989898,...
 * </pre>
 */
public final class ConcurrentVoiceLoadTest {

    private static final int ENROLLMENT_REPS = 3;
    private static final int MAX_TAKES_PER_REP = 3;
    private static final int MAX_REASK_ATTEMPTS = 3;

    private static final Pattern GENERIC_GREETING = Pattern.compile("Welcome.*How can I help you today");
    private static final Pattern CONTEXT_LOST_FALLBACK =
            Pattern.compile("(?i)didn.t understand that.*what would you like to do");

    /** The full pool of distinct, provisioned test accounts available for concurrency testing —
     * 44 numbers, so up to 44 virtual users can run without two threads sharing a phone number
     * (see this class's javadoc on why sharing one causes session-collision false failures).
     * Names are kept in the comment purely for traceability back to whoever provided this list;
     * only the phone number is used at runtime. Overridable per-run with {@code -Dphones=}. */
    private static final List<String> KNOWN_TEST_ACCOUNTS = List.of(
            "9876543210", // Amit Sharma
            "9123456780", // Priya Singh
            "9988776655", // Rahul Verma
            "9811122233", // Neha Gupta
            "9898989898", // Rohit Mehta
            "9445566778", // Ananya Iyer
            "9723456789", // Suresh Patel
            "9632587410", // Pooja Nair
            "9911223344", // Karan Malhotra
            "9765432109", // Sneha Kulkarni
            "9001100011", // Arjun Reddy
            "9001100012", // Meera Joshi
            "9001100013", // Vikram Desai
            "9001100014", // Ishita Bansal
            "9001100015", // Nitin Chawla
            "9001100016", // Kavya Menon
            "9001100017", // Aditya Rao
            "9001100018", // Ritika Sethi
            "9001100019", // Manish Yadav
            "9001100020", // Shreya Kapoor
            "9001100021", // Harsh Vardhan
            "9001100022", // Nandini Pillai
            "9001100023", // Siddharth Jain
            "9001100024", // Aarti Mishra
            "9001100025", // Pranav Kulshreshtha
            "9001100026", // Divya Narayanan
            "9001100027", // Yashwant Tripathi
            "9001100028", // Bhavna Tiwari
            "9001100029", // Raghav Khurana
            "9001100030", // Tanvi Chatterjee
            "9812341041", // Aniket More
            "9812341042", // Leena Kamat
            "9881395656", // Gautam Rege
            "7974933860", // Anshumant Dhawan
            "8459875361", // Vinayak Behere
            "9172304337", // Apurva Rawal
            "7972007624", // Pallavi Patil
            "9850434267", // Sethupathi Ashokan
            "7499978110", // Ashiya Ajare
            "9322616376", // Sobiya Shaikh
            "8788140465", // Yash Shah
            "7218000603", // Ashwini Todewar
            "9090909090", // test_user 1
            "9909098880"  // test_user 2
    );

    private ConcurrentVoiceLoadTest() {
    }

    public static void main(String[] args) throws Exception {
        int vus = Integer.getInteger("vus", 3);
        int iterations = Integer.getInteger("iterations", 5);
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));

        List<String> phonePool = new ArrayList<>(
                System.getProperty("phones") != null
                        ? List.of(System.getProperty("phones").split(","))
                        : KNOWN_TEST_ACCOUNTS);
        List<String> voicePool = List.of(
                EdgeTtsEngine.VOICE, EdgeTtsEngine.VOICE_EN_ALTERNATE);

        if (vus > phonePool.size()) {
            System.out.println("[LoadTest] WARNING: " + vus + " virtual users requested but only "
                    + phonePool.size() + " distinct phone numbers are available (" + phonePool
                    + "). Some threads WILL share an account and can invalidate each other's "
                    + "session mid-run — see this class's javadoc. Pass -Dphones=<comma list> "
                    + "with more provisioned test accounts to raise this safely.");
        }

        String runTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        long runStart = System.currentTimeMillis();

        System.out.println("[LoadTest] Starting " + vus + " concurrent virtual users, "
                + iterations + " balance-query iterations each, against " + Endpoints.getUiBaseUrl());

        ExecutorService pool = Executors.newFixedThreadPool(vus);
        List<Future<VirtualUserResult>> futures = new ArrayList<>();

        for (int i = 0; i < vus; i++) {
            int userId = i + 1;
            String phone = phonePool.get(i % phonePool.size());
            String voice = voicePool.get(i % voicePool.size());
            futures.add(pool.submit(() -> runVirtualUser(userId, phone, voice, iterations, headless)));
        }

        List<VirtualUserResult> results = new ArrayList<>();
        for (Future<VirtualUserResult> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                System.out.println("[LoadTest] A virtual user thread crashed entirely: " + e.getMessage());
            }
        }

        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);

        long totalRunMs = System.currentTimeMillis() - runStart;
        printSummary(results);
        writeReport(results, runTimestamp, vus, iterations, totalRunMs);
    }

    private static VirtualUserResult runVirtualUser(
            int userId, String phone, String voice, int iterations, boolean headless) {
        String tag = "[VU" + userId + ":" + phone + "]";
        List<IterationResult> iterationResults = new ArrayList<>();

        String generatedWavPath = null;
        Playwright playwright = null;
        Browser browser = null;
        Page page = null;

        try {
            generatedWavPath = TtsUtil.generateWav(VoiceQueries.English.VOICE_ENROLLMENT_PHRASE, voice);

            List<String> chromeArgs = new ArrayList<>();
            chromeArgs.add("--disable-gpu");
            chromeArgs.add("--use-fake-device-for-media-stream");
            chromeArgs.add("--use-fake-ui-for-media-stream");
            chromeArgs.add("--use-file-for-fake-audio-capture=" + generatedWavPath);

            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(headless)
                    .setArgs(chromeArgs));
            var context = browser.newContext(new Browser.NewContextOptions()
                    .setPermissions(List.of("microphone")));
            page = context.newPage();

            page.addInitScript(
                    "(function() {"
                    + "  window.__micStreams = [];"
                    + "  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) return;"
                    + "  var original = navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices);"
                    + "  navigator.mediaDevices.getUserMedia = function(constraints) {"
                    + "    return original(constraints).then(function(stream) {"
                    + "      window.__micStreams.push(stream);"
                    + "      return stream;"
                    + "    });"
                    + "  };"
                    + "})();");
            page.addInitScript(
                    "(function() {"
                    + "  window.AnalyserNode.prototype.getByteFrequencyData = function(array) {"
                    + "    for (var i = 0; i < array.length; i++) { array[i] = 255; }"
                    + "  };"
                    + "})();");

            long loginStart = System.currentTimeMillis();
            HomePage homePage = loginAndEnsureVoiceRegistered(page, phone, voice, generatedWavPath, tag);
            System.out.println(tag + " ready (login + voice registration) in "
                    + (System.currentTimeMillis() - loginStart) + " ms");

            for (int i = 1; i <= iterations; i++) {
                long start = System.currentTimeMillis();
                try {
                    String botResponse = askBalanceWithVoice(homePage, voice, generatedWavPath, tag);
                    long elapsedMs = System.currentTimeMillis() - start;
                    boolean success = Pattern.compile(BotResponsePatterns.Balance.ANY)
                            .matcher(botResponse).find();
                    iterationResults.add(new IterationResult(userId, phone, i, elapsedMs, success, botResponse));
                    System.out.println(tag + " iter " + i + "/" + iterations + " -> " + elapsedMs
                            + " ms, success=" + success + ", response=\"" + botResponse + "\"");
                } catch (Exception e) {
                    long elapsedMs = System.currentTimeMillis() - start;
                    iterationResults.add(new IterationResult(
                            userId, phone, i, elapsedMs, false, "ERROR: " + e.getMessage()));
                    System.out.println(tag + " iter " + i + "/" + iterations + " FAILED: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println(tag + " setup/login FAILED — no iterations ran: " + e.getMessage());
            for (int i = 1; i <= iterations; i++) {
                iterationResults.add(new IterationResult(
                        userId, phone, i, 0, false, "SETUP_FAILED: " + e.getMessage()));
            }
        } finally {
            try {
                if (page != null) {
                    HomePage cleanupHome = new HomePage(page);
                    if (cleanupHome.isVoiceRegistered()) {
                        cleanupHome.removeRegisteredVoice();
                    }
                }
            } catch (Exception cleanupError) {
                System.out.println(tag + " WARN — voiceprint cleanup failed: " + cleanupError.getMessage());
            }
            if (browser != null) {
                browser.close();
            }
            if (playwright != null) {
                playwright.close();
            }
            if (generatedWavPath != null) {
                TtsUtil.deleteWav(generatedWavPath);
            }
        }

        return new VirtualUserResult(userId, phone, iterationResults);
    }

    /** Logs into {@code phone} and completes voice registration if not already registered — same
     * flow as {@code registerVoiceAndReachHome} in UI11/UI14/UI15, condensed to a static helper
     * since this class has no shared test-lifecycle state to hang it off of. */
    private static HomePage loginAndEnsureVoiceRegistered(
            Page page, String phone, String voice, String generatedWavPath, String tag) throws Exception {
        WelcomePage welcomePage = new WelcomePage(page, Endpoints.getUiBaseUrl());
        welcomePage.navigate();
        welcomePage.dismissPwaPopupIfPresent();
        welcomePage.enterPhoneNumber(phone);
        welcomePage.clickSendOtp();

        OtpPage otpPage = new OtpPage(page);
        otpPage.waitForPageLoad();
        otpPage.enterOtp(OtpPage.getTestOtp());
        otpPage.clickContinue();

        LanguagePage languagePage = new LanguagePage(page);
        try {
            languagePage.waitForPageLoad();
            languagePage.selectEnglish();
            languagePage.clickContinue();
        } catch (PlaywrightException ignored) {
            // language page absent for returning users
        }

        VoiceRegistrationPage voicePage = new VoiceRegistrationPage(page);
        try {
            voicePage.waitForPageLoad();
        } catch (PlaywrightException notAutoPrompted) {
            HomePage homePage = new HomePage(page);
            homePage.waitForPageLoad();
            if (homePage.isVoiceRegistered()) {
                return homePage;
            }
            System.out.println(tag + " registration screen didn't auto-appear — forcing via user menu.");
            homePage.clickRegisterVoiceFromMenu();
            voicePage.waitForPageLoad();
        }

        String enrollWavPath = TtsUtil.generateWav(VoiceQueries.English.VOICE_ENROLLMENT_PHRASE, voice);
        Files.copy(Path.of(enrollWavPath), Path.of(generatedWavPath), StandardCopyOption.REPLACE_EXISTING);
        TtsUtil.deleteWav(enrollWavPath);

        voicePage.checkConsent();
        voicePage.clickStartRegistration();

        for (int rep = 1; rep <= ENROLLMENT_REPS; rep++) {
            recordAcceptedTake(voicePage, rep, tag);
            voicePage.clickSubmit();
        }
        voicePage.clickStartBanking();

        HomePage homePage = new HomePage(page);
        homePage.waitForPageLoad();
        return homePage;
    }

    private static void recordAcceptedTake(VoiceRegistrationPage voicePage, int rep, String tag) throws Exception {
        for (int take = 1; take <= MAX_TAKES_PER_REP; take++) {
            voicePage.tapMicAndRecord();
            if (voicePage.waitForRecordingAccepted(5000)) {
                return;
            }
            System.out.println(tag + " enrollment rep " + rep + " take " + take + " not accepted — retrying...");
            voicePage.clickRerecord();
        }
        throw new RuntimeException("Enrollment rep " + rep + " rejected " + MAX_TAKES_PER_REP + " times");
    }

    /** Same query/re-ask flow as {@code askBalanceWithVoice} in UI11/UI14 — a reconnect's
     * session-start greeting or context-lost fallback can otherwise swallow the query. */
    private static String askBalanceWithVoice(
            HomePage homePage, String voice, String generatedWavPath, String tag) throws Exception {
        String queryWavPath = TtsUtil.generateWav(VoiceQueries.English.ACCOUNT_BALANCE, voice);
        Files.copy(Path.of(queryWavPath), Path.of(generatedWavPath), StandardCopyOption.REPLACE_EXISTING);
        int holdMs = (int) TtsUtil.getWavDurationMs(generatedWavPath);
        TtsUtil.deleteWav(queryWavPath);

        homePage.reacquireMicrophoneForFollowUp();
        homePage.holdToSpeakWithRetry(holdMs, 3, 8000);
        homePage.waitForVoiceResponse(15000);

        String botResponse = homePage.getLastBotResponse();

        for (int reaskNum = 1;
             reaskNum <= MAX_REASK_ATTEMPTS
                     && (GENERIC_GREETING.matcher(botResponse).find()
                        || CONTEXT_LOST_FALLBACK.matcher(botResponse).find()
                        || botResponse.isBlank());
             reaskNum++) {
            System.out.println(tag + " got greeting/fallback/empty response — re-asking ("
                    + reaskNum + "/" + MAX_REASK_ATTEMPTS + ")...");
            homePage.reacquireMicrophoneForFollowUp();
            homePage.holdToSpeakWithRetry(holdMs, 3, 8000);
            homePage.waitForVoiceResponse(15000);
            botResponse = homePage.getLastBotResponse();
        }

        return botResponse;
    }

    private static void printSummary(List<VirtualUserResult> results) {
        List<Long> successLatencies = new ArrayList<>();
        int totalFailures = 0;
        for (VirtualUserResult result : results) {
            for (IterationResult iter : result.iterations()) {
                if (iter.success()) {
                    successLatencies.add(iter.latencyMs());
                } else {
                    totalFailures++;
                }
            }
        }
        Collections.sort(successLatencies);

        System.out.println("\n================ FRONTEND CONCURRENCY LOAD TEST SUMMARY ================");
        System.out.println("Virtual users        : " + results.size());
        System.out.println("Successful queries   : " + successLatencies.size());
        System.out.println("Failed queries       : " + totalFailures);
        if (!successLatencies.isEmpty()) {
            double avg = successLatencies.stream().mapToLong(Long::longValue).average().orElse(0);
            System.out.println("Min latency (ms)     : " + successLatencies.get(0));
            System.out.println("Avg latency (ms)     : " + Math.round(avg));
            System.out.println("p50 latency (ms)     : " + percentile(successLatencies, 50));
            System.out.println("p95 latency (ms)     : " + percentile(successLatencies, 95));
            System.out.println("Max latency (ms)     : " + successLatencies.get(successLatencies.size() - 1));
        }
        for (VirtualUserResult result : results) {
            long ok = result.iterations().stream().filter(IterationResult::success).count();
            long failed = result.iterations().size() - ok;
            System.out.println("  " + result.phone() + " (VU" + result.userId() + "): " + ok + " ok, " + failed + " failed");
        }
        System.out.println("==========================================================================");
    }

    private static long percentile(List<Long> sortedLatencies, int pct) {
        int index = (int) Math.ceil(pct / 100.0 * sortedLatencies.size()) - 1;
        return sortedLatencies.get(Math.max(0, Math.min(index, sortedLatencies.size() - 1)));
    }

    /** Persists this run's results to {@code target/loadtest-reports/<timestamp>/} so they
     * outlive the console — a per-iteration CSV for raw data (spreadsheet/diffing across runs)
     * plus a plain-text summary mirroring what {@link #printSummary} already printed. Unlike the
     * TestNG functional suites, this harness never runs through the {@code test} Surefire phase
     * (it's launched via {@code exec:java}), so nothing else writes a report for it. */
    private static void writeReport(
            List<VirtualUserResult> results, String runTimestamp, int vus, int iterations, long totalRunMs)
            throws Exception {
        Path reportDir = Paths.get("target", "loadtest-reports", runTimestamp);
        Files.createDirectories(reportDir);

        List<IterationResult> allIterations = new ArrayList<>();
        for (VirtualUserResult result : results) {
            allIterations.addAll(result.iterations());
        }

        Path csvPath = reportDir.resolve("iterations.csv");
        StringBuilder csv = new StringBuilder("userId,phone,iteration,latencyMs,success,botResponse\n");
        for (IterationResult iter : allIterations) {
            csv.append(iter.userId()).append(',')
                    .append(iter.phone()).append(',')
                    .append(iter.iteration()).append(',')
                    .append(iter.latencyMs()).append(',')
                    .append(iter.success()).append(',')
                    .append(csvEscape(iter.botResponse())).append('\n');
        }
        Files.writeString(csvPath, csv.toString());

        List<Long> successLatencies = new ArrayList<>();
        int totalFailures = 0;
        for (IterationResult iter : allIterations) {
            if (iter.success()) {
                successLatencies.add(iter.latencyMs());
            } else {
                totalFailures++;
            }
        }
        Collections.sort(successLatencies);

        StringBuilder summary = new StringBuilder();
        summary.append("Frontend Concurrency Load Test — ").append(runTimestamp).append('\n');
        summary.append("Target               : ").append(Endpoints.getUiBaseUrl()).append('\n');
        summary.append("Virtual users         : ").append(vus).append('\n');
        summary.append("Iterations per user   : ").append(iterations).append('\n');
        summary.append("Total wall-clock time : ").append(totalRunMs).append(" ms\n");
        summary.append("Successful queries    : ").append(successLatencies.size()).append('\n');
        summary.append("Failed queries        : ").append(totalFailures).append('\n');
        if (!successLatencies.isEmpty()) {
            double avg = successLatencies.stream().mapToLong(Long::longValue).average().orElse(0);
            summary.append("Min latency (ms)      : ").append(successLatencies.get(0)).append('\n');
            summary.append("Avg latency (ms)      : ").append(Math.round(avg)).append('\n');
            summary.append("p50 latency (ms)      : ").append(percentile(successLatencies, 50)).append('\n');
            summary.append("p95 latency (ms)      : ").append(percentile(successLatencies, 95)).append('\n');
            summary.append("Max latency (ms)      : ").append(successLatencies.get(successLatencies.size() - 1)).append('\n');
        }
        summary.append('\n').append("Per-user breakdown:\n");
        for (VirtualUserResult result : results) {
            long ok = result.iterations().stream().filter(IterationResult::success).count();
            long failed = result.iterations().size() - ok;
            summary.append("  ").append(result.phone()).append(" (VU").append(result.userId()).append("): ")
                    .append(ok).append(" ok, ").append(failed).append(" failed\n");
        }

        Path summaryPath = reportDir.resolve("summary.txt");
        Files.writeString(summaryPath, summary.toString());

        System.out.println("[LoadTest] Report written to: " + reportDir.toAbsolutePath());
        System.out.println("[LoadTest]   - " + summaryPath.getFileName());
        System.out.println("[LoadTest]   - " + csvPath.getFileName());
    }

    private static String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private record IterationResult(
            int userId, String phone, int iteration, long latencyMs, boolean success, String botResponse) {
    }

    private record VirtualUserResult(int userId, String phone, List<IterationResult> iterations) {
    }
}
