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

/** Transfer-money flow. Every row completes a real ₹1 transfer to Pooja Nair (a beneficiary on Rohit
 * Mehta's savings account), so one run can make up to 8 live transfers. Assertions are
 * keyword-based until the response formats are confirmed. */
public class UI10_TransferMoneyTest extends BaseVoiceTest {

    /** Shared session is riskier here: a state leak between rows could complete a wrong transfer or
     * burn OTP attempts. Revert this override if transfers or OTP failures look wrong. */
    @Override
    protected boolean useSharedSession() {
        return true;
    }

    /** Rohit Mehta; he has 3 beneficiaries, so generic rows hit beneficiary disambiguation and
     * answer "Pooja Nair". */
    @Override
    protected String getLoginPhoneNumber() {
        return "9898989898";
    }

    @DataProvider(name = "voiceQueries")
    public Object[][] voiceQueries() {
        return new Object[][]{
            // {queryName, query, expectedKeywords, assertionPattern, disambiguationAccount}
            // disambiguationAccount doubles as "who to answer with if the bot asks which
            // beneficiary to use" — every row uses "Pooja Nair" since every row is meant to
            // reach a completed transfer regardless of how much the query itself already specified.
            {"Can I Transfer Money", VoiceQueries.English.CAN_TRANSFER_MONEY,
                new String[]{"transfer", "success"}, null, "Pooja Nair"},
            {"Transfer Money Short", VoiceQueries.English.TRANSFER_MONEY_SHORT,
                new String[]{"transfer", "success"}, null, "Pooja Nair"},
            {"Send Money From Savings", VoiceQueries.English.SEND_MONEY_FROM_SAVINGS,
                new String[]{"transfer", "success"}, null, "Pooja Nair"},
            {"Transfer Using UPI", VoiceQueries.English.TRANSFER_USING_UPI,
                new String[]{"transfer", "success"}, null, "Pooja Nair"},
            {"Make Transfer To Beneficiary", VoiceQueries.English.MAKE_TRANSFER_TO_BENEFICIARY,
                new String[]{"transfer", "success"}, null, "Pooja Nair"},
            {"Transfer Amount To Beneficiary", VoiceQueries.English.TRANSFER_AMOUNT_TO_BENEFICIARY,
                new String[]{"transfer", "success"}, null, "Pooja Nair"},
            {"Send Amount To Beneficiary", VoiceQueries.English.SEND_AMOUNT_TO_BENEFICIARY,
                new String[]{"transfer", "success"}, null, "Pooja Nair"},
            {"Pay Amount To Beneficiary", VoiceQueries.English.PAY_AMOUNT_TO_BENEFICIARY,
                new String[]{"transfer", "success"}, null, "Pooja Nair"},};
    }

    @Test(dataProvider = "voiceQueries", groups = {"ui", "regression", "botverificationTransferMoney"},
            description = "Should process English transfer-money voice query and verify bot response")
    public void testVoiceQuery(String queryName, String query, String[] expectedKeywords,
            String assertionPattern, String disambiguationAccount) throws Exception {
        runVoiceQuery(queryName, query, expectedKeywords, assertionPattern, disambiguationAccount);
    }

    /** Fast health-check tier covering each starting level of detail (none, beneficiary only,
     * beneficiary + amount). Each row makes a real ₹1 transfer. */
    @DataProvider(name = "smokeQueries")
    public Object[][] smokeQueries() {
        return new Object[][]{
            {"Can I Transfer Money", VoiceQueries.English.CAN_TRANSFER_MONEY,
                new String[]{"transfer", "success"}, null, "Pooja Nair"},
            {"Send Money From Savings", VoiceQueries.English.SEND_MONEY_FROM_SAVINGS,
                new String[]{"transfer", "success"}, null, "Pooja Nair"},
            {"Make Transfer To Beneficiary", VoiceQueries.English.MAKE_TRANSFER_TO_BENEFICIARY,
                new String[]{"transfer", "success"}, null, "Pooja Nair"},
            {"Transfer Amount To Beneficiary", VoiceQueries.English.TRANSFER_AMOUNT_TO_BENEFICIARY,
                new String[]{"transfer", "success"}, null, "Pooja Nair"},
            {"Pay Amount To Beneficiary", VoiceQueries.English.PAY_AMOUNT_TO_BENEFICIARY,
                new String[]{"transfer", "success"}, null, "Pooja Nair"},
        };
    }

    @Test(dataProvider = "smokeQueries", groups = {"ui", "smoke", "botverificationTransferMoney"},
            description = "Smoke: should process a basic English transfer-money voice query")
    public void testSmokeQuery(String queryName, String query, String[] expectedKeywords,
            String assertionPattern, String disambiguationAccount) throws Exception {
        runVoiceQuery(queryName, query, expectedKeywords, assertionPattern, disambiguationAccount);
    }

