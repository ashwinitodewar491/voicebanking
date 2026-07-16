package com.voicebanking.tests.ui;

import com.voicebanking.DataText.VoiceQueries;
import com.voicebanking.pages.HomePage;
import com.voicebanking.tests.ui.base.BaseVoiceTest;
import com.voicebanking.utils.TtsUtil;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Predicate;

/**
 * Exploratory run: no transfer-response format has been confirmed via manual testing yet, so
 * every row asserts on keywords rather than a regex — the point is to observe what the bot
 * actually says at each step before writing a precise pattern.
 * <p>
 * Every row walks the full follow-up chain (beneficiary → amount → confirm → OTP) through to a
 * completed real ₹1 transfer to Pooja Desai — a confirmed real beneficiary for this account, and
 * ₹1 matches this account's own seeded transfer pattern. This means a single run of this suite
 * executes up to 8 separate real transfers on the live system.
 */
public class UI10_TransferMoneyTest extends BaseVoiceTest {

    /** Customer A (Sneha Kulkarni, CIF202602260010) — has Pooja Desai as a real, existing
     * beneficiary (confirmed live: she appears alongside Vikas Patil in every "available
     * options" prompt this account's transfer flow has produced). */
    @Override
    protected String getLoginPhoneNumber() {
        return "9765432109";
    }

    @DataProvider(name = "voiceQueries")
    public Object[][] voiceQueries() {
        return new Object[][]{

            // {queryName, query, expectedKeywords, assertionPattern, disambiguationAccount}
            // disambiguationAccount doubles as "who to answer with if the bot asks which
            // beneficiary to use" — every row uses "Pooja Desai" since every row is meant to
            // reach a completed transfer regardless of how much the query itself already specified.
            {"Can I Transfer Money",           VoiceQueries.English.CAN_TRANSFER_MONEY,
                    new String[]{"transfer", "success"}, null, "Pooja Desai"},
            {"Transfer Money Short",           VoiceQueries.English.TRANSFER_MONEY_SHORT,
                    new String[]{"transfer", "success"}, null, "Pooja Desai"},
            {"Send Money From Savings",        VoiceQueries.English.SEND_MONEY_FROM_SAVINGS,
                    new String[]{"transfer", "success"}, null, "Pooja Desai"},
            {"Transfer Using UPI",             VoiceQueries.English.TRANSFER_USING_UPI,
                    new String[]{"transfer", "success"}, null, "Pooja Desai"},
            {"Make Transfer To Beneficiary",   VoiceQueries.English.MAKE_TRANSFER_TO_BENEFICIARY,
                    new String[]{"transfer", "success"}, null, "Pooja Desai"},
            {"Transfer Amount To Beneficiary", VoiceQueries.English.TRANSFER_AMOUNT_TO_BENEFICIARY,
                    new String[]{"transfer", "success"}, null, "Pooja Desai"},
            {"Send Amount To Beneficiary",     VoiceQueries.English.SEND_AMOUNT_TO_BENEFICIARY,
                    new String[]{"transfer", "success"}, null, "Pooja Desai"},
            {"Pay Amount To Beneficiary",      VoiceQueries.English.PAY_AMOUNT_TO_BENEFICIARY,
                    new String[]{"transfer", "success"}, null, "Pooja Desai"},
        };
    }

    @Test(dataProvider = "voiceQueries", groups = {"ui", "regression", "botverification"},
            description = "Should process English transfer-money voice query and verify bot response")
    public void testVoiceQuery(String queryName, String query, String[] expectedKeywords,
                                String assertionPattern, String disambiguationAccount) throws Exception {
        runVoiceQuery(queryName, query, expectedKeywords, assertionPattern, disambiguationAccount);
    }

