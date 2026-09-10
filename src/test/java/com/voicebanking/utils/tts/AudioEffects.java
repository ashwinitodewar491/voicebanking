package com.voicebanking.utils.tts;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Post-processes an already-synthesized WAV to simulate acoustic differences that have nothing
 * to do with what TTS engine/voice produced it — a different recording channel, device, or
 * delivery style for the <b>same</b> speaker saying the <b>same</b> words. Deliberately separate
 * from {@link EdgeTtsEngine}/{@link VoiceVariation} (which vary the synthesis itself via rate/
 * pitch): these filters operate on the finished audio, the way a real phone call, a cheap mic, or
 * someone talking faster would change a recording after the fact. Each method takes an existing
 * WAV path and returns a new temp WAV path in the same 16kHz mono PCM format the fake-audio-
 * capture pipeline expects; the input file is left untouched (caller deletes it if it's temporary).
 */
public class AudioEffects {

    private AudioEffects() {
    }

    /** Band-limits to the classic analog telephone band (300Hz-3400Hz) — simulates verifying over
     * a phone call instead of a full-bandwidth microphone recording. */
    public static String applyTelephoneBandpass(String wavPath) throws Exception {
        return runFilter(wavPath, "highpass=f=300,lowpass=f=3400");
    }

    /** Pure playback-speed change via ffmpeg's own time-stretch, decoupled from edge-tts's own
     * {@code --rate} (which can alter word-level prosody/timing together rather than just
     * stretching/compressing the whole signal). {@code factor} must be in ffmpeg's single-pass
     * {@code atempo} range {@code [0.5, 2.0]}; e.g. {@code 1.15} for 15% faster. */
    public static String applyTempo(String wavPath, double factor) throws Exception {
        if (factor < 0.5 || factor > 2.0) {
            throw new IllegalArgumentException("atempo factor must be in [0.5, 2.0], got " + factor);
        }
        return runFilter(wavPath, "atempo=" + factor);
    }

    /** Linear volume multiplier — {@code 1.0} is unchanged, {@code 0.6} noticeably quieter,
     * {@code 1.4} noticeably louder. Simulates natural loudness variation (a whispered vs.
     * projected version of the same words) rather than a different speaker. */
    public static String applyVolume(String wavPath, double factor) throws Exception {
        return runFilter(wavPath, "volume=" + factor);
    }

    /** Shelf-EQ tone shift — {@code bassGainDb}/{@code trebleGainDb} positive boosts, negative
     * cuts. E.g. {@code (+6, -6)} simulates a duller/muffled voice (weak mic placement, speaking
     * away from the device); {@code (-6, +6)} simulates a tinnier, brighter recording. */
    public static String applyToneShift(String wavPath, int bassGainDb, int trebleGainDb) throws Exception {
        return runFilter(wavPath, "bass=g=" + bassGainDb + ",treble=g=" + trebleGainDb);
    }

    /** Mixes in mild pink noise (a generated {@code anoisesrc}, not a recorded sample) under the
     * speech — simulates a real room with some ambient hum/hiss rather than a dead-silent studio
     * recording. {@code amplitude} is pink noise's own linear level (0.0-1.0); {@code 0.03} reads
     * as a subtle, constant background hiss under clearly-intelligible speech, not overpowering
     * noise. Unlike the single-input filters above this needs a second (generated) input and an
     * explicit mix, so it doesn't go through {@link #runFilter}. */
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