    /** Also stops retrying on the confirmation, amount, OTP and beneficiary prompts, so a replayed
     * query isn't taken as an answer. */
    @Override
    protected boolean shouldStopRetrying(String botResponse) {
        return super.shouldStopRetrying(botResponse)
                || isTransferConfirmation(botResponse)
                || isAmountPrompt(botResponse)
                || isOtpRequested(botResponse)
                || isBeneficiaryDisambiguation(botResponse);
    }

    /** Walks the remaining beneficiary, amount, confirm and OTP steps from wherever the bot is.
     * Advances on the bot's response rather than exact STT wording, to avoid wasting OTP attempts. */
    @Override
    protected String handleAdditionalFollowUp(String queryName, String query, String botResponse,
            String disambiguationAccount, HomePage homePage) throws Exception {
        String beneficiary = disambiguationAccount != null ? disambiguationAccount : "Pooja Nair";

        // Wrapped in a bounded loop, not a single sequential pass — a session drop mid-chain
        // forces reestablishIntentIfSessionEnded (inside each stage helper below) to re-send the
        // ORIGINAL query, which can rewind the conversation to an earlier stage than where it
        // dropped (e.g. back to "which beneficiary" after already having reached "confirm"). One
        // pass through the four checks wouldn't re-walk forward from there; several passes will.
        // Capped well above the 4 real stages so a full rewind-and-replay still completes.
        for (int pass = 0; pass < 8; pass++) {
            String beforePass = botResponse;

            if (isBeneficiaryDisambiguation(botResponse)) {
                botResponse = speakSingleFollowUp(queryName, query, homePage, beneficiary,
                        "Bot asked to choose a beneficiary", this::isBeneficiaryDisambiguation);
            }
            if (isAmountPrompt(botResponse)) {
                botResponse = speakSingleFollowUp(queryName, query, homePage, VoiceQueries.English.TRANSFER_AMOUNT_FOLLOWUP,
                        "Bot asked for the amount", this::isAmountPrompt);
            }
            if (isTransferConfirmation(botResponse)) {
                botResponse = speakSingleFollowUp(queryName, query, homePage, VoiceQueries.English.CONFIRM_THE_TRANSFER,
                        "Bot asked to confirm the transfer", this::isTransferConfirmation);
            }
            if (isOtpRequested(botResponse)) {
                botResponse = answerOtpPrompt(queryName, query, homePage);
            }

            boolean stillInKnownState = isBeneficiaryDisambiguation(botResponse) || isAmountPrompt(botResponse)
                    || isTransferConfirmation(botResponse) || isOtpRequested(botResponse);
            if (!stillInKnownState || botResponse.equals(beforePass)) break;
        }

        return botResponse;
    }

    /** True if the bot is asking to confirm the transfer (loose match on "confirm"). */
    private boolean isTransferConfirmation(String response) {
        return response.toLowerCase().contains("confirm");
    }

    /** True if the bot is asking for the transfer amount. */
    private boolean isAmountPrompt(String response) {
        return response.toLowerCase().contains("how much");
    }

    /** True if the bot is asking for (or rejecting) the OTP; "six number" appears in every variant. */
    private boolean isOtpRequested(String response) {
        String lower = response.toLowerCase();
        return lower.contains("otp") || lower.contains("six number");
    }

    /** True if the bot is asking which beneficiary to use. */
    private boolean isBeneficiaryDisambiguation(String response) {
        return response.toLowerCase().contains("available options");
    }

