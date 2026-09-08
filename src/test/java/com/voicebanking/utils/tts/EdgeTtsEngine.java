package com.voicebanking.utils.tts;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Cross-platform TTS using Microsoft's free, unlimited Edge neural voices via the
 * {@code edge-tts} CLI, converted to 16kHz mono PCM WAV via {@code ffmpeg}. Same commands
 * on Windows, macOS, and Linux — no OS-specific branching or native APIs.
 */
public class EdgeTtsEngine implements TtsEngine {

    public static final String VOICE = "en-US-AriaNeural";
    public static final String VOICE_HINDI = "hi-IN-SwaraNeural";
    public static final String VOICE_BENGALI = "bn-IN-TanishaaNeural";
    public static final String VOICE_MARATHI = "mr-IN-AarohiNeural";

    /** A distinct English speaker from {@link #VOICE} — used to simulate a voice that does NOT
     * match whatever was registered during voice-authentication enrollment (UI11). */
    public static final String VOICE_EN_ALTERNATE = "en-US-GuyNeural";

    /** Silence prepended before speech starts, giving the fake-audio-capture pipeline time to
     * actually start capturing before any spoken audio appears — without it, words at the very
     * start of the clip (e.g. "one" in "one rupee") can be clipped or missed by the app's STT. */
    private static final int LEAD_SILENCE_MS = 2000;

    @Override
    public String generate(String text) throws Exception {
        return generate(text, VOICE);
    }

    /** Same as {@link #generate(String)} but with an explicit edge-tts voice, for locales
     * other than the default English (e.g. {@code hi-IN-SwaraNeural} for Hindi). */
    public String generate(String text, String voice) throws Exception {
        Path txtFile = Files.createTempFile("voice_query_", ".txt");
        Path mp3File = Files.createTempFile("voice_query_", ".mp3");
        Path wavFile = Files.createTempFile("voice_query_", ".wav");
        Files.writeString(txtFile, text, StandardCharsets.UTF_8);

        try {
            // Invoked as "python -m edge_tts" rather than the bare "edge-tts" stub — pip's
            // auto-generated console-script .exe wrappers are unsigned, and Windows Smart App
            // Control (once it graduates out of evaluation mode) blocks them outright with
            // "did not meet the Enterprise signing level requirements." Going through the
            // (signed/trusted) python.exe interpreter instead runs the identical package code
            // without hitting that block.
            // Text is passed via a UTF-8 file (not --text) to avoid command-line encoding issues.
            runCommand(new String[]{
                    "python", "-m", "edge_tts", "--voice", voice,
                    "--file", txtFile.toAbsolutePath().toString(),
                    "--write-media", mp3File.toAbsolutePath().toString()
            }, "edge-tts synthesis failed for: " + text);

            runCommand(new String[]{
                    "ffmpeg", "-y", "-i", mp3File.toAbsolutePath().toString(),
                    "-af", "adelay=" + LEAD_SILENCE_MS + ":all=1",
                    "-ar", "16000", "-ac", "1", "-acodec", "pcm_s16le",
                    wavFile.toAbsolutePath().toString()
            }, "ffmpeg conversion failed for: " + text);
        } finally {
            Files.deleteIfExists(txtFile);
            Files.deleteIfExists(mp3File);
        }

        return wavFile.toAbsolutePath().toString();
    }

    /** Same as {@link #generate(String, String)} but with an explicit rate/pitch offset applied
     * on top of the given voice's own baseline — used to simulate the small take-to-take
     * variation a real speaker naturally produces (e.g. across repeated enrollment reps)
     * <b>without changing which voice/speaker is used</b>. {@code rate} and {@code pitch} use
     * edge-tts's own relative-offset syntax, e.g. {@code "+7%"} / {@code "-12Hz"}; {@code "+0%"}
     * / {@code "+0Hz"} reproduces {@link #generate(String, String)}'s behavior exactly. */
    public String generate(String text, String voice, String rate, String pitch) throws Exception {
        Path txtFile = Files.createTempFile("voice_query_", ".txt");
        Path mp3File = Files.createTempFile("voice_query_", ".mp3");
        Path wavFile = Files.createTempFile("voice_query_", ".wav");
        Files.writeString(txtFile, text, StandardCharsets.UTF_8);

        try {
            // "--rate=value" (not a separate arg) so negative offsets like "-12Hz" aren't
            // misparsed by argparse as an unrecognized option flag.
            runCommand(new String[]{
                    "python", "-m", "edge_tts", "--voice", voice,
                    "--rate=" + rate, "--pitch=" + pitch,
                    "--file", txtFile.toAbsolutePath().toString(),
                    "--write-media", mp3File.toAbsolutePath().toString()
            }, "edge-tts synthesis failed for: " + text);

            runCommand(new String[]{
                    "ffmpeg", "-y", "-i", mp3File.toAbsolutePath().toString(),
                    "-af", "adelay=" + LEAD_SILENCE_MS + ":all=1",
                    "-ar", "16000", "-ac", "1", "-acodec", "pcm_s16le",
                    wavFile.toAbsolutePath().toString()
            }, "ffmpeg conversion failed for: " + text);
        } finally {
            Files.deleteIfExists(txtFile);
            Files.deleteIfExists(mp3File);
        }

        return wavFile.toAbsolutePath().toString();
    }

    private static void runCommand(String[] cmd, String errorPrefix) throws Exception {
        Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) {
            throw new RuntimeException(errorPrefix + "\n" + output);
        }
    }
}
