package com.voicebanking.DataText;

import java.util.regex.Pattern;

public class BotResponsePatterns {

    public static class Balance {
        // "The balance in your SAVINGS (ACC...) account is 67000.0" — but the account-number
        // segment isn't always there; stage has been observed replying "The balance in your
        // SAVINGS account is 67000.0" with no parenthesised account number at all for the exact
        // same query. Not under this suite's control, so the segment is optional rather than
        // required.
        // Fixed: "The balance in your … account is"
        // Dynamic: account type, optional account number, amount
        public static final String ANY =
                "The balance in your [A-Z]+ (?:\\([A-Z0-9]+\\) )?account is [\\d,]+(?:\\.\\d+)?";
        public static final String SAVINGS =
                "The balance in your SAVINGS (?:\\([A-Z0-9]+\\) )?account is [\\d,]+(?:\\.\\d+)?";
        public static final String CURRENT =
                "The balance in your CURRENT (?:\\([A-Z0-9]+\\) )?account is [\\d,]+(?:\\.\\d+)?";

        // Capturing counterpart of ANY/SAVINGS/CURRENT above, for extracting the spoken balance
        // out of an already-matched response so it can be cross-checked against the ground-truth
        // Account Balance API (see UI7_BalanceInquiryTest#testKnownAccountBalance) — not used for
        // pass/fail shape matching itself.
        public static final Pattern VALUE =
                Pattern.compile("The balance in your [A-Z]+ (?:\\([A-Z0-9]+\\) )?account is ([\\d,]+(?:\\.\\d+)?)");
    }

    public static class Transactions {
        // One transaction entry renders as a structured card (nested <div>s), not a plain text
        // bubble — Locator.textContent() concatenates nested block elements with NO separator
        // (no space, no newline). Raw text for one entry looks like:
        //   "Transfer to Suresh Patel-₹1.00DEBIT • 23 Jul 2026, 12:00 am"
        //   "Refund | Swiggy | ref=REF20260210+₹350.00CREDIT • 10 Feb 2026, 12:00 am"
        // The description ("Transfer to Suresh Patel") is too free-form to anchor on, so this only
        // matches the fixed part: "+|-₹<amount>DEBIT|CREDIT • <D> <Mon> <YYYY>, <hh>:<mm> AM|PM".
        // An earlier version of this pattern assumed the amount trailed the timestamp instead
        // (amount-after-AM/PM, Month-Day date order) — that never actually matched any response
        // observed live across many runs; sign+amount+type consistently comes immediately before
        // the date, and the date order is Day-Month-Year, not Month-Day. AM/PM casing isn't
        // consistent either (stage has replied with lowercase "am"/"pm" for the same query type),
        // so that group is matched case-insensitively.
        public static final String ENTRY =
                "[+-]₹[\\d,]+(?:\\.\\d+)?(?:DEBIT|CREDIT) • \\d{1,2} [A-Za-z]{3} \\d{4}, \\d{2}:\\d{2} (?i:AM|PM)";

        // "Recent transactions" phrasings render a "Recent Transactions" header div before the
        // entry list — confirmed live, e.g.:
        //   "Recent TransactionsMobile Phone EMI | UPI-₹1,000.00DEBIT • 09 Feb 2026, 07:55 am..."
        // Requiring the header (not just a bare ENTRY match) confirms the bot actually answered a
        // recency query rather than returning some other transaction card that happens to match
        // ENTRY's shape — plain ENTRY is too generic on its own for these rows.
        public static final String RECENT_ENTRY =
                "Recent Transactions.*?" + ENTRY;

        // "Latest transaction" queries render a "Latest Transaction" header div (singular) before
        // the single entry — confirmed live, e.g.:
        //   "Latest TransactionMobile Phone EMI | UPI-₹1,000.00DEBIT • 09 Feb 2026, 07:55 am..."
        // Same rationale as RECENT_ENTRY: anchors on the header so a bare ENTRY-shaped card
        // returned for the wrong reason doesn't pass.
        public static final String LATEST_ENTRY =
                "Latest Transaction.*?" + ENTRY;

