package com.voicebanking.utils.tts;

public class TtsFactory {

    public static TtsEngine create() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win"))  return new WindowsSapiTtsEngine();
        if (os.contains("mac"))  return new MacSayTtsEngine();
        return new EspeakTtsEngine(); // Linux / CI
    }
}
