package com.voicebanking.DataText;

public class VoiceQueries {

    public static class English {

        public static final String LOCALE = "en";

        // --- Account Balance ---
        public static final String ACCOUNT_BALANCE              = "What is my account balance";
        public static final String BALANCE_SHORT                = "What is my balance";
        public static final String SAVINGS_BALANCE              = "What is my savings account balance";
        public static final String CURRENT_BALANCE              = "What is my current account balance";
        public static final String HOW_MUCH_MONEY               = "How much money do I have";
        public static final String MONEY_IN_ACCOUNT             = "How much money is there in my account";
        public static final String SHOW_ACCOUNT_BALANCE         = "Show me my account balance";
        public static final String TELL_ACCOUNT_BALANCE         = "Tell me my account balance";
        public static final String CAN_TELL_BALANCE             = "Can you tell me my balance";
        public static final String PLEASE_CHECK_BALANCE         = "Please check my balance";
        public static final String CHECK_ACCOUNT_BALANCE        = "Check my account balance";
        public static final String SHOW_MY_BALANCE              = "Show my balance";
        public static final String BALANCE_AVAILABLE            = "How much balance is available in my account";
        public static final String AVAILABLE_BALANCE_IN_ACCOUNT = "What is the available balance in my account";
        public static final String AMOUNT_LEFT                  = "How much amount is left in my account";
        public static final String CASH_IN_ACCOUNT              = "How much cash do I have in my account";
        public static final String AMOUNT_IN_ACCOUNT            = "Whats the amount in my account";
        public static final String SAVINGS_BALANCE_TELL         = "Tell me the balance in my savings account";
        public static final String CURRENT_BALANCE_TELL         = "Tell me the balance in my current account";
        public static final String SAVINGS_BALANCE_OF           = "What is the balance of my savings account";
        public static final String CURRENT_BALANCE_OF           = "What is the balance of my current account";
        public static final String ENOUGH_MONEY                 = "Do I have enough money in my account";
        public static final String CAN_KNOW_BALANCE             = "Can I know my account balance";
        public static final String WANT_TO_KNOW_BALANCE         = "I want to know my balance";
        public static final String GET_ACCOUNT_BALANCE          = "Get my account balance";
        public static final String READ_OUT_BALANCE             = "Read out my balance";
        public static final String ACCESS_ACCOUNT_BALANCE       = "Access my account balance";
        public static final String FETCH_ACCOUNT_BALANCE        = "Fetch my account balance";
        public static final String BALANCE_IN_ACCOUNT           = "Balance in my account";
        public static final String ACCOUNT_BALANCE_QUERY        = "Account balance";
        public static final String BALANCE_QUERY                = "Balance";
        public static final String SAVINGS_BALANCE_QUERY        = "Savings balance";
        public static final String CURRENT_ACCOUNT_BALANCE_QUERY = "Current account balance";
        public static final String MY_BALANCE                   = "My balance";
        public static final String AVAILABLE_BALANCE            = "Available balance";
        public static final String REMAINING_BALANCE            = "Remaining balance";
        public static final String CHECK_BALANCE                = "Check balance";
        public static final String SHOW_BALANCE                 = "Show balance";
        public static final String NEED_BALANCE                 = "Need my balance";
        public static final String REMAINING_AMOUNT             = "Remaining amount";
        public static final String FUNDS_AVAILABLE              = "Funds available";
        public static final String AMOUNT_AVAILABLE             = "Amount available";
        public static final String BALANCE_CHECK                = "Balance check";
        public static final String ACCOUNT_BALANCE_CHECK        = "Account balance check";