        // Category-filtered queries (UPI, card, all-credit, all-debit) sometimes answer with a
        // plain-sentence summary instead of the card format — same data, different shape:
        //   "Here are your upi transactions for ACC..., 6 transactions found: 1010 on 2026-02-12 via ..."
        public static final String SUMMARY =
                "\\d+ transactions? found: [\\d,]+(?:\\.\\d+)? on \\d{4}-\\d{2}-\\d{2} via .+";

        // A legitimate "nothing matched" response — e.g. no ATM transactions in the account's
        // history, or no transactions dated today/yesterday in the seed data. Not a failure on
        // its own; only meaningful when the query genuinely has no matching data to return.
        // Multiple phrasings observed live for the same underlying "nothing found" case: the
        // account-ID segment before "in the selected period" / "matching your request" is
        // sometimes present ("...for ACC... in the selected period") and sometimes just missing
        // ("...for in the selected period") — so it's optional, not required. A third phrasing
        // echoes the resolved date range instead ("...for from 2026-08-03 to 2026-08-03.").
        public static final String NO_RESULTS =
                "I couldn't find any [\\w\\s]*transactions? for "
                + "(?:(?:[A-Z0-9]+ )?(?:in the selected period|matching your request)"
                + "|from \\d{4}-\\d{2}-\\d{2} to \\d{4}-\\d{2}-\\d{2})";

        // UPI / card / all-credit: accept either the structured card or the prose summary.
        public static final String ENTRY_OR_SUMMARY = "(?:" + ENTRY + ")|(?:" + SUMMARY + ")";

        // ATM / today / yesterday / made-today / groceries: accept the card, or a legitimate
        // empty result for a filter/date range this seed account has nothing in.
        public static final String ENTRY_OR_NO_RESULTS = "(?:" + ENTRY + ")|(?:" + NO_RESULTS + ")";
    }

    public static class Authorization {
        // Confirmed live (UI11): a balance query spoken in a voice other than the one registered
        // gets the exact response "Not authorized". The other phrasings are kept as fallback
        // coverage in case wording varies by scenario/build, but "not authoriz" is the one
        // observed in practice.
        public static final String VOICE_NOT_RECOGNIZED =
                "(?i)(not authoriz|could not verify|couldn't verify|voice (does not|doesn't) match|"
                + "voice not recogni[sz]ed|unable to authenticate|access denied|verification failed)";
    }

    public static class Loans {
        // Confirmed for Customer A (Rohit Mehta, 9898989898): Home Loan (LN10005, active),
        // Education Loan (LN20005, closed). Personal Loan confirmed for Customer C (Aniket More,
        // 9812341041, LN10041, active) — same phrasing pattern as Home/Education throughout this
        // class, just a third loan-type name, so every (?:Home|Education|Personal) alternation below
        // includes it too.
        // "The outstanding amount on your Home Loan is Rs.800000.0"
        public static final String OUTSTANDING =
                "The outstanding amount on your (?:Home|Education|Personal) Loan is Rs\\.[\\d,]+(?:\\.\\d+)?";

        // "The next EMI due date for your Education Loan is N/A." — "N/A" is a legitimate value
        // when no due date is scheduled, so the date portion is left loose on purpose.
        public static final String NEXT_EMI_DUE =
                "The next EMI due date for your (?:Home|Education|Personal) Loan is .+";

        // Precise per-category patterns confirmed live for the "Give me details of my home loan"
        // flow (see LOAN_DETAIL_OPTIONS_PROMPT below) — each answers a single-word/short-phrase
        // follow-up ("EMI", "Tenure", "Pending tenure", "Interest rate") asked after that prompt.
        // Only the numeric value itself varies per account; the wording is fixed.
        // "Your EMI for Home Loan is Rs.18000."
        public static final String EMI_AMOUNT =
                "Your EMI for (?:Home|Education|Personal) Loan is Rs\\.[\\d,]+(?:\\.\\d+)?";
        // "Your Home Loan has a tenure of 240 months."
        public static final String TENURE =
                "Your (?:Home|Education|Personal) Loan has a tenure of \\d+ months?";
        // "The remaining tenure for your Home Loan is 5 months."
        public static final String PENDING_TENURE =
                "The remaining tenure for your (?:Home|Education|Personal) Loan is \\d+ months?";
        // "The interest rate on your Home Loan is 7.8%."
        public static final String INTEREST_RATE =
                "The interest rate on your (?:Home|Education|Personal) Loan is [\\d.]+%";
        // "The sanctioned amount for your Home Loan is Rs.900000.0." — confirmed live. The
        // follow-up category is spoken as "Loan amount", but the bot answers with "sanctioned
        // amount" wording instead — an earlier guess assumed it would echo "loan amount" back
        // (matching OUTSTANDING's "outstanding amount" self-echo pattern), which was wrong.
        public static final String LOAN_AMOUNT =
                "The sanctioned amount for your (?:Home|Education|Personal) Loan is Rs\\.[\\d,]+(?:\\.\\d+)?";

