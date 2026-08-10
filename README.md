# Voice Banking — Automation Suite

API and UI test automation for the Voice Banking platform.  
**Current coverage: 9 APIs (21 test methods) + 12 UI/voice screens (incl. voice regression suites for balance, transaction history, loan inquiry, and money transfer), 2 environments (stage + prod).**

---

## Quick Start

```bash
# Run all tests — defaults to prod URL
mvn clean test

# Run smoke suite against prod
mvn clean test -DtestGroups=smoke -Denv=prod

# Run smoke suite against stage
mvn clean test -DtestGroups=smoke -Denv=stage

# Run full regression against stage
mvn clean test -DtestGroups=regression -Denv=stage

# Run all UI tests
mvn clean test -DtestGroups=ui

# Run just the voice balance-inquiry regression suite (44 queries)
mvn clean test -DtestGroups=botverification

# Run just the voice transfer-money regression suite
mvn clean test -DtestGroups=botverificationTransferMoney

# Run a UI test headed (visible browser) instead of the CI default (headless)
mvn clean test -Dtest=UI7_BalanceInquiryTest -Dheadless=false

# Run against stage, headed, one class, one group
mvn clean test -Dtest=UI9_LoanInquiryTest -Dheadless=false -Denv=stage -DtestGroups=smoke
```

> `-Dtest=SomeClass` bypasses `testng.xml` entirely (Surefire builds its own ad-hoc suite), so a
> plain `mvn clean test` with no `-Dtest` is the only way to run everything in `testng.xml`,
> including `UI12_MultilingualVoiceQueryTest` — its own `@Test` groups don't include
> `regression`, only `ui`/`smoke`/`multilingual`, so `-DtestGroups=regression` alone skips it.

---

## Technology Stack

| Component | Technology | Version |
|---|---|---|
| Language | Java (Eclipse Temurin) | 21 |
| Build | Maven | 3.8+ |
| Test Framework | TestNG | 7.x |
| HTTP Client | Java `java.net.http.HttpClient` | 21 built-in |
| JSON Parsing | Jackson Databind | 2.16.0 |
| CI/CD | Jenkins Declarative Pipeline | — |

---

## Code Flow

```mermaid
flowchart TD
    A[Backend Deploy\nor Frontend Deploy] --> B[Trigger Jenkins Job]
    B --> C["Parameters:\nENV = prod | stage\nSUITE = smoke | regression"]
    C --> D[Checkout code from SCM]
    D --> E["mvn clean test\n-DtestGroups=SUITE -Denv=ENV"]
    E --> F["Endpoints.getBaseUrl()\nreads -Denv system property\nreturns prod or stage URL"]
    F --> G["Maven Surefire Plugin\nreads testng.xml\nfilters by groups property"]
    G --> H["TestNG Runner\ndiscovers annotated test methods"]
    H --> I["BaseApiPage\n@BeforeMethod alwaysRun=true\nnew APIClient(Endpoints.getBaseUrl())"]
    I --> J{Group Selected}
    J -->|smoke| K["7 tests\nAPI1 API2 API3 API6"]
    J -->|regression| L["21 tests\nAll 9 APIs"]
    J -->|api| L
    K --> M["APIClient\nPlaywright APIRequestContext\nHTTP POST to API server"]
    L --> M
    M --> N["API Server\nstaging or prod"]
    N --> O[JSON Response]
    O --> P["TestNG Assertions\nstatus · statusCode · data fields\nbusiness rules"]
    P --> Q["target/surefire-reports/*.xml"]
    Q --> Q2["DashboardGenerator\nexec-maven-plugin, bound to test phase\nruns even after failures (testFailureIgnore=true)"]
    Q2 --> Q3["target/dashboard-report/index.html"]
    Q --> R["Jenkins JUnit Plugin\nPublish test results"]
    R --> S{Result}
    S -->|Pass| T[Build SUCCESS]
    S -->|Fail| U[Build UNSTABLE + echo log]
```

---

## Test Groups

