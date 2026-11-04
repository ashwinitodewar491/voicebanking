package com.voicebanking.utils.tts;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Post-processes a WAV to simulate channel, device or delivery differences for the same speaker.
 * Each method returns a new 16kHz mono temp WAV and leaves the input untouched. */
public class AudioEffects {

    private AudioEffects() {
    }

    /** Band-limits to the classic analog telephone band (300Hz-3400Hz) — simulates verifying over
     * a phone call instead of a full-bandwidth microphone recording. */
    public static String applyTelephoneBandpass(String wavPath) throws Exception {
        return runFilter(wavPath, "highpass=f=300,lowpass=f=3400");
    }

    /** Pure tempo change via ffmpeg atempo; {@code factor} must be within [0.5, 2.0]. */
    public static String applyTempo(String wavPath, double factor) throws Exception {
        if (factor < 0.5 || factor > 2.0) {
            throw new IllegalArgumentException("atempo factor must be in [0.5, 2.0], got " + factor);
        }
        return runFilter(wavPath, "atempo=" + factor);
    }

    /** Linear volume multiplier (1.0 = unchanged). */
    public static String applyVolume(String wavPath, double factor) throws Exception {
        return runFilter(wavPath, "volume=" + factor);
    }

    /** Shelf-EQ shift: positive gains boost, negative gains cut. */
    public static String applyToneShift(String wavPath, int bassGainDb, int trebleGainDb) throws Exception {
        return runFilter(wavPath, "bass=g=" + bassGainDb + ",treble=g=" + trebleGainDb);
    }

    /** Mixes pink noise at {@code amplitude} (0.0-1.0) under the speech. */
    public static String applyBackgroundNoise(String wavPath, double amplitude) throws Exception {
        Path outFile = Files.createTempFile("voice_effect_", ".wav");

        Process process = new ProcessBuilder(
                "ffmpeg", "-y",
                "-i", wavPath,
                "-f", "lavfi", "-i", "anoisesrc=color=pink:amplitude=" + amplitude + ":sample_rate=16000",
                "-filter_complex", "[0:a][1:a]amix=inputs=2:duration=first:dropout_transition=0[out]",
                "-map", "[out]",
                "-ar", "16000", "-ac", "1", "-acodec", "pcm_s16le",
                outFile.toAbsolutePath().toString()
        ).redirectErrorStream(true).start();

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) {
            throw new RuntimeException("ffmpeg background-noise mix failed for: " + wavPath + "\n" + output);
        }

        return outFile.toAbsolutePath().toString();
    }

    private static String runFilter(String wavPath, String audioFilter) throws Exception {
        Path outFile = Files.createTempFile("voice_effect_", ".wav");

        Process process = new ProcessBuilder(
                "ffmpeg", "-y", "-i", wavPath,
                "-af", audioFilter,
                "-ar", "16000", "-ac", "1", "-acodec", "pcm_s16le",
                outFile.toAbsolutePath().toString()
        ).redirectErrorStream(true).start();

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) {
            throw new RuntimeException("ffmpeg filter '" + audioFilter + "' failed for: " + wavPath
                    + "\n" + output);
        }

        return outFile.toAbsolutePath().toString();
    }
}
