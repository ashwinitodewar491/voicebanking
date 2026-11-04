package com.voicebanking.utils.tts;

import java.util.concurrent.ThreadLocalRandom;

/** Small random rate/pitch offsets: the same voice, a slightly different take. */
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

    /** A random rate offset in {@code [-rangePercent, +rangePercent]}. */
    public static String randomRate(int rangePercent) {
        return signed(ThreadLocalRandom.current().nextInt(-rangePercent, rangePercent + 1)) + "%";
    }

    /** A random pitch offset in {@code [-rangeHz, +rangeHz]}. */
    public static String randomPitch(int rangeHz) {
        return signed(ThreadLocalRandom.current().nextInt(-rangeHz, rangeHz + 1)) + "Hz";
    }

    private static String signed(int value) {
        return value >= 0 ? "+" + value : String.valueOf(value);
    }
}