| Group | Tests | Purpose |
|---|---|---|
| `smoke` | API1, API2, API3, API6 + a fast subset from every UI class (UI7–UI11) | Quick sanity after any deploy |
| `regression` | 21 API methods + all UI classes except UI12 (see note above) | Full regression |
| `api` | 21 methods (all 9 APIs) | Run all API tests |
| `ui` | UI1–UI12 | Run all UI tests, including UI12 |
| `botverification` | UI7 (44 balance queries) + UI8 (transaction-history queries) + UI9 (loan queries) | Voice/bot-response regression suites |
| `botverificationTransferMoney` | UI10 — transfer-money voice queries | Transfer-money voice regression suite |
| `multilingual` | UI12 — non-English voice queries | Multilingual voice regression suite (not part of `regression`) |

---

## Environments

| Environment | URL | How selected |
|---|---|---|
| `prod` | `http://98.93.75.232:9090` | `-Denv=prod` or default (no flag) |
| `stage` | `http://3.111.41.3:9090` | `-Denv=stage` |

---

## Project Structure

```
voicebanking/
├── src/test/java/com/voicebanking/
│   ├── DataText/
│   │   ├── Constants.java          # API test data, expected values
│   │   ├── Endpoints.java          # API paths + base URLs, UI base URL
│   │   ├── VoiceQueries.java       # Spoken query text for voice tests
│   │   └── BotResponsePatterns.java # Regex patterns for bot responses (Balance, Transactions, Loans)
│   ├── utils/
│   │   ├── APIClient.java          # Playwright HTTP wrapper
│   │   ├── TtsUtil.java            # Generates/deletes WAV files for fake-mic voice input
│   │   ├── SessionEndedTracker.java # Counts + timestamps "Session Ended" recoveries across a run
│   │   └── NoResponseTracker.java  # Counts + timestamps blank/stuck-Processing bot responses across a run
│   ├── pages/
│   │   ├── BaseApiPage.java        # @BeforeMethod — creates APIClient
│   │   ├── BasePage.java           # Shared Playwright browser setup for non-voice UI tests
│   │   ├── HomePage.java, WelcomePage.java, OtpPage.java, LanguagePage.java, VoiceRegistrationPage.java
│   ├── tests/api/
│   │   ├── API1_GetAccountListTest.java
│   │   ├── API2_GetCustomerInfoTest.java
│   │   ├── API3_GetAccountBalanceTest.java
│   │   ├── API4_GetBeneficiariesListTest.java
│   │   ├── API5_GetTransactionsListTest.java
│   │   ├── API6_TransferMoneyTest.java
│   │   ├── API8_GetLoanStatementTest.java
│   │   ├── API9_GetLoanOverdueDetailsTest.java
│   │   └── API10_GetLoanSummaryListTest.java
│   └── tests/ui/
│       ├── base/BaseVoiceTest.java # Chromium + fake-mic lifecycle, shared runVoiceQuery() flow
│       ├── UI1_WelcomePageTest.java
│       ├── UI2_LoginTest.java
│       ├── UI3_OtpTest.java
│       ├── UI4_LanguageTest.java
│       ├── UI5_VoiceRegistrationTest.java
│       ├── UI6_HomePageTest.java
│       ├── UI7_BalanceInquiryTest.java        # 44-query voice balance-inquiry regression suite
│       ├── UI8_TransactionHistoryTest.java    # Voice transaction-history regression suite
│       ├── UI9_LoanInquiryTest.java           # Voice loan-inquiry regression suite (shared session)
│       ├── UI10_TransferMoneyTest.java        # Voice transfer-money regression suite
│       ├── UI11_VoiceRegistrationAuthTest.java # Registered-voice vs. mismatched-voice auth check
│       └── UI12_MultilingualVoiceQueryTest.java # Non-English voice queries
├── testng.xml                      # Suite — all API + UI classes
├── Jenkinsfile                     # CI pipeline
├── pom.xml                         # Maven config
├── skills.md                       # AI skills — tool-agnostic rules for API AND UI/voice test generation
├── TEST_PLAN.md                    # Full test plan + roadmap (API + UI)
├── QUICK_REFERENCE.md              # Commands cheat sheet (API + UI)
├── MICROPHONE_TESTING_GUIDE.md     # Voice/microphone reference
└── BDD_FRAMEWORKS_GUIDE.md         # BDD options reference (API + UI)
```