        // Asked when the loan type is already known (e.g. "Give me details of my home loan") but
        // no specific detail was named — lists every category the bot can answer about that loan
        // and waits for a follow-up naming one. Confirmed live, exact phrasing observed:
        // "What would you like to know about your Home Loan?I can tell you the EMI, tenure.pending
        // tenure, interest rate, outstanding amount, loan amount.or next EMI due date." — the odd
        // "tenure.pending"/"amount.or" joins are the app's own card-style rendering running
        // adjacent sentences together with no space (same phenomenon as the transaction-card text
        // concatenation elsewhere in this file), not something to anchor on, so this only checks
        // the fixed lead-in.
        public static final String LOAN_DETAIL_OPTIONS_PROMPT =
                "(?i)What would you like to know about your (?:Home|Education|Personal) Loan";

        // Fallback when the named loan type isn't recognised (e.g. "car loan", which this
        // customer doesn't have), or no type was named and the customer has more than one loan —
        // the bot lists what it actually has and asks the caller to pick. The exact wording is
        // NOT stable — at least 7 lead-in variants have been observed for the same underlying
        // prompt ("You have the following loan accounts: ...", "I found multiple loan accounts
        // for you: ...", "I see you have multiple loan accounts. Which one are you referring
        // to? ...", etc.), so this deliberately doesn't match a literal phrase. Instead it
        // requires, anywhere in the response: the word "which", the word "loan", and at least two
        // distinct loan references — either an account number (LN followed by digits) or a named
        // loan type ("Home Loan"/"Education Loan"). Originally required two LN-codes specifically,
        // but a live response instead listed loans by type name with no LN-code at all: "You have
        // the following loans: 1. (Home Loan), 2. (Education Loan). Which loan would you like to
        // check?" — the bot isn't consistent about which identifier style it lists loans by, so
        // both count.
        public static final String LOAN_OPTIONS_PROMPT =
                "(?i)(?=.*\\bwhich\\b)(?=.*\\bloan\\b)(?=(?:.*?(?:LN\\d+|(?:Home|Education|Personal) Loan)){2})";

        // Capturing-group counterparts of a few patterns above, for extracting the actual value
        // out of an already-matched response so it can be cross-checked against the ground-truth
        // Loan Summary/Overdue APIs (see UI9_LoanInquiryTest#crossCheckLoanDetail) — not used for
        // pass/fail matching itself, that's still the non-capturing patterns above.
        public static final Pattern EMI_AMOUNT_VALUE =
                Pattern.compile("Your EMI for (?:Home|Education|Personal) Loan is Rs\\.([\\d,]+(?:\\.\\d+)?)");
        public static final Pattern TENURE_VALUE =
                Pattern.compile("Your (?:Home|Education|Personal) Loan has a tenure of (\\d+) months?");
        public static final Pattern PENDING_TENURE_VALUE =
                Pattern.compile("The remaining tenure for your (?:Home|Education|Personal) Loan is (\\d+) months?");
        public static final Pattern INTEREST_RATE_VALUE =
                Pattern.compile("The interest rate on your (?:Home|Education|Personal) Loan is ([\\d.]+)%");
        public static final Pattern OUTSTANDING_VALUE =
                Pattern.compile("The outstanding amount on your (?:Home|Education|Personal) Loan is Rs\\.([\\d,]+(?:\\.\\d+)?)");
        public static final Pattern LOAN_AMOUNT_VALUE =
                Pattern.compile("The sanctioned amount for your (?:Home|Education|Personal) Loan is Rs\\.([\\d,]+(?:\\.\\d+)?)");
        public static final Pattern NEXT_EMI_DUE_VALUE =
                Pattern.compile("The next EMI due date for your (?:Home|Education|Personal) Loan is (.+?)\\.?$");
    }
}