        // --- Transactions ---
        // Placeholders from the original request ({date}, {expense name}, {merchant name}, {person name})
        // are replaced with concrete example values below, since TTS needs literal spoken text.
        // The "₹" symbol is spelled out as "rupees" since TTS cannot reliably pronounce it.
        public static final String RECENT_TRANSACTIONS          = "Show me my recent transactions";
        public static final String WANT_RECENT_TRANSACTIONS     = "I want to see my recent transactions";
        public static final String CAN_SEE_RECENT_TRANSACTIONS  = "Can I see my recent transactions";
        public static final String LATEST_TRANSACTION           = "Show me my latest transaction";
        public static final String LAST_TRANSACTION             = "Show me my last transaction";
        public static final String WHAT_WAS_LAST_TRANSACTION    = "What was my last transaction";
        public static final String TODAYS_TRANSACTIONS          = "Show me todays transactions";
        public static final String YESTERDAYS_TRANSACTIONS      = "Show me yesterdays transactions";
        public static final String THIS_WEEK_TRANSACTIONS       = "Show me transactions from this week";
        public static final String LAST_WEEK_TRANSACTIONS       = "Show me transactions from last week";
        public static final String THIS_MONTH_TRANSACTIONS      = "Show me transactions from this month";
        public static final String LAST_MONTH_TRANSACTIONS      = "Show me transactions from last month";
        public static final String THIS_YEAR_TRANSACTIONS       = "Show me transactions from this year";
        public static final String LAST_YEAR_TRANSACTIONS       = "Show me transactions from last year";
        public static final String TRANSACTIONS_JUNE_RANGE      = "Show me transactions from 1 June to 30 June";
        public static final String TRANSACTIONS_JULY_RANGE      = "Show me transactions from 5 July to 20 July";
        public static final String TRANSACTIONS_AFTER_DATE      = "Show me transactions after 1 June";
        public static final String TRANSACTIONS_BEFORE_DATE     = "Show me transactions before 30 June";
        public static final String SPEND_ON_GROCERIES           = "How much money did I spend on groceries";
        public static final String ALL_DEBIT_TRANSACTIONS       = "Show me all debit transactions";
        public static final String ALL_CREDIT_TRANSACTIONS      = "Show me all credit transactions";
        public static final String MONEY_CREDITED                = "Show me money credited to my account";
        public static final String MONEY_DEBITED                 = "Show me money debited from my account";
        public static final String TRANSACTIONS_ABOVE_AMOUNT     = "Show me transactions above 10000 rupees";
        public static final String TRANSACTIONS_BELOW_AMOUNT     = "Show me transactions below 500 rupees";
        public static final String SAVINGS_TRANSACTIONS          = "Show me transactions from my savings account";
        public static final String CURRENT_TRANSACTIONS          = "Show me transactions from my current account";
        public static final String UPI_TRANSACTIONS              = "Show me UPI transactions";
        public static final String ATM_TRANSACTIONS              = "Show me ATM transactions";
        public static final String CARD_TRANSACTIONS             = "Show me card transactions";
        public static final String TRANSACTIONS_TO_MERCHANT      = "Show me transactions made to Amazon";
        public static final String TRANSACTIONS_FROM_PERSON      = "Show me transactions received from Rohit";
        public static final String TRANSACTIONS_MADE_TODAY       = "What transactions did I make today";
        public static final String SPEND_THIS_MONTH              = "What did I spend money on this month";
        public static final String TRANSACTION_HISTORY           = "Show my transaction history";
        public static final String READ_OUT_RECENT_TRANSACTIONS  = "Read out my recent transactions";

        // --- Loans — real loan types ---
        // Confirmed for Customer A (Rohit Mehta, CIF202602260005, 9898989898): Home Loan (LN10005,
        // active) and Education Loan (LN20005, closed). Used for the active trial rows in
        // UI9_LoanInquiryTest. Constant names below still say "_PERSONAL" for historical reasons —
        // only their spoken text changed to "education loan" — renaming the identifiers would churn
        // every reference in UI9_LoanInquiryTest for no behavioral benefit.
        public static final String LOAN_TYPE_DETAILS_HOME       = "Give me details of my home loan";
        public static final String LOAN_TYPE_EMI_HOME           = "What is the EMI on my home loan";
        public static final String LOAN_TYPE_INTEREST_HOME      = "What is the interest rate on my home loan";
        public static final String LOAN_TYPE_OUTSTANDING_HOME   = "What is the outstanding amount on my home loan";
        public static final String LOAN_TYPE_STATUS_HOME        = "What is the status of my home loan";