---

## API Coverage

| API | Endpoint | Groups | Auto |
|---|---|---|---|
| 1 — Account List | POST `/api/v1/accounts/list` | smoke, regression, api | ✅ |
| 2 — Customer Info | POST `/api/v1/customers/info` | smoke, regression, api | ✅ |
| 3 — Account Balance | POST `/api/v1/accounts/balance` | smoke, regression, api | ✅ |
| 4 — Beneficiaries | POST `/api/v1/beneficiaries/list` | regression, api | ✅ |
| 5 — Transactions | POST `/api/v1/transactions/list` | regression, api | ✅ |
| 6 — Transfer Money | POST `/api/v1/transactions/transfer` | smoke, regression, api | ✅ |
| 7 — (out of scope) | — | — | — |
| 8 — Loan Statement | POST `/api/v1/loans/statement` | regression, api | ✅ |
| 9 — Loan Overdue | POST `/api/v1/loans/overdue` | regression, api | ✅ |
| 10 — Loan Summary | POST `/api/v1/loans/summary` | regression, api | ✅ |
| 11–13 — (out of scope) | — | — | — |

---

## UI / Voice Coverage

| Screen | Test Class | Groups | Auto |
|---|---|---|---|
| Welcome / phone entry | UI1_WelcomePageTest | ui, regression | ✅ |
| Login | UI2_LoginTest | ui, regression | ✅ |
| OTP | UI3_OtpTest | ui, regression | ✅ |
| Language selection | UI4_LanguageTest | ui, regression | ✅ |
| Voice registration | UI5_VoiceRegistrationTest | ui, regression | ✅ |
| Home screen | UI6_HomePageTest | ui, regression | ✅ |
| Voice balance inquiry (44 queries) | UI7_BalanceInquiryTest | ui, regression, botverification | ✅ |
| Voice transaction history | UI8_TransactionHistoryTest | ui, regression, botverification | ✅ |
| Voice loan inquiry (76 queries) | UI9_LoanInquiryTest | ui, regression, botverification | ✅ |
| Voice money transfer | UI10_TransferMoneyTest | ui, regression, botverificationTransferMoney | ✅ |
| Voice-auth (registered vs. mismatched voice) | UI11_VoiceRegistrationAuthTest | ui, regression, smoke | ✅ |
| Multilingual voice queries | UI12_MultilingualVoiceQueryTest | ui, multilingual (not `regression` — run explicitly) | ✅ |
| Beneficiary add / edit UI flow | — | — | 🔜 planned |

