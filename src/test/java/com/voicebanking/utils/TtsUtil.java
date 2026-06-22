package com.voicebanking.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class TtsUtil {

    public static String generateWav(String text) throws Exception {
        return generateWavSapi(text);
    }

    private static String generateWavSapi(String text) throws Exception {
        Path tempFile = Files.createTempFile("voice_query_", ".wav");
        String wavPath = tempFile.toAbsolutePath().toString();

        String psCommand =
                "Add-Type -AssemblyName System.Speech; " +
                "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                "$s.Rate = -1; " +
                "$fmt = New-Object System.Speech.AudioFormat.SpeechAudioFormatInfo(" +
                "16000, [System.Speech.AudioFormat.AudioBitsPerSample]::Sixteen, " +
                "[System.Speech.AudioFormat.AudioChannel]::Mono); " +
                "$s.SetOutputToWaveFile('" + wavPath + "', $fmt); " +
                "$b = New-Object System.Speech.Synthesis.PromptBuilder; " +
                "$b.AppendBreak([TimeSpan]::FromSeconds(1.5)); " +
                "$b.AppendText('" + text.replace("'", "''") + "'); " +
                "$b.AppendBreak([TimeSpan]::FromSeconds(6)); " +
                "$s.Speak($b); " +
                "$s.Dispose()";

        Process process = new ProcessBuilder("powershell.exe", "-Command", psCommand)
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("SAPI TTS failed for: " + text + "\n" + output);
        }
        return wavPath;
    }

    public static void deleteWav(String wavPath) {
        if (wavPath != null) new File(wavPath).delete();
    }
}
