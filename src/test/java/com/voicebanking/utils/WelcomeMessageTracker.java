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

/** Counts how many times the welcome/bot greeting was absent from the chat right after a locale
 * switch, across a whole test run — mirrors {@link SessionEndedTracker} and
 * {@link NoResponseTracker}: this is an observation, not an assertion (see
 * UI12_MultilingualVoiceQueryTest#checkWelcomeMessage), so it never fails a test and would
 * otherwise only ever show up in console output. Keeps a per-occurrence detail line (which query,
 * what time) so a run with several can be traced back to exactly where they happened, not just
 * how many. Written out to disk at suite end (see TestListener#onFinish) so DashboardGenerator can
 * read it back in a separate JVM invocation and report it alongside the parsed test results. */
public final class WelcomeMessageTracker {

    private static final AtomicInteger COUNT = new AtomicInteger(0);
    private static final List<String> DETAILS = Collections.synchronizedList(new ArrayList<>());
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final Path OUTPUT_FILE = Paths.get("target/welcome-message-absent-count.txt");
    private static final Path DETAILS_FILE = Paths.get("target/welcome-message-absent-details.txt");

    private WelcomeMessageTracker() {
    }

    /** @param queryName the voice query whose landing point was being checked when the welcome
     *                    message was found absent — pass "unknown" (or similar) if the caller has
     *                    no query context available.
     * @param context     the landing point, e.g. "post-switch" — see
     *                    UI12_MultilingualVoiceQueryTest#checkWelcomeMessage. */
    public static void recordOccurrence(String queryName, String context) {
        COUNT.incrementAndGet();
        String label = (queryName == null || queryName.isBlank()) ? "unknown query" : queryName;
        DETAILS.add(LocalDateTime.now().format(TIME_FORMAT) + " — " + label + " [" + context + "]");
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
