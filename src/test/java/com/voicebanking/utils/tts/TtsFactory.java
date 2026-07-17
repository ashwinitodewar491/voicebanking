package com.voicebanking.utils.tts;

public class TtsFactory {

    public static TtsEngine create() {
        return new EdgeTtsEngine();
    }
}
