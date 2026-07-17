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

    private static final String VOICE = "en-US-AriaNeural";

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
            // Text is passed via a UTF-8 file (not --text) to avoid command-line encoding issues.
            runCommand(new String[]{
                    "edge-tts", "--voice", voice,
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
