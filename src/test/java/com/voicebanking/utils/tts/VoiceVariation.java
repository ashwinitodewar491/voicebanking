package com.voicebanking.utils.tts;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates small randomized rate/pitch offsets that mimic how a real speaker's repeated takes
 * naturally differ from one another, without changing which TTS voice (i.e. which speaker) is
 * used — same person, different take. Bounds are deliberately subtle: wide enough to be a
 * genuinely different take, narrow enough that a human listener would still call it the same
 * person speaking (verified by ear against {@code en-US-AriaNeural} before picking these ranges).
 */
public class VoiceVariation {

    private static final int RATE_RANGE_PERCENT = 10;
    private static final int PITCH_RANGE_HZ = 20;

    private VoiceVariation() {
    }

    /** A random rate offset in {@code [-10%, +10%]}, e.g. {@code "+4%"}. */
    public static String randomRate() {
        return randomRate(RATE_RANGE_PERCENT);
    }

    /** A random pitch offset in {@code [-20Hz, +20Hz]}, e.g. {@code "-13Hz"}. */
    public static String randomPitch() {
        return randomPitch(PITCH_RANGE_HZ);
    }

    /** A random rate offset in {@code [-rangePercent, +rangePercent]} — for a caller that wants
     * a narrower or wider band than the default {@link #randomRate()}. */
    public static String randomRate(int rangePercent) {
        return signed(ThreadLocalRandom.current().nextInt(-rangePercent, rangePercent + 1)) + "%";
    }

    /** A random pitch offset in {@code [-rangeHz, +rangeHz]} — for a caller that wants a
     * narrower or wider band than the default {@link #randomPitch()}. */
    public static String randomPitch(int rangeHz) {
        return signed(ThreadLocalRandom.current().nextInt(-rangeHz, rangeHz + 1)) + "Hz";
    }

    private static String signed(int value) {
        return value >= 0 ? "+" + value : String.valueOf(value);
    }
}
