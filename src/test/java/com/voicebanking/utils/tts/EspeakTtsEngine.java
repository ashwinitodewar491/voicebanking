package com.voicebanking.utils.tts;

import java.nio.file.Files;
import java.nio.file.Path;

public class EspeakTtsEngine implements TtsEngine {

    @Override
    public String generate(String text) throws Exception {
        Path tempFile = Files.createTempFile("voice_query_", ".wav");
        String wavPath = tempFile.toAbsolutePath().toString();

        // -v en  : English voice
        // -s 120 : slightly slower speed (matches SAPI Rate=-4)
        // -a 100 : full amplitude
        // -w     : write to WAV file
        Process process = new ProcessBuilder(
                "espeak", "-v", "en", "-s", "120", "-a", "100", "-w", wavPath, text)
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("eSpeak TTS failed for: " + text + "\n" + output);
        }
        return wavPath;
    }
}
