# Voice Banking — Test Plan

## 1. Project Overview

| Property | Value |
|---|---|
| Application | Voice Banking |
| Type | Microservices REST API + Web UI |
| Language | Java 21 |
| Build | Maven 3.8+ |
| Test Framework | TestNG |
| HTTP Client | Playwright `APIRequestContext` |
| Environments | `staging`, `prod` |
| CI/CD | Jenkins (Declarative Pipeline) |

### 1.1 Scope

**In Scope — Current (API Automation)**
- APIs 1–6, 8–10: Account, Customer, Transactions, Loans
- Smoke and Regression groups
- Automated via TestNG + Playwright Java

**In Scope — Current (UI/Voice Automation)**
- Welcome, Login/OTP, Language, Voice Registration, Home screens (UI1–UI6)
- Voice Balance Inquiry — 44-query regression suite with account disambiguation handling (UI7, `botverification` group)
- Voice Transaction History (UI8, `botverification` group)
- Voice Loan Inquiry — 76-query regression suite with which-loan and detail-category disambiguation (UI9, `botverification` group)
- Voice Money Transfer (UI10, `botverificationTransferMoney` group)
- Voice-auth: registered vs. mismatched voice (UI11)
- Multilingual voice queries (UI12, `multilingual` group)
- Playwright Chromium with `--use-file-for-fake-audio-capture` for mic simulation

**In Scope — Future (UI Automation)**
- Beneficiary add / edit UI flow
- Cross-browser UI runs

**Out of Scope**
- APIs 7, 11, 12, 13 (Loan Details, Passbook, RD List, Account Details)
- Load and stress testing
- Security penetration testing
- Mobile testing

---

## 2. Current Automation — API Layer

### 2.1 Technology Stack

| Component | Technology | Version |
|---|---|---|
| Test Framework | TestNG | 7.x |
| HTTP Client | Playwright APIRequestContext | 1.50.0 |
| JSON Parsing | Jackson Databind | 2.16.0 |
| Language | Java | 21 |
| Build | Maven | 3.8+ |

### 2.2 Test Groups

Three TestNG groups are used to control which tests run in a given pipeline execution.

| Group | Purpose | APIs Covered |
|---|---|---|
| `smoke` | Fast sanity check after any deploy — runs a small critical subset | API1, API2, API3, API6 |
| `regression` | Full API regression — all 9 APIs | All |
| `api` | Run all API tests irrespective of smoke/regression split | All |

Run a specific group:
```bash
mvn clean test -DtestGroups=smoke
mvn clean test -DtestGroups=regression
mvn clean test -DtestGroups=api
```

### 2.3 API Test Coverage

| API | Test Class | Group | Test Methods | Validates |
|---|---|---|---|---|
| 1 — Account List | API1_GetAccountListTest | smoke, regression, api | testGetAccountList, testValidateAccountDetails | Response structure, account types (SAVINGS/CURRENT), ACTIVE status |
| 2 — Customer Info | API2_GetCustomerInfoTest | smoke, regression, api | testCustomerInfoResponse, testCustomerDataValidation | Required fields, email/mobile format, KYC status |
| 3 — Account Balance | API3_GetAccountBalanceTest | smoke, regression, api | testAccountBalanceResponse, testAccountBalanceDataValidation | Balance value, masked account number, account type |
| 4 — Beneficiaries | API4_GetBeneficiariesListTest | regression, api | testBeneficiariesResponse, testBeneficiarySchemaValidation, testBeneficiaryDataValidation | Beneficiary fields, transfer types, IFSC |
| 5 — Transactions | API5_GetTransactionsListTest | regression, api | testTransactionsResponse, testTransactionListPagination, testTransactionDataValidation | Transaction schema, pagination, amounts |
| 6 — Transfer Money | API6_TransferMoneyTest | smoke, regression, api | testTransferBalanceChaining | Full transfer cycle: debit+credit+cleanup, balance reconciliation |
| 8 — Loan Statement | API8_GetLoanStatementTest | regression, api | testLoanStatementResponse, testLoanStatementDataValidation | Pagination, transaction list, EMI amounts |
| 9 — Loan Overdue | API9_GetLoanOverdueDetailsTest | regression, api | testLoanOverdueResponse, testLoanOverdueDataValidation | Overdue amounts, instalment, maturity date |
| 10 — Loan Summary | API10_GetLoanSummaryListTest | regression, api | testLoanSummaryResponse, testLoanDetailsValidation, testCommunicationAddressValidation, testLoanProductTypes | Loan types (HOME/PERSONAL), address fields, interest rates |

**Total automated test methods: 21**

### 2.4 Environments

| Environment | URL Source | Used By |
|---|---|---|
| `staging` | Jenkins global var `STAGING_API_URL` | QA validation after deploy |
| `prod` | Jenkins global var `PROD_API_URL` | Prod smoke after release |

`API_BASE_URL` env var is set by Jenkins and read by `BaseApiPage.setUp()`. Fallback is `Endpoints.BASE_URL_PROD` when running locally without the env var.

### 2.5 Key Design Decisions