    /** Also recognizes transfer confirmation, the amount prompt, OTP requests, and beneficiary
     * disambiguation, so the transcription-retry loop stops re-playing the original query's audio
     * once the bot has already moved into one of those states. This was the root cause behind
     * rows appearing to "skip" the intended follow-up entirely: a retry of the original query
     * (e.g. spoken a second time because the first attempt was mis-transcribed) could get heard
     * by the bot as an answer to whatever it was actually asking by then, silently advancing the
     * conversation past the follow-up this row meant to exercise. */
    @Override
    protected boolean shouldStopRetrying(String botResponse) {
        return super.shouldStopRetrying(botResponse)
                || isTransferConfirmation(botResponse)
                || isAmountPrompt(botResponse)
                || isOtpRequested(botResponse)
                || isBeneficiaryDisambiguation(botResponse);
    }

    /** Walks whatever's left of the beneficiary → amount → confirm → OTP chain from wherever the
     * bot's response currently sits — each query in the data provider already specifies a
     * different amount of detail up front (some name a beneficiary and amount, some name
     * neither), so the bot can land on any of these prompts first. Each step uses
     * {@link #speakSingleFollowUp} rather than an exact word-match retry: two prior live runs
     * showed STT unreliably drops words from a follow-up phrase (a leading "Yes." on the confirm
     * reply, or inserting "means" into the cancel reply) even when the bot itself understood and
     * advanced — a retry gated on exact wording kept re-sending replies the bot had already acted
     * on, and each superfluous retry got misread by the bot as a wrong OTP attempt, burning
     * through its limited attempt budget before the real OTP was ever sent. Gating on "did the
     * bot's response actually move past this prompt" instead avoids that. */
    @Override
    protected String handleAdditionalFollowUp(String queryName, String botResponse,
                                               String disambiguationAccount, HomePage homePage) throws Exception {
        String beneficiary = disambiguationAccount != null ? disambiguationAccount : "Pooja Desai";

        if (isBeneficiaryDisambiguation(botResponse)) {
            botResponse = speakSingleFollowUp(queryName, homePage, beneficiary,
                    "Bot asked to choose a beneficiary", this::isBeneficiaryDisambiguation);
        }
        if (isAmountPrompt(botResponse)) {
            botResponse = speakSingleFollowUp(queryName, homePage, VoiceQueries.English.TRANSFER_AMOUNT_FOLLOWUP,
                    "Bot asked for the amount", this::isAmountPrompt);
        }
        if (isTransferConfirmation(botResponse)) {
            botResponse = speakSingleFollowUp(queryName, homePage, VoiceQueries.English.CONFIRM_THE_TRANSFER,
                    "Bot asked to confirm the transfer", this::isTransferConfirmation);
        }
        if (isOtpRequested(botResponse)) {
            botResponse = answerOtpPrompt(queryName, homePage);
        }

        return botResponse;
    }

    /** Returns true when the bot response is asking the user to confirm a pending money
     * transfer. Exploratory — no confirmation phrasing has been confirmed via manual testing
     * yet, so this is deliberately the loosest possible signal (the word "confirm") pending
     * observation of what the bot actually says. */
    private boolean isTransferConfirmation(String response) {
        return response.toLowerCase().contains("confirm");
    }

    /** Returns true when the bot is asking how much money to transfer — observed verbatim as
     * "How much money would you like to transfer?". */
    private boolean isAmountPrompt(String response) {
        return response.toLowerCase().contains("how much");
    }

    /** Returns true when the bot is asking for (or rejecting an attempt at) the OTP that
     * authorizes a confirmed transfer. Covers both the initial ask ("Please say your six number
     * OTP, one number at a time...") and rejection phrasings that don't repeat the word "OTP" at
     * all ("You provided too many numbers. Please say exactly six numbers. You have 2
     * attempt(s) left.") — "six number" appears in every variant seen so far, so retries aren't
     * cut short just because a rejection message happened to be worded differently. */
    private boolean isOtpRequested(String response) {
        String lower = response.toLowerCase();
        return lower.contains("otp") || lower.contains("six number");
    }

    /** Returns true when the bot is asking which beneficiary to use — observed verbatim as
     * "Please choose from the available options: Vikas Patil and Pooja Desai." */
    private boolean isBeneficiaryDisambiguation(String response) {
        return response.toLowerCase().contains("available options");
    }

