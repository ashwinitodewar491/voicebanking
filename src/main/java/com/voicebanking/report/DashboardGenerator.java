package com.voicebanking.report;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

/** Phase 1 of the static QA dashboard: parses target/surefire-reports, matches each failed test
 * to a ScreenshotUtil-captured screenshot if one exists, and writes one self-contained
 * target/dashboard-report/index.html — screenshots are embedded as base64 data URIs, not linked
 * as separate files, so the single index.html is what you'd actually share (email/Slack/etc.)
 * without a screenshots/ folder tagging along. No trends/flaky-detection/video yet. */
public class DashboardGenerator {

    public static void main(String[] args) throws Exception {
        File projectDir = new File(".").getCanonicalFile();
        File surefireDir = new File(projectDir, "target/surefire-reports");
        File screenshotsDir = new File(projectDir, "target/screenshots");
        File outputDir = new File(projectDir, "target/dashboard-report");
        File sessionEndedFile = new File(projectDir, "target/session-ended-count.txt");
        File sessionEndedDetailsFile = new File(projectDir, "target/session-ended-details.txt");
        File noResponseFile = new File(projectDir, "target/no-response-count.txt");
        File noResponseDetailsFile = new File(projectDir, "target/no-response-details.txt");
        File welcomeAbsentFile = new File(projectDir, "target/welcome-message-absent-count.txt");
        File welcomeAbsentDetailsFile = new File(projectDir, "target/welcome-message-absent-details.txt");

        if (!surefireDir.isDirectory()) {
            System.out.println("[Dashboard] No surefire-reports found at " + surefireDir + " — run tests first.");
            return;
        }

        deleteRecursively(outputDir);
        outputDir.mkdirs();

        List<TestResult> results = new SurefireReportParser().parse(surefireDir);
        results.sort(Comparator.comparing((TestResult r) -> r.className).thenComparing(r -> r.fullName));

        matchScreenshots(results, screenshotsDir);
        int sessionEndedCount = readCount(sessionEndedFile);
        List<String> sessionEndedDetails = readDetails(sessionEndedDetailsFile);
        int noResponseCount = readCount(noResponseFile);
        List<String> noResponseDetails = readDetails(noResponseDetailsFile);
        int welcomeAbsentCount = readCount(welcomeAbsentFile);
        List<String> welcomeAbsentDetails = readDetails(welcomeAbsentDetailsFile);

        String html = new HtmlRenderer().render(results, sessionEndedCount, sessionEndedDetails,
                noResponseCount, noResponseDetails, welcomeAbsentCount, welcomeAbsentDetails);
        Files.writeString(new File(outputDir, "index.html").toPath(), html);

        long total = results.size();
        long passed = results.stream().filter(r -> r.status == TestResult.Status.PASSED).count();
        long failed = results.stream().filter(r -> r.status == TestResult.Status.FAILED).count();
        long skipped = results.stream().filter(r -> r.status == TestResult.Status.SKIPPED).count();

        System.out.println("[Dashboard] Generated " + new File(outputDir, "index.html").getAbsolutePath());
        System.out.println("[Dashboard] Total: " + total + "  Passed: " + passed
                + "  Failed: " + failed + "  Skipped: " + skipped);
    }

    /** Matches each failed test to the screenshot ScreenshotUtil would have saved for it
     * (same class-name + param-label convention) and inlines it as a base64 data URI, so the
     * image travels inside index.html itself instead of needing a sibling screenshots/ folder. */
    private static void matchScreenshots(List<TestResult> results, File screenshotsDir) {
        if (!screenshotsDir.isDirectory()) return;
        File[] files = screenshotsDir.listFiles((dir, name) -> name.endsWith(".png"));
        if (files == null || files.length == 0) return;

        for (TestResult r : results) {
            if (r.status != TestResult.Status.FAILED) continue;

            String label = r.paramLabel != null ? r.paramLabel : r.methodName;
            String sanitizedLabel = label.replaceAll("[^a-zA-Z0-9_-]", "_");
            String prefix = r.simpleClassName() + "_" + sanitizedLabel + "_";

            File best = null;
            for (File f : files) {
                if (f.getName().startsWith(prefix) && (best == null || f.getName().compareTo(best.getName()) > 0)) {
                    best = f;
                }
            }
            if (best == null) continue;

            try {
                byte[] bytes = Files.readAllBytes(best.toPath());
                r.screenshotDataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
            } catch (Exception ignored) {
                // no screenshot for this row — the report just renders without one
            }
        }
    }

    /** Reads the count SessionEndedTracker/NoResponseTracker/WelcomeMessageTracker wrote out at
     * suite end (test-side; not importable here since src/main can't depend on src/test) — a plain
     * text file with just the integer. Returns 0 if it doesn't exist (e.g. no test run happened,
     * or none occurred). */
    private static int readCount(File file) {
        try {
            return Integer.parseInt(Files.readString(file.toPath()).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    /** Reads the per-occurrence detail lines SessionEndedTracker/NoResponseTracker/
     * WelcomeMessageTracker wrote out (timestamp — query name), one per line. Returns an empty
     * list if the file doesn't exist (no test run yet) or is empty (none this run). */
    private static List<String> readDetails(File file) {
        try {
            return Files.readAllLines(file.toPath());
        } catch (Exception e) {
            return List.of();
        }
    }

    private static void deleteRecursively(File file) {
        if (!file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) deleteRecursively(child);
        }
        file.delete();
    }
}