| Decision | Why |
|---|---|
| `@BeforeMethod(alwaysRun = true)` in BaseApiPage | TestNG skips `@BeforeMethod` when group filtering is active unless `alwaysRun = true`; without it, `apiClient` is null and all tests NPE |
| Single `testng.xml` + Maven `-DtestGroups` | Avoids maintaining multiple XML suite files; groups live as annotations on the test methods themselves |
| Playwright `APIRequestContext` for HTTP | Consistent with the future UI layer — same Playwright instance can drive both API and browser tests |
| `bat` in Jenkinsfile | Jenkins agent runs on Windows; `sh` is Linux-only |

---

## 3. Jenkins Pipeline

### 3.1 Pipeline Parameters

| Parameter | Values | Description |
|---|---|---|
| `ENV` | `staging`, `prod` | Target server to test against |
| `SUITE` | `smoke`, `regression` | Which test group to execute |

### 3.2 Trigger Strategy

The automation job should be triggered automatically by the backend and frontend deploy jobs:

```
Backend deploy job (staging)   → trigger automation: ENV=staging, SUITE=smoke
Frontend deploy job (staging)  → trigger automation: ENV=staging, SUITE=smoke
Backend deploy job (prod)      → trigger automation: ENV=prod,    SUITE=smoke
Frontend deploy job (prod)     → trigger automation: ENV=prod,    SUITE=smoke

Scheduled nightly (staging)    → trigger automation: ENV=staging, SUITE=regression
```

### 3.3 Required Jenkins Global Environment Variables

DevOps must configure these in **Manage Jenkins → Configure System → Global properties → Environment variables**:

| Variable | Example Value |
|---|---|
| `STAGING_API_URL` | `http://staging-server:9090` |
| `PROD_API_URL` | `http://98.93.75.232:9090` |

### 3.4 Test Reports

Jenkins publishes results via the JUnit plugin (which reads the standard XML format produced by Maven Surefire, regardless of whether tests are written in TestNG or JUnit):

```
target/surefire-reports/*.xml
```

Two additional self-contained HTML reports are produced per run and archived/published as Jenkins build artifacts + HTML Publisher links (see [README.md](README.md) → Test Results and [QUICK_REFERENCE.md](QUICK_REFERENCE.md) → Test Results & Reports for details):

| Report | Source | Output |
|---|---|---|
| Extent Report | `TestListener` (in-JVM TestNG listener) | `target/extent-report/index.html` |
| Dashboard Report | `DashboardGenerator` (`exec-maven-plugin`, `test` phase, reads `target/surefire-reports`) | `target/dashboard-report/index.html` |

Maven Surefire runs with `testFailureIgnore=true` so the build reaches the dashboard-generation step even when tests fail; Jenkins build result is therefore driven by the `junit` step (UNSTABLE on failures), not the Maven exit code.

---

## 4. UI Automation — Current & Planned

### 4.1 Why Playwright for UI

| Requirement | Playwright Support |
|---|---|
| API + UI in one framework | Native — `APIRequestContext` + `Page` |
| Microphone permission for voice banking | `context.grantPermissions(["microphone"])` |
| Fake mic input for voice queries | Chromium `--use-file-for-fake-audio-capture=<wav>` |
| Java / Maven | Full support |
| Video recording | Built-in — wired up in `BaseVoiceTest.launchBrowser()` (`setRecordVideoDir`), local-only output under `target/videos/` |
| CI/CD headless mode | `headless` system property, defaults to `true` |

### 4.2 UI Test Coverage — Current

| Class | Covers | Group(s) |
|---|---|---|
| `UI1_WelcomePageTest` | Phone entry, OTP trigger | `ui`, `regression` |
| `UI2_LoginTest` | Login flow | `ui`, `regression` |
| `UI3_OtpTest` | OTP verification | `ui`, `regression` |
| `UI4_LanguageTest` | Language selection | `ui`, `regression` |
| `UI5_VoiceRegistrationTest` | Voice registration screen | `ui`, `regression` |
| `UI6_HomePageTest` | Home screen elements, one mocked voice balance query | `ui`, `regression` |
| `UI7_BalanceInquiryTest` | 44 real voice balance queries (fake-mic WAV → STT → bot response), incl. account disambiguation follow-up (savings/current) with retry; shared browser session across all rows | `ui`, `regression`, `botverification` |
| `UI8_TransactionHistoryTest` | Voice transaction-history queries, incl. recency/date-range/category filters; some queries known not working live and marked `SkipException`-skipped (see class); fresh login per row deliberately | `ui`, `regression`, `botverification` |
| `UI9_LoanInquiryTest` | 76 real voice loan-inquiry queries across two loans (Home/Personal); which-loan disambiguation, plus a "what would you like to know" detail-category follow-up (EMI, tenure, pending tenure, interest rate, outstanding amount, next-EMI-due, loan amount) walked in one conversation; shared browser session across all rows | `ui`, `regression`, `botverification` |
| `UI10_TransferMoneyTest` | Voice money-transfer queries | `ui`, `regression`, `botverificationTransferMoney` |
| `UI11_VoiceRegistrationAuthTest` | Balance query spoken in the registered voice (authorized) vs. a different voice (rejected) | `ui`, `regression`, `smoke` |
| `UI12_MultilingualVoiceQueryTest` | Non-English voice queries | `ui`, `multilingual` (not `regression` — run explicitly) |