        public static final String LOAN_TYPE_EMI_PERSONAL          = "What is the EMI on my education loan";
        public static final String LOAN_TYPE_INTEREST_PERSONAL     = "What is the interest rate on my education loan";
        public static final String LOAN_TYPE_OUTSTANDING_PERSONAL  = "What is the outstanding amount on my education loan";
        public static final String LOAN_TYPE_NEXT_EMI_DUE_PERSONAL = "When is my next EMI due on my education loan";

        // --- Loans — full phrasing bank ---
        // {loan type} is replaced with "home loan" or "education loan" (alternating, for coverage
        // of both real loans) — except LOAN_TYPE_DETAILS and LOAN_TYPE_EMI_WHAT, deliberately left
        // as "car loan" (a type this customer does NOT have) to exercise the bot's fallback
        // (BotResponsePatterns.Loans.LOAN_OPTIONS_PROMPT) as a negative test. LOAN_TYPE_INTEREST_WHAT
        // and LOAN_TYPE_OUTSTANDING_WHAT are left unused/orphaned — their phrasing is already covered
        // verbatim by the dedicated _HOME/_PERSONAL constants above, so assigning them a loan type
        // would just duplicate an existing row.
        public static final String LOAN_DETAILS                    = "Give me my loan details";
        public static final String LOAN_ACCOUNTS_TELL               = "Tell me about my loan accounts";
        public static final String LOAN_ACCOUNTS_WHAT               = "What loan accounts do I have";
        public static final String LOAN_ACCOUNTS_SHOW               = "Show me my loan accounts";
        public static final String LOANS_WHAT                       = "What loans do I have";
        public static final String ACTIVE_LOANS_ANY                 = "Do I have any active loans";
        public static final String LOAN_TYPE_DETAILS                = "Give me details of my car loan";
        public static final String LOAN_TYPE_TELL                   = "Tell me about my home loan";
        public static final String LOAN_TYPE_WHAT_CAN_TELL          = "What can you tell me about my education loan";
        public static final String LOAN_TYPE_SHOW_DETAILS           = "Show details of my home loan";
        public static final String LOAN_TYPE_EMI_WHAT               = "What is the EMI on my car loan";
        public static final String LOAN_TYPE_EMI_TELL               = "Tell me the EMI for my education loan";
        public static final String LOAN_TYPE_EMI_WANT               = "I want to know the EMI of my home loan";
        public static final String LOAN_TYPE_EMI_HOW_MUCH           = "How much EMI am I paying for my education loan";
        public static final String LOAN_TYPE_INTEREST_WHAT          = "What is the interest rate on my car loan";
        public static final String LOAN_TYPE_INTEREST_TELL          = "Tell me the interest rate of my home loan";
        public static final String LOAN_TYPE_INTEREST_WANT          = "I want to know the interest rate of my education loan";
        public static final String LOAN_TYPE_TENURE_WHAT            = "What is the tenure of my home loan";
        public static final String LOAN_TYPE_TENURE_FOR             = "What is the tenure for my education loan";
        public static final String LOAN_TYPE_TENURE_WANT            = "I want to know the tenure of my home loan";
        public static final String LOAN_TYPE_TENURE_REMAINING       = "Which is the remaining tenure on my education loan";
        public static final String LOAN_TYPE_EMIS_REMAINING         = "How many EMIs are remaining on my home loan";
        public static final String LOAN_TYPE_OUTSTANDING_WHAT       = "What is the outstanding amount on my car loan";
        public static final String LOAN_TYPE_OUTSTANDING_TELL       = "Tell me the outstanding amount on my education loan";
        public static final String LOAN_TYPE_LEFT_TO_PAY            = "How much is left to pay on my home loan";
        public static final String LOAN_TYPE_REMAINING_BALANCE      = "What is the remaining balance on my education loan";
        public static final String LOAN_TYPE_SANCTIONED_HOW_MUCH    = "How much money was sanctioned for my home loan";
        public static final String LOAN_TYPE_SANCTIONED_WHAT        = "What is the sanctioned amount for my education loan";
        public static final String LOAN_TYPE_TOTAL_AMOUNT           = "What is the total loan amount on my home loan";
        public static final String LOAN_AMOUNT_WAS                  = "What was my loan amount";
        public static final String LOAN_TYPE_NEXT_EMI_DUE           = "When is my next EMI due on my home loan";
        public static final String NEXT_EMI_DUE_DATE                = "What is the due date for my next EMI";
        public static final String NEXT_EMI_PAY_WHEN                = "When do I need to pay my next EMI";
        public static final String LATEST_EMI_PAID                  = "Has my latest EMI been paid";
        public static final String LOAN_TYPE_PAYMENT_STATUS         = "Show me the payment status of my education loan";
        public static final String LOAN_ACTIVE_IS                   = "Is my loan active";
        public static final String LOAN_TYPE_STATUS                 = "What is the status of my education loan";
        public static final String LOANS_TELL_ABOUT                 = "Tell me about my loans";
        public static final String ACTIVE_LOANS_SHOW                = "Show my active loans";
        public static final String LOANS_RUNNING_WHICH              = "Which loans are running in my name";
        public static final String LOANS_CURRENTLY_PAYING           = "What loans am I currently paying for";
        public static final String EMI_HOW_MUCH                     = "How much is my EMI";
        public static final String MONTHLY_INSTALLMENT_WHAT         = "What is my monthly installment";
        public static final String LOAN_TYPE_PAY_EVERY_MONTH        = "How much do I pay every month for my home loan";
        public static final String PAY_EVERY_MONTH_WHAT             = "What am I paying every month";
        public static final String LOAN_TYPE_INTEREST_GENERIC       = "What is the interest on my education loan";
        public static final String INTEREST_HOW_MUCH                = "How much interest am I paying";
        public static final String LOAN_INTEREST_RATE_MY            = "What is my loan interest rate";
        public static final String STILL_OWE_HOW_MUCH               = "How much do I still owe";
        public static final String LOAN_LEFT_HOW_MUCH               = "How much loan is left";
        public static final String LOAN_TYPE_AMOUNT_PENDING         = "What amount is pending on my home loan";
        public static final String REMAINING_LOAN_BALANCE_MY        = "What is my remaining loan balance";
        public static final String EMIS_LEFT_HOW_MANY               = "How many EMIs are left";
        public static final String INSTALLMENTS_REMAINING_HOW_MANY  = "How many installments are remaining";
        public static final String MONTHS_LEFT_LOAN                 = "How many months are left on my loan";
        public static final String LOAN_END_WHEN                    = "When will my loan end";
        public static final String REMAINING_TENURE_LOAN            = "What is the remaining tenure on my loan";
        public static final String STILL_HAVE_TO_PAY_HOW_LONG       = "How long do I still have to pay";
        public static final String LOAN_TAKEN_HOW_MUCH              = "How much loan did I take";
        public static final String SANCTIONED_AMOUNT_WAS            = "What was the sanctioned amount";
        public static final String LOAN_TYPE_APPROVED_HOW_MUCH      = "How much was approved for my education loan";
        public static final String TOTAL_LOAN_AMOUNT_WHAT           = "What is the total loan amount";
        public static final String NEXT_EMI_DUE_WHEN                = "When is my next EMI due";
        public static final String NEXT_EMI_DATE_WHAT               = "What is my next EMI date";
        public static final String PAYMENT_DUE_WHEN                 = "When is the payment due";
        public static final String INSTALLMENT_DUE_WHEN             = "When is my installment due";
        public static final String LOAN_STATUS_WHAT                 = "What is the status of my loan";
        public static final String LOAN_STILL_RUNNING               = "Is my loan still running";
        public static final String LOAN_CLOSED_HAS                  = "Has my loan been closed";

