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
}
