package com.voicebanking.report;

public class TestResult {

    public enum Status { PASSED, FAILED, SKIPPED }

    String className;
    String fullName;
    String methodName;
    String paramLabel;
    double durationSeconds;
    Status status;
    String failureMessage;
    String failureType;
    String stackTrace;
    String screenshotDataUri;

    String simpleClassName() {
        int dot = className.lastIndexOf('.');
        return dot == -1 ? className : className.substring(dot + 1);
    }

    String displayName() {
        return paramLabel != null ? methodName + " — " + paramLabel : methodName;
    }

    /** There's no existing "module" concept in this codebase — @Test groups (smoke/regression/
     * api/ui/...) are execution tags, not business features — so this buckets by keyword against
     * the test class name itself. Order matters: more specific keywords are checked first (e.g.
     * "Loan" before the generic "Account") so a class like GetLoanStatement lands in Loans, not
     * Accounts. Update this if a new test class doesn't fit an existing bucket. */
    String module() {
        String name = simpleClassName();
        if (name.contains("Loan")) return "Loans";
        if (name.contains("Transfer")) return "Transfers";
        if (name.contains("Transaction")) return "Transactions";
        if (name.contains("Beneficiar")) return "Beneficiaries";
        if (name.contains("Multilingual")) return "Multilingual";
        if (name.contains("Balance") || name.contains("Account")) return "Accounts";
        if (name.contains("CustomerInfo")) return "Customer";
        if (name.contains("HomePage")) return "Home";
        if (name.contains("Otp") || name.contains("Login") || name.contains("Welcome")
                || name.contains("Registration") || name.contains("Language")) return "Onboarding & Auth";
        return "Other";
    }

    /** Buckets failures into a small set of human-readable reasons via keyword matching against
     * the failure type/message — the same "no annotation needed" approach as module(). Order
     * matters: checks go from most specific signal to most generic so, e.g., a timed-out
     * Playwright wait triggered by the app dropping the voice session doesn't get misfiled as a
     * plain "Timeout" ahead of the more specific assertion messages this codebase's own tests
     * raise for that (see BaseVoiceTest/UI12's "Home page should remain visible" / "Bot did not
     * respond" assertions). Returns null for non-failures. Update this if a new failure shape
     * doesn't fit an existing bucket. */
    String failureCategory() {
        if (status != Status.FAILED) return null;
        String type = failureType != null ? failureType : "";
        String msg = failureMessage != null ? failureMessage : "";
        String combined = (type + " " + msg).toLowerCase();

        if (combined.contains("home page should remain visible")) return "Session Dropped";
        if (combined.contains("bot did not respond")) return "No Bot Response";
        if (combined.contains("speech-to-text not detected") || combined.contains("voice not recognised")) {
            return "Speech Not Recognised";
        }
        if (combined.contains("transcription mismatch")) return "Transcription Mismatch";
        if (combined.contains("did not match expected pattern") || combined.contains("not relevant")) {
            return "Assertion Mismatch";
        }
        if (combined.contains("connect") && combined.contains("timed out")) return "Network";
        if (combined.contains("timeout")) return "Timeout";
        if (combined.contains("assertionerror") || combined.contains("assertion")) return "Assertion Mismatch";
        return "Other";
    }
}
