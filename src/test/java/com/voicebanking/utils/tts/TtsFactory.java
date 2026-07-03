package com.voicebanking.utils.tts;

public class TtsFactory {

    public static TtsEngine create() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return new WindowsSapiTtsEngine();
        }
        return new EspeakTtsEngine();
    }
}
