package com.voicebanking.DataText;

/**
 * Hindi and Bengali voice queries for the multilingual trial — kept separate
 * from {@link VoiceQueries} (English-only) so the existing English test data
 * and flow are untouched.
 */
public class MultilingualVoiceQueries {

    public static class Hindi {

        public static final String LOCALE = "hi";

        public static final String SAVINGS_BALANCE = "मेरे सेविंग्स अकाउंट में कितना बैलेंस है?";
        public static final String TRANSACTION_LIST_SAVINGS = "मुझे मेरे सेविंग्स अकाउंट की transaction लिस्ट दिखाएँ";
        public static final String TRANSFER_TO_BENEFICIARY = "मेरे बेनिफ़िशियरी पूजा देसाई को 1 रुपया ट्रांसफर करें";
        public static final String LOAN_ACCOUNT_SAVINGS = "मेरे सेविंग्स अकाउंट के लिए मेरा लोन अकाउंट दिखाएँ";
        public static final String EMI_STATEMENT_HOME_LOAN = "मेरे education लोन के लिए सेविंग्स अकाउंट का EMI स्टेटमेंट";
    }

    public static class Bengali {

        public static final String LOCALE = "bn";

        public static final String SAVINGS_BALANCE = "আমার সেভিংস অ্যাকাউন্টে কত টাকা আছে?";
        public static final String TRANSACTION_LIST_SAVINGS = "আমার সেভিংস অ্যাকাউন্টের লেনদেনের তালিকা দেখান";
        public static final String TRANSFER_TO_BENEFICIARY = "আমার সুবিধাভোগী পূজা দেশাই-এর অ্যাকাউন্টে ১ রুপি স্থানান্তর করুন";
        public static final String LOAN_ACCOUNT_SAVINGS = "আমার সেভিংস অ্যাকাউন্টের বিপরীতে থাকা লোন অ্যাকাউন্টটি দেখান";
        public static final String EMI_STATEMENT_HOME_LOAN = "আমার সেভিংস অ্যাকাউন্টের home লোনের ইএমআই (EMI) স্টেটমেন্ট";
    }
}
