package com.voicebanking.DataText;

public class BotResponsePatterns {

    public static class Balance {
        // "The balance in your SAVINGS (ACC...) account is 67000.0"
        // Fixed: "The balance in your … account is"
        // Dynamic: account type, account number, amount
        public static final String ANY =
                "The balance in your [A-Z]+ \\([A-Z0-9]+\\) account is [\\d,]+(?:\\.\\d+)?";
        public static final String SAVINGS =
                "The balance in your SAVINGS \\([A-Z0-9]+\\) account is [\\d,]+(?:\\.\\d+)?";
        public static final String CURRENT =
                "The balance in your CURRENT \\([A-Z0-9]+\\) account is [\\d,]+(?:\\.\\d+)?";
    }

    public static class Transactions {
        // One transaction entry renders as a structured card (nested <div>s), not a plain text
        // bubble — Locator.textContent() concatenates nested block elements with NO separator
        // (no space, no newline). Raw text for one entry looks like:
        //   "Mobile Phone EMI | UPIDEBIT • Feb 09, 2026, 07:55 AM₹1000.00"
        // The description ("Mobile Phone EMI | UPI") is too free-form to anchor on, so this only
        // matches the fixed part: "DEBIT|CREDIT • <Mon> <D>, <YYYY>, <hh>:<mm> AM|PM" immediately
        // followed by "₹<amount>".
        public static final String ENTRY =
                "(?:DEBIT|CREDIT) • [A-Za-z]{3} \\d{1,2}, \\d{4}, \\d{2}:\\d{2} (?:AM|PM)₹[\\d,]+(?:\\.\\d+)?";

        // Category-filtered queries (UPI, card, all-credit, all-debit) sometimes answer with a
        // plain-sentence summary instead of the card format — same data, different shape:
        //   "Here are your upi transactions for ACC..., 6 transactions found: 1010 on 2026-02-12 via ..."
        public static final String SUMMARY =
                "\\d+ transactions? found: [\\d,]+(?:\\.\\d+)? on \\d{4}-\\d{2}-\\d{2} via .+";

        // A legitimate "nothing matched" response — e.g. no ATM transactions in the account's
        // history, or no transactions dated today/yesterday in the seed data. Not a failure on
        // its own; only meaningful when the query genuinely has no matching data to return.
        public static final String NO_RESULTS =
                "I couldn't find any [\\w\\s]*transactions? for [A-Z0-9]+ (?:in the selected period|matching your request)";

        // UPI / card / all-credit: accept either the structured card or the prose summary.
        public static final String ENTRY_OR_SUMMARY = "(?:" + ENTRY + ")|(?:" + SUMMARY + ")";

        // ATM / today / yesterday / made-today / groceries: accept the card, or a legitimate
        // empty result for a filter/date range this seed account has nothing in.
        public static final String ENTRY_OR_NO_RESULTS = "(?:" + ENTRY + ")|(?:" + NO_RESULTS + ")";
    }
}