        // --- Transfer Money ---
        // {amount} / {beneficiary name} replaced with "one rupee" / "Pooja Nair" — Pooja Nair is
        // a real, confirmed beneficiary on Rohit Mehta's SAVINGS account (CIF202602260005),
        // matching the "Send Money From Savings" row below. Suresh Patel (also a real beneficiary
        // of Rohit's, but on his CURRENT account) was tried first and technically worked since the
        // bot's beneficiary listing turned out to be customer-wide rather than account-scoped, but
        // that's a real savings/current mismatch for that one row regardless of whether the bot
        // enforces it — Pooja Nair keeps every row internally consistent with the account it names.
        // ₹1 is used as a safe/small amount rather than an arbitrary new value. Spelled out as "one"
        // rather than the digit "1" — TTS pronounces "1" as the word "one," so STT transcribes it
        // back as "one" too; using the digit in the expected text made a perfectly correct
        // transcription fail the word-match check every time, since "1" never literally appears in
        // "...one rupee...".
        public static final String CAN_TRANSFER_MONEY            = "Can I transfer money";
        public static final String TRANSFER_MONEY_SHORT          = "Transfer money";
        public static final String SEND_MONEY_FROM_SAVINGS       = "Send money from my savings account";
        public static final String TRANSFER_AMOUNT_TO_BENEFICIARY = "Transfer one rupee to Pooja Nair";
        public static final String SEND_AMOUNT_TO_BENEFICIARY     = "Send one rupee to Pooja Nair";
        public static final String PAY_AMOUNT_TO_BENEFICIARY      = "Pay one rupee to Pooja Nair";
        public static final String TRANSFER_USING_UPI             = "Send money using UPI";
        public static final String MAKE_TRANSFER_TO_BENEFICIARY   = "Make a transfer to Pooja Nair";

