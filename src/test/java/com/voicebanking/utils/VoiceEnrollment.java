package com.voicebanking.utils;

import com.voicebanking.DataText.VoiceQueries;
import com.voicebanking.pages.VoiceRegistrationPage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Walks the whole voice-enrollment flow, from the first recording step to the "Start Banking"
 * screen — shared by every class that enrolls a voiceprint (UI11, UI13, UI14, UI15).
 *
 * <p>Enrollment is currently 6 steps (confirmed live on stage, 2026-10-08): steps 1-2 show an
 * image to describe, steps 3-6 show a random question to answer. Rather than hardcoding that, this
 * keeps recording steps until Start Banking appears, and decides what to say per step:
 * <ul>
 *   <li>Image step — the image's own description, read off the speaker button.</li>
 *   <li>Question step — a natural answer to the question on screen (see {@link #answerFor}).</li>
 * </ul>
 * Each step's speech is generated in {@code voice} and copied over the browser's fake-mic file
 * ({@code --use-file-for-fake-audio-capture}) right before recording that step.
 */
public final class VoiceEnrollment {

    /** Safety stop — well above today's 6 steps, so a flow that never finishes fails clearly. */
    private static final int MAX_STEPS = 12;
    private static final int MAX_TAKES_PER_STEP = 3;

    private VoiceEnrollment() {
    }

    /**
     * Call right after {@link VoiceRegistrationPage#clickStartRegistration()}. Returns once the
     * "Start Banking" screen is showing — the caller still clicks Start Banking itself.
     *
     * @param fakeAudioPath the WAV the browser was launched with as its fake microphone
     * @param voice         edge-tts voice to enroll with, e.g. {@code EdgeTtsEngine.VOICE}
     * @param tag           log prefix and kept-audio file-name prefix, e.g. "VoiceRegistration"
     */
    public static void completeAllSteps(VoiceRegistrationPage voicePage, String fakeAudioPath,
                                        String voice, String tag) throws Exception {
        for (int step = 1; step <= MAX_STEPS; step++) {
            voicePage.waitForRecordingScreenReady();
            boolean isQuestion = voicePage.waitForStepContentIsQuestion(5000);
            String label = voicePage.getStepLabel();

            String toSay;
            if (isQuestion) {
                String question = voicePage.getQuestionText();
                toSay = answerFor(question);
                System.out.println("[" + tag + "] Enrollment " + label + " (question): " + question);
            } else {
                String description = voicePage.capturePlayAudioText(3000);
                toSay = description != null ? description : VoiceQueries.English.VOICE_ENROLLMENT_PHRASE;
                System.out.println("[" + tag + "] Enrollment " + label + " (image): "
                        + (description != null ? description : "<no description read out — using the fixed enrollment phrase>"));
            }
            System.out.println("[" + tag + "] Enrollment " + label + " speaking: " + toSay);

            String stepWav = TtsUtil.generateWav(toSay, voice);
            TtsUtil.keepCopy(stepWav, tag + "_enrollment_step" + step + "_" + (isQuestion ? "question" : "image") + "_" + voice);
            Files.copy(Path.of(stepWav), Path.of(fakeAudioPath), StandardCopyOption.REPLACE_EXISTING);
            TtsUtil.deleteWav(stepWav);

            recordAcceptedTake(voicePage, label.isEmpty() ? "step " + step : label, tag);
            voicePage.clickSubmit();

            if (voicePage.waitForNextStepOrFinish(30000)) {
                System.out.println("[" + tag + "] Enrollment complete after " + step + " steps.");
                return;
            }
        }
        throw new IllegalStateException("[" + tag + "] Enrollment never reached Start Banking after "
                + MAX_STEPS + " steps");
    }

    private static void recordAcceptedTake(VoiceRegistrationPage voicePage, String label, String tag) {
        for (int take = 1; take <= MAX_TAKES_PER_STEP; take++) {
            System.out.println("[" + tag + "] Recording " + label + " (take " + take + " of "
                    + MAX_TAKES_PER_STEP + ")...");
            voicePage.tapMicAndRecord();
            if (voicePage.waitForRecordingAccepted(5000)) return;

            System.out.println("[" + tag + "] Recording not accepted — re-recording...");
            voicePage.clickRerecord();
        }
        throw new RuntimeException("Recording rejected " + MAX_TAKES_PER_STEP
                + " times in a row for enrollment " + label + " — giving up");
    }

    /**
     * A natural, few-sentence spoken answer to an enrollment question. The questions are picked at
     * random by the app, so this matches on topic keywords (covering every question seen live so
     * far) and falls back to a general everyday-life answer. The app doesn't check what the answer
     * says — it's capturing the voice — but answering on-topic keeps the recording realistic.
     */
    static String answerFor(String question) {
        String q = question.toLowerCase(Locale.ROOT);
        if (q.matches(".*\\bbreakfast\\b.*")) {
            return "For breakfast today I had poha with a little lemon and some peanuts, and a cup of "
                    + "hot tea. Yes, I really enjoyed it. It is light, quick to make, and it keeps me "
                    + "going until lunch without feeling too heavy.";
        }
        if (q.matches(".*\\b(tea|coffee)\\b.*")) {
            return "I prefer tea over coffee. I like it the Indian way, with milk, a little sugar, "
                    + "some ginger and cardamom, boiled for a few minutes. I usually have one cup in the "
                    + "morning and another one in the evening with a light snack.";
        }
        if (q.matches(".*\\bweather\\b.*")) {
            return "The weather here today is quite pleasant. It was a little cloudy in the morning, "
                    + "and now the sun is out with a light breeze. It is not too hot, so it is a nice "
                    + "day to step outside for a walk in the evening.";
        }
        if (q.matches(".*\\b(morning|routine|typical day)\\b.*")) {
            return "A typical morning for me starts at around six thirty. I drink a glass of warm water, "
                    + "do some light stretching, and then make tea and breakfast. After that I get ready, "
                    + "check my messages and leave for work by nine.";
        }
        // Before the travel check — "a market or shop you like to visit" also says "visit".
        if (q.matches(".*\\b(market|shop|shops|shopping|store)\\b.*")) {
            return "I like visiting the vegetable market near my house on Sunday mornings. It is busy and "
                    + "colourful, the vegetables are fresh, and I know a few of the sellers well. I usually "
                    + "pick up fruits too and stop for a quick cup of tea on the way back.";
        }
        if (q.matches(".*\\b(visit|someday|trip|travel to)\\b.*")) {
            return "Someday I would really like to visit Ladakh. I have seen so many pictures of the "
                    + "mountains, the clear blue lakes and the monasteries. I want to go there on a road "
                    + "trip with my friends and just enjoy the quiet and the views.";
        }
        if (q.matches(".*\\b(room|sitting)\\b.*")) {
            return "I am sitting in my living room right now. There is a comfortable sofa, a small "
                    + "wooden table with a plant on it, and a big window that lets in a lot of light. "
                    + "The walls are light cream and there are some family photos on one side.";
        }
        if (q.matches(".*\\b(book|story|novel|read)\\b.*")) {
            return "One book I really liked is Malgudi Days by R K Narayan. The stories are simple, "
                    + "warm and funny, about everyday people in a small town. Reading it reminded me of "
                    + "my own childhood and the neighbours we had back then.";
        }
        if (q.matches(".*\\b(season|monsoon|summer|winter)\\b.*")) {
            return "I like the monsoon season the best. The first rain after the hot summer feels "
                    + "wonderful, everything turns green, and the air smells fresh. I love sitting by the "
                    + "window with a cup of tea and some hot pakoras while it rains.";
        }
        if (q.matches(".*\\b(festival|celebrate|celebration)\\b.*")) {
            return "Diwali is the festival I enjoy the most. We clean and decorate the house, light diyas "
                    + "and make rangoli at the entrance. The whole family gets together, we make sweets at "
                    + "home, wear new clothes and visit friends and relatives in the evening.";
        }
        if (q.matches(".*\\b(animal|animals|pet|pets|dog|cat|bird)\\b.*")) {
            return "I really like dogs. They are loyal, playful and always happy to see you. "
                    + "When I was young we had a dog named Bruno, and he would wait at the gate every day "
                    + "for me to come home from school.";
        }
        if (q.matches(".*\\b(cook|dinner|food|meal|eat|recipe|dish)\\b.*")) {
            return "If friends came over for dinner, I would cook a simple home style meal. Probably dal, "
                    + "jeera rice, fresh rotis and a paneer curry, with some kheer for dessert. "
                    + "I like food that everyone can enjoy while we sit and talk for a long time.";
        }
        if (q.matches(".*\\b(get around|travel|commute|town|city|transport|office)\\b.*")) {
            return "I usually get around my city by metro and sometimes by auto rickshaw. "
                    + "For short distances I prefer to walk, because it is relaxing and I get to see the "
                    + "local shops. On weekends I drive if the whole family is going out together.";
        }
        if (q.matches(".*\\b(sport|sports|game|games|cricket|football|play|playing)\\b.*")) {
            return "I really enjoy cricket, both playing and watching it. On Sunday mornings I play a "
                    + "friendly match with my neighbours, and whenever there is a big match on TV the whole "
                    + "family sits together to watch it. I also like a game of badminton in the evening.";
        }
        if (q.matches(".*\\b(hobby|hobbies)\\b.*")) {
            return "One hobby I really enjoy is gardening. I got started a few years ago when my mother "
                    + "gave me a small tulsi plant, and now my balcony is full of flowers and herbs. "
                    + "Watering them every morning is a calm way to start the day.";
        }
        if (q.matches(".*\\bwhere\\b.*\\brelax\\b.*|.*\\b(place|places)\\b.*")) {
            return "When I want to relax, I like to go to the park near my house or to the lake in the "
                    + "evening. It is quiet there, there are lots of trees, and I can sit on a bench, "
                    + "listen to music and just watch people walk by for an hour or so.";
        }
        if (q.matches(".*\\b(day off|holiday|vacation|perfect day|relax)\\b.*")) {
            return "A perfect day off for me starts with a slow breakfast and a cup of tea. "
                    + "Then I would read a book for a while, meet a couple of friends in the afternoon, "
                    + "and end the day watching a good movie at home with my family.";
        }
        if (q.matches(".*\\b(weekend|weekends|free time|fun)\\b.*")) {
            return "On weekends I like to sleep in a little, clean up the house and go grocery shopping. "
                    + "In the evening I usually go for a long walk in the park or visit my relatives, "
                    + "and sometimes we order food and play board games together.";
        }
        return "That is a nice question. I would say I enjoy simple things, like spending time with my "
                + "family, cooking at home and going for a walk in the evening. "
                + "Most days I keep things relaxed and try to do something that makes me happy.";
    }
}
