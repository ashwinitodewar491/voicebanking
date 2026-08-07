package com.voicebanking.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Counts how many times a voice query got a blank/stuck-"Processing…" bot response across a
 * whole test run — an environment-stability signal distinct from ordinary pass/fail, mirroring
 * {@link SessionEndedTracker}: a no-response that the re-ask loop in BaseVoiceTest.runVoiceQuery
 * successfully recovers from (a later re-ask got a real answer) never shows up as a test failure
 * at all, but is still worth seeing. Keeps a per-occurrence detail line (which query, what time)
 * so a run with several can be traced back to exactly where they happened, not just how many.
 * Written out to disk at suite end (see TestListener#onFinish) so DashboardGenerator can read it
 * back in a separate JVM invocation and report it alongside the parsed test results. */
public final class NoResponseTracker {

    private static final AtomicInteger COUNT = new AtomicInteger(0);
    private static final List<String> DETAILS = Collections.synchronizedList(new ArrayList<>());
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final Path OUTPUT_FILE = Paths.get("target/no-response-count.txt");
    private static final Path DETAILS_FILE = Paths.get("target/no-response-details.txt");

    private NoResponseTracker() {
    }

    /** @param queryName the voice query in progress when a blank/stuck response was detected —
     *                    pass "unknown" (or similar) if the caller has no query context available. */
    public static void recordOccurrence(String queryName) {
        COUNT.incrementAndGet();
        String label = (queryName == null || queryName.isBlank()) ? "unknown query" : queryName;
        DETAILS.add(LocalDateTime.now().format(TIME_FORMAT) + " — " + label);
    }

    public static int getCount() {
        return COUNT.get();
    }

    public static List<String> getDetails() {
        return List.copyOf(DETAILS);
    }

    /** Called from TestListener#onFinish at suite end. DashboardGenerator (which runs main-side,
     * as a separate JVM, after the test run) reads these same plain-text files directly rather
     * than depending on this class, since src/main can't depend on src/test. */
    public static void writeToDisk() {
        try {
            Files.createDirectories(OUTPUT_FILE.getParent());
            Files.writeString(OUTPUT_FILE, String.valueOf(COUNT.get()));
            Files.write(DETAILS_FILE, DETAILS);
        } catch (Exception ignored) {
            // best-effort — dashboard just shows 0 / no details if this couldn't be written
        }
    }
}