UI7–UI10 (and UI11's auth check) drive real speech via Chromium's `--use-file-for-fake-audio-capture` (a generated WAV per query), asserting the bot's response against regex patterns in `BotResponsePatterns` (`Balance`, `Transactions`, `Loans`). Shared machinery lives in `BaseVoiceTest`: session-drop reconnect + re-ask retries (`recoverFromSessionEndedIfPresent`, `MAX_REASK_ATTEMPTS`), account/loan disambiguation follow-ups, and — for UI7 and UI9 — a shared browser session across all rows in the class (`useSharedSession()`) instead of a fresh login per row, to cut regression runtime. UI8 deliberately stays on fresh-login-per-row after a shared-session filter-state leak was observed live. See [TEST_PLAN.md](TEST_PLAN.md) → Section 4 for details.

---

## Jenkins Pipeline

The `Jenkinsfile` in the repo root defines the pipeline.

**Parameters:**

| Parameter | Options | Default |
|---|---|---|
| `ENV` | `prod`, `stage` | `prod` |
| `SUITE` | `smoke`, `regression` | — |

**No Jenkins global environment variables required** — URLs are hardcoded in `Endpoints.java` and selected via `-Denv`.

**Auto-trigger** — dev teams call this after deployment:
```bash
curl -X POST "http://JENKINS_URL/job/voicebanking-automation/buildWithParameters" \
  --user "username:api_token" \
  --data "token=voicebanking-trigger&ENV=stage&SUITE=smoke"
```

---

## Test Results

Results in `target/surefire-reports/`, `target/extent-report/index.html`, and `target/dashboard-report/index.html`.

```bash
# Run tests — Extent report and dashboard report both auto-generate
mvn clean test -Denv=stage

# Open Extent report (Windows)
start target\extent-report\index.html

# Open dashboard report (Windows)
start target\dashboard-report\index.html
```

The dashboard report (`DashboardGenerator`, bound to the Maven `test` phase via `exec-maven-plugin`) parses `target/surefire-reports`, matches each failed test to its `ScreenshotUtil` screenshot, and writes one self-contained `index.html` — module pass-rate breakdown, a searchable results table, and inlined failure screenshots, no `screenshots/` folder needed alongside it. It also surfaces two environment-stability signals that don't show up as ordinary failures (a session drop or a stuck response that a retry successfully recovered from still never fails the test): a "Session Drops" / "Bot No-Response" stat card plus a timestamped detail list for each, read from `target/session-ended-*.txt` / `target/no-response-*.txt` (written by `SessionEndedTracker`/`NoResponseTracker`, which only actually run when `TestListener` is registered — see the `@Listeners(TestListener.class)` note on `BaseVoiceTest`).

Voice test runs also record video via Playwright (`target/videos/*.webm`) — one continuous clip per browser context, so a `useSharedSession()` class (UI7, UI9) produces one video covering the whole run rather than one clip per row. Local only; `target/` is gitignored.

Maven Surefire runs with `testFailureIgnore=true`, so `mvn clean test` always completes (exit 0) even when tests fail — this is what lets the dashboard step run after a failing suite instead of the build stopping at Surefire. Whether a run had failures is read from the report contents (or the `junit` step in Jenkins), not the Maven exit code.

Jenkins publishes both reports as downloadable artifacts and as build-page links — **"Test Report"** (Extent) and **"Dashboard"** (dashboard report) — requires the HTML Publisher plugin.

---

## Future: UI Automation Beyond Current Coverage

Login, OTP, language, voice registration, home screen, voice balance inquiry, voice transaction history, voice loan inquiry, voice money transfer, voice-auth, and multilingual voice queries are already automated (see UI/Voice Coverage above). Still planned:
- Beneficiary add / edit UI flow
- Cross-browser UI runs

See [TEST_PLAN.md](TEST_PLAN.md) → Section 4 for the full UI roadmap.

---

## Documentation

| File | Purpose |
|---|---|
| [README.md](README.md) | This file — start here |
| [skills.md](skills.md) | AI skills — tool-agnostic rules for API AND UI/voice test generation |
| [TEST_PLAN.md](TEST_PLAN.md) | Full test plan, strategy, roadmap (API + UI) |
| [QUICK_REFERENCE.md](QUICK_REFERENCE.md) | Commands cheat sheet, common issues (API + UI) |
| [MICROPHONE_TESTING_GUIDE.md](MICROPHONE_TESTING_GUIDE.md) | Voice/microphone reference — see "Actual Implementation" section for this project's real setup |
| [BDD_FRAMEWORKS_GUIDE.md](BDD_FRAMEWORKS_GUIDE.md) | BDD options for future consideration (API + UI) |

---

**Status**: API automation complete ✅ | UI automation in progress 🔄 (Welcome/Login/OTP/Language/VoiceRegistration/Home + voice Balance/Transaction-History/Loan-Inquiry/Transfer/Auth/Multilingual done; Beneficiary UI flow + cross-browser runs planned)  
**Java**: 21 | **Framework**: TestNG | **CI**: Jenkins