    /** Speaks the fixed test OTP (111111) as separate digit words, matching the bot's own
     * instruction to say the six digits individually rather than as a single number (TTS would
     * otherwise read "111111" as one large number, not six digits). */
    private String answerOtpPrompt(String queryName, HomePage homePage) throws Exception {
        String otpPhrase = VoiceQueries.English.TEST_OTP_SPOKEN;
        String response = "";

        for (int attempt = 1; attempt <= 3; attempt++) {
            System.out.println("[" + queryName + "] Bot asked for the OTP — speaking '" + otpPhrase
                    + "' (attempt " + attempt + ")...");

            long oldAudioDurationMs = TtsUtil.getWavDurationMs(currentAudioPath);
            String otpPath = TtsUtil.generateWav(otpPhrase);
            Files.copy(Path.of(otpPath), Path.of(currentAudioPath), StandardCopyOption.REPLACE_EXISTING);
            int otpHoldMs = (int) TtsUtil.getWavDurationMs(currentAudioPath);
            TtsUtil.deleteWav(otpPath);

            homePage.reacquireMicrophoneForFollowUp();

            int preWaitMs = (int) oldAudioDurationMs + 2000 + ((attempt - 1) * 2000);
            homePage.speakFollowUp(preWaitMs, otpHoldMs, 8000);
            homePage.waitForVoiceResponse(15000);

            String transcribed = homePage.getLastTranscribedText();
            response = homePage.getLastBotResponse();
            System.out.println("[" + queryName + "] OTP Transcribed : " + transcribed);
            System.out.println("[" + queryName + "] OTP Bot response: " + response);

            if (!isOtpRequested(response)) break;
            System.out.println("[" + queryName + "] WARN — OTP not accepted (attempt " + attempt
                    + "): transcribed [" + transcribed + "], bot [" + response + "] — retrying...");
        }

        return response;
    }

    /** Speaks a follow-up phrase, retrying up to 3 times if either speech-to-text detects
     * nothing at all, or the bot's response indicates it hasn't actually moved past the prompt
     * it was stuck on ({@code stillWaiting} returns true for the response) — a garbled
     * transcription can still count as "something was heard" without the bot understanding it,
     * so silence alone isn't a reliable retry signal. Used for chaining a resolved conversation
     * forward one step at a time without repeating the full word-match retry ceremony that
     * previously caused wasted retries to eat into the bot's own OTP-attempt budget. */
    private String speakSingleFollowUp(String queryName, HomePage homePage, String phrase, String logLabel,
                                        Predicate<String> stillWaiting) throws Exception {
        String response = "";

        for (int attempt = 1; attempt <= 3; attempt++) {
            System.out.println("[" + queryName + "] " + logLabel + " — speaking '" + phrase
                    + "' (attempt " + attempt + ")...");

            long oldAudioDurationMs = TtsUtil.getWavDurationMs(currentAudioPath);
            String followUpPath = TtsUtil.generateWav(phrase);
            Files.copy(Path.of(followUpPath), Path.of(currentAudioPath), StandardCopyOption.REPLACE_EXISTING);
            int followUpHoldMs = (int) TtsUtil.getWavDurationMs(currentAudioPath);
            TtsUtil.deleteWav(followUpPath);

            homePage.reacquireMicrophoneForFollowUp();

            int preWaitMs = (int) oldAudioDurationMs + 2000 + ((attempt - 1) * 2000);
            try {
                homePage.speakFollowUp(preWaitMs, followUpHoldMs, 10000);
            } catch (RuntimeException e) {
                System.out.println("[" + queryName + "] " + logLabel + " — no speech detected (attempt "
                        + attempt + "): " + e.getMessage() + " — retrying...");
                continue;
            }
            homePage.waitForVoiceResponse(15000);

            String transcribed = homePage.getLastTranscribedText();
            response = homePage.getLastBotResponse();
            System.out.println("[" + queryName + "] " + logLabel + " Transcribed : " + transcribed);
            System.out.println("[" + queryName + "] " + logLabel + " Bot response: " + response);

            if (!stillWaiting.test(response)) break;
            System.out.println("[" + queryName + "] " + logLabel + " — bot did not advance (attempt "
                    + attempt + ") — retrying...");
        }

        return response;
    }
}
