package com.voicebanking.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

/** Counts how many times the app's "Session Ended" state was detected mid-voice-query across a
 * whole test run — an environment-stability signal distinct from ordinary pass/fail, since a
 * session drop that gets successfully recovered from never shows up as a test failure at all.
 * Written out to disk at suite end (see TestListener#onFinish) so DashboardGenerator can read it
 * back in a separate JVM invocation and report it alongside the parsed test results. */
public final class SessionEndedTracker {

    private static final AtomicInteger COUNT = new AtomicInteger(0);
    private static final Path OUTPUT_FILE = Paths.get("target/session-ended-count.txt");

    private SessionEndedTracker() {
    }

    public static void recordOccurrence() {
        COUNT.incrementAndGet();
    }

    public static int getCount() {
        return COUNT.get();
    }

    /** Called from TestListener#onFinish at suite end. DashboardGenerator (which runs main-side,
     * as a separate JVM, after the test run) reads this same plain-text file directly rather than
     * depending on this class, since src/main can't depend on src/test. */
    public static void writeToDisk() {
        try {
            Files.createDirectories(OUTPUT_FILE.getParent());
            Files.writeString(OUTPUT_FILE, String.valueOf(COUNT.get()));
        } catch (Exception ignored) {
            // best-effort — dashboard just shows 0 if this couldn't be written
        }
    }
}