    /** Speaks the test OTP 111111 as six separate digit words. */
    private String answerOtpPrompt(String queryName, String query, HomePage homePage) throws Exception {
        String otpPhrase = VoiceQueries.English.TEST_OTP_SPOKEN;
        String response = "";

        for (int attempt = 1; attempt <= 3; attempt++) {
            response = reestablishIntentIfSessionEnded(homePage, queryName, query, response);
            // Excludes a still-broken context-lost fallback — if re-sending the original query got
            // the same fallback back, the bot hasn't actually recovered; keep retrying instead of
            // handing that fallback off to the caller as if it were a resolved stage.
            if (!response.isEmpty() && !isOtpRequested(response) && !isContextLostFallback(response)) {
                // Session dropped and the resend either completed the transfer a different way
                // or rewound to an earlier stage (beneficiary/amount/confirm) — either way, OTP
                // isn't the next thing to answer; let the caller's outer loop re-check from here.
                return response;
            }

            System.out.println("[" + queryName + "] Bot asked for the OTP — speaking '" + otpPhrase
                    + "' (attempt " + attempt + ")...");

            long oldAudioDurationMs = TtsUtil.getWavDurationMs(currentAudioPath);
            String otpPath = TtsUtil.generateWav(otpPhrase);
            Files.copy(Path.of(otpPath), Path.of(currentAudioPath), StandardCopyOption.REPLACE_EXISTING);
            int otpHoldMs = (int) TtsUtil.getWavDurationMs(currentAudioPath);
            TtsUtil.deleteWav(otpPath);

            homePage.reacquireMicrophoneForFollowUp();

            int preWaitMs = (int) oldAudioDurationMs + 2000 + ((attempt - 1) * 2000);
            try {
                homePage.speakFollowUp(preWaitMs, otpHoldMs, 8000);
            } catch (RuntimeException sttNotDetected) {
                // A transient STT miss here (no user bubble at all within the poll window) is not
                // the same as the bot rejecting a heard OTP — but until now it escaped this loop's
                // own retry mechanism uncaught and killed the test outright on whichever attempt
                // hit it, even attempt 1 or 2 of the intended 3. Confirmed live: the transfer often
                // completes for real seconds later regardless (visible in the failure screenshot,
                // captured after tearDown's own delay), because the browser session and the bot's
                // own OTP handling keep going — only this test's premature exit was ever the
                // problem. Treat it exactly like an "OTP not accepted" response: log and retry.
                System.out.println("[" + queryName + "] WARN — OTP attempt " + attempt
                        + " STT not detected (" + sttNotDetected.getMessage() + ") — retrying...");
                continue;
            }
            homePage.waitForVoiceResponse(15000);

            String transcribed = homePage.getLastTranscribedText();
            response = homePage.getLastBotResponse();
            System.out.println("[" + queryName + "] OTP Transcribed : " + transcribed);
            System.out.println("[" + queryName + "] OTP Bot response: " + response);

            // Excludes the context-lost fallback for the same reason as speakSingleFollowUp()
            // below — it isn't an OTP-request prompt either, so without this the loop would break
            // out treating the fallback as a real advance instead of retrying/recovering. Also
            // excludes a blank response — confirmed live via the account's actual transaction
            // history that the transfer can still be mid-flight (bot hasn't finished responding)
            // right when this check runs, and a blank response was being treated as "resolved,
            // stop retrying" the same way BaseVoiceTest's own re-ask loop already knows blank means
            // "not done yet" elsewhere — here it fell through to the caller as a final answer,
            // which then failed the keyword/pattern check downstream against nothing at all
            // instead of ever seeing the real confirmation.
            if (!response.isBlank() && !isOtpRequested(response) && !isContextLostFallback(response)) {
                break;
            }
            System.out.println("[" + queryName + "] WARN — OTP not accepted (attempt " + attempt
                    + "): transcribed [" + transcribed + "], bot [" + response + "] — retrying...");
        }

        return response;
    }

    /** Speaks a follow-up, retrying up to 3 times while nothing is heard or {@code stillWaiting} is
     * true for the response. */
    private String speakSingleFollowUp(String queryName, String query, HomePage homePage, String phrase, String logLabel,
            Predicate<String> stillWaiting) throws Exception {
        String response = "";

        for (int attempt = 1; attempt <= 3; attempt++) {
            response = reestablishIntentIfSessionEnded(homePage, queryName, query, response);
            // Excludes a still-broken context-lost fallback — same reasoning as answerOtpPrompt()
            // above: a fallback here means recovery hasn't actually succeeded, so keep retrying
            // rather than returning it to the caller as a resolved stage.
            if (!response.isEmpty() && !stillWaiting.test(response) && !isContextLostFallback(response)) {
                // Session dropped and the resend either resolved this stage a different way or
                // rewound to an earlier one — either way, the caller's outer loop re-checks from
                // here rather than this method blindly re-speaking a now-stale follow-up phrase.
                return response;
            }

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

            // A context-lost fallback response would make stillWaiting.test(...) return false too
            // (it's neither the awaited prompt nor a real advance), so it must be excluded here as
            // well — otherwise this breaks out treating the fallback as if the flow had advanced,
            // never reaching a later attempt where reestablishIntentIfSessionEnded() recovers it.
            // Same reasoning excludes a blank response — see answerOtpPrompt's identical fix for
            // why "hasn't responded yet" was being treated as "resolved, return this as final."
            if (!response.isBlank() && !stillWaiting.test(response) && !isContextLostFallback(response)) {
                break;
            }
            System.out.println("[" + queryName + "] " + logLabel + " — bot did not advance (attempt "
                    + attempt + ") — retrying...");
        }

        return response;
    }
}
