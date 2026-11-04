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

/** Run-wide count of missing welcome messages after a locale switch, written to disk at suite end
 * for the dashboard. */
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

    /** Called at suite end. DashboardGenerator reads the files directly since src/main can't depend
     * on src/test. */
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
