package com.voicebanking.utils.tts;

import java.nio.file.Files;
import java.nio.file.Path;

public class WindowsSapiTtsEngine implements TtsEngine {

    @Override
    public String generate(String text) throws Exception {
        Path tempFile = Files.createTempFile("voice_query_", ".wav");
        String wavPath = tempFile.toAbsolutePath().toString();

        String psCommand =
                "Add-Type -AssemblyName System.Speech; " +
                "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                "try { $s.SelectVoice('Microsoft Zira Desktop') } catch { }; " +
                "$s.Rate = -4; " +
                "$fmt = New-Object System.Speech.AudioFormat.SpeechAudioFormatInfo(" +
                "16000, [System.Speech.AudioFormat.AudioBitsPerSample]::Sixteen, " +
                "[System.Speech.AudioFormat.AudioChannel]::Mono); " +
                "$s.SetOutputToWaveFile('" + wavPath + "', $fmt); " +
                "$b = New-Object System.Speech.Synthesis.PromptBuilder; " +
                "$b.AppendText('" + text.replace("'", "''") + "'); " +
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
}