        // Follow-up utterance spoken after the bot asks to confirm a pending transfer. The bot
        // requires an exact scripted phrase (confirmed live: it echoes back "To authorize this
        // transfer, please speak the following phrase clearly: 'Yes. I confirm the transfer of
        // 1.0 rupees to <beneficiary> from my account'."), so CONFIRM_THE_TRANSFER carries that
        // wording with Pooja Nair as the beneficiary — but with a comma instead of the period
        // after "Yes": a period made edge-tts insert a full sentence-break pause there, long
        // enough that the bot's STT/VAD split it into two separate messages ("Yes" arriving on
        // its own, "I confirm..." arriving after) instead of one continuous utterance, so the
        // scripted phrase was never recognized as a whole and the transfer got rejected. A comma
        // keeps the same wording but only introduces a brief natural pause, not a full stop.
        public static final String CONFIRM_THE_TRANSFER = "Yes, I confirm the transfer of 1.0 rupees to Pooja Nair from my account";

        // Spoken when the bot asks "how much money would you like to transfer?" after a
        // beneficiary is resolved from a leftover/unnamed-amount state — matches the ₹1 amount
        // used everywhere else in this section.
        public static final String TRANSFER_AMOUNT_FOLLOWUP = "one rupee";

        // The fixed test OTP (123456), spoken as individual digit words rather than the number
        // itself — the bot asks for it "one number at a time," and TTS would otherwise read
        // "123456" as one large number instead of six separate digits. Six distinct digits
        // (rather than the same digit repeated six times) transcribe far more reliably: STT kept
        // merging/splitting the identical repeated "one one one one one one" into 5 or 7 digits
        // instead of exactly 6, causing the bot to reject it every attempt and auto-cancel the
        // transfer via its own OTP-attempt limit.
        public static final String TEST_OTP_SPOKEN = "one two three four five six";

        // --- Voice Registration / Enrollment ---
        // Spoken during voice-registration enrollment (UI11); the app builds the speaker's
        // voiceprint from these recordings and later checks whether a query's voice matches it
        // before treating the caller as authorized. The enrollment screen records for a fixed
        // ~15s window regardless of how long the spoken audio actually is — a short phrase (a
        // few seconds of speech, padded out with silence) left most of that window silent and
        // the app's quality check rejected it consistently. This is deliberately long enough to
        // speak continuously for close to the full 15s window instead of relying on silence padding.
        public static final String VOICE_ENROLLMENT_PHRASE =
                "My voice is my password, please verify me. I am registering my voice with this "
                + "banking application so that I can securely access my account, check my "
                + "balances, and complete transactions using natural spoken commands whenever I "
                + "need to bank on the go";
    }
}