`BaseVoiceTest` (used by UI7–UI10) owns the Chromium+fake-audio lifecycle and the shared `runVoiceQuery(...)` flow: navigate → login → speak query → assert bot response, with automatic retry on transcription mismatch, on account/loan disambiguation follow-up, and on a "Session Ended" mid-query drop (`recoverFromSessionEndedIfPresent`, up to `MAX_REASK_ATTEMPTS` re-asks if the bot answers with a generic greeting or stays blank past `BOT_RESPONSE_TIMEOUT_MS`). Two environment-stability signals — session drops and blank/stuck bot responses — are tracked and timestamped by `SessionEndedTracker`/`NoResponseTracker` regardless of whether the retry recovered, surfaced on the dashboard report rather than as test failures.

`useSharedSession()` lets a class share one browser/login across every data-provider row instead of a fresh one per row (UI7, UI9 use this — cuts a ~76-row regression run roughly in half). UI8 deliberately stays on fresh-login-per-row: a shared session there once let a category filter from one query leak into several later, unrelated queries in the same conversation.

Run just the voice-verification suites:
```bash
mvn clean test -DtestGroups=botverification
mvn clean test -DtestGroups=botverificationTransferMoney
mvn clean test -DtestGroups=multilingual
```

### 4.3 Planned — Not Yet Automated

**Transactions & Transfers**
- Beneficiary add / edit
- Transfer success / error messages beyond the voice flow already covered by UI10

**Quality & Coverage**
- Cross-browser runs (Chrome, Firefox)
- Accessibility checks
- Session timeout handling (distinct from the live-bot "Session Ended" mid-conversation drop UI7–UI10 already recover from)
- Error state UI validation

### 4.4 Known Limitations — Fake Audio & Live-Bot Timing

`--use-file-for-fake-audio-capture` loads the WAV file into memory and loops it; overwriting the file on disk for a disambiguation follow-up ("savings"/"current", "which loan", a loan-detail category) doesn't always get picked up before the next hold-to-speak window closes. `BaseVoiceTest` mitigates this with up to 3 retries (increasing pre-wait each attempt) rather than asserting on the first attempt. This is a framework-level workaround, not a product bug — keep it in mind if new voice flows need a second spoken input.

Separately, the live bot itself is sometimes slow or drops the session mid-query — confirmed live via `[Browser] error: ... 502 Bad Gateway` / `SmallWebRTC Connect error` on the stage environment. `BaseVoiceTest.runVoiceQuery` tolerates this: it waits `BOT_RESPONSE_TIMEOUT_MS` (30s) for a reply, reconnects and re-asks the same query up to `MAX_REASK_ATTEMPTS` times if the bot answers with its generic session-start greeting or stays blank, and only fails the test once every attempt is exhausted — not on the first blank response. This is an environment-stability characteristic to expect, not something a new voice-query test needs to work around itself.

---

## 5. Test Pyramid

```
          ┌─────────────────────┐
          │   Manual / E2E      │  ~10%
          │  (Voice, UX, Cross  │
          │    browser)         │
          └─────────┬───────────┘
                    │
          ┌─────────▼───────────┐
          │    UI Automation     │  ~20% (In Progress — Playwright)
          │  (Login, Dashboard,  │
          │   Transfer, Voice)   │
          └─────────┬───────────┘
                    │
          ┌─────────▼───────────┐
          │    API Automation    │  ~70% (Current — TestNG)
          │  (All 9 APIs, smoke  │
          │   & regression)      │
          └─────────────────────┘
```

---

## 6. Implementation Roadmap

| Phase | Status | Description |
|---|---|---|
| Phase 1: API Framework Setup | ✅ Done | BaseApiPage, APIClient, pom.xml, TestNG config |
| Phase 2: API Tests | ✅ Done | All 9 APIs automated (21 test methods) |
| Phase 3: Jenkins CI/CD | ✅ Done | Jenkinsfile with ENV/SUITE params, group filtering |
| Phase 4: UI Test Framework | ✅ Done | `BaseVoiceTest`, `BasePage`, page objects, headless-by-default browser setup |
| Phase 5: UI Tests | 🔄 In Progress | Welcome/Login/OTP/Language/VoiceRegistration/Home done (UI1–UI6); Beneficiary UI flow still planned |
| Phase 6: Voice/Microphone | 🔄 In Progress | Fake-audio-capture + disambiguation (with retry) working for Balance (UI7), Transaction History (UI8), Loan Inquiry (UI9), Transfer (UI10), voice-auth (UI11), and Multilingual (UI12); session-drop reconnect + generic-greeting re-ask handling shared across all of them via `BaseVoiceTest` |
| Phase 7: HTML Reporting | ✅ Done | Extent Report (`TestListener`) + custom `DashboardGenerator` (module breakdown, inlined failure screenshots, session-drop/no-response detail lists, video recording) |
