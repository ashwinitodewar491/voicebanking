package com.voicebanking.utils.tts;

import java.nio.file.Files;
import java.nio.file.Path;

/** macOS TTS using the built-in {@code say} command and {@code afconvert} — no installation required. */
public class MacSayTtsEngine implements TtsEngine {

    @Override
    public String generate(String text) throws Exception {
        Path aiffFile = Files.createTempFile("voice_query_", ".aiff");
        Path wavFile  = Files.createTempFile("voice_query_", ".wav");
        String aiffPath = aiffFile.toAbsolutePath().toString();
        String wavPath  = wavFile.toAbsolutePath().toString();

        // say: built-in macOS TTS, no install needed
        // -v Samantha: clear US English voice present on all macOS versions
        // -r 130: ~130 words/min, comparable to SAPI Rate=-4
        Process say = new ProcessBuilder("say", "-v", "Samantha", "-r", "130", "-o", aiffPath, text)
                .redirectErrorStream(true)
                .start();
        String sayOut = new String(say.getInputStream().readAllBytes());
        if (say.waitFor() != 0) {
            throw new RuntimeException("macOS say failed for: " + text + "\n" + sayOut);
        }

        // afconvert: built-in macOS audio converter
        // -f WAVE  : output format WAV
        // -d LEI16@16000 : 16-bit little-endian PCM at 16 kHz (same as SAPI output)
        // -c 1     : mono
        Process convert = new ProcessBuilder(
                "afconvert", "-f", "WAVE", "-d", "LEI16@16000", "-c", "1", aiffPath, wavPath)
                .redirectErrorStream(true)
                .start();
        String convertOut = new String(convert.getInputStream().readAllBytes());
        Files.deleteIfExists(aiffFile);
        if (convert.waitFor() != 0) {
            throw new RuntimeException("afconvert failed: " + convertOut);
        }

        return wavPath;
    }
}
