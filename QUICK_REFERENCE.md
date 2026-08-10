# Voice Banking — Quick Reference

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| Java (Eclipse Temurin) | 21 | Set `JAVA_HOME` to JDK 21 path |
| Maven | 3.8+ | `mvn -version` to verify |
| Git | Any | For source checkout |

---

## Running Tests Locally

### Run all tests
```bash
mvn clean test
```

### Run by group
```bash
# Smoke only — fast sanity check (API1, API2, API3, API6)
mvn clean test -DtestGroups=smoke

# Full regression — all 9 APIs (21 test methods) + all UI classes
mvn clean test -DtestGroups=regression

# All API tests
mvn clean test -DtestGroups=api

# All UI tests (UI1-UI12) — note: -Dtest=SomeClass below bypasses testng.xml/groups entirely
mvn clean test -DtestGroups=ui

# Voice balance/transaction-history/loan-inquiry bot-response regression suites (UI7, UI8, UI9)
mvn clean test -DtestGroups=botverification

# Voice transfer-money bot-response regression suite (UI10)
mvn clean test -DtestGroups=botverificationTransferMoney

# Multilingual voice queries (UI12) — NOT included in -DtestGroups=regression, run explicitly
mvn clean test -DtestGroups=multilingual
```

### Run a UI test headed (visible browser)
```bash
# CI defaults to headless=true — override locally to watch the browser
mvn clean test -Dtest=UI7_BalanceInquiryTest -Dheadless=false
```

### Run against a specific environment
```bash
# Stage
mvn clean test -DtestGroups=smoke -Denv=stage

# Prod (also the default when -Denv is omitted)
mvn clean test -DtestGroups=smoke -Denv=prod
```

### Run a single test class
```bash
mvn test -Dtest=API1_GetAccountListTest
mvn test -Dtest=API6_TransferMoneyTest
mvn test -Dtest=UI7_BalanceInquiryTest
mvn test -Dtest=UI9_LoanInquiryTest -Dheadless=false -Denv=stage -DtestGroups=smoke
```

> Note: `-Dtest=SomeClass` makes Surefire build its own ad-hoc suite instead of reading
> `testng.xml` — so `TestListener` (registered there via a suite-level `<listener>` tag) would
> silently never run for any `-Dtest=` invocation, and with it `SessionEndedTracker`/
> `NoResponseTracker`/the Extent report. `BaseVoiceTest` now also carries `@Listeners(TestListener.class)`
> directly on the class specifically to make `-Dtest=` runs work correctly too — if a similar gap
> ever resurfaces (e.g. target/extent-report/ missing after a `-Dtest=` run), check for a listener
> that's only declared in testng.xml.

### Skip tests during build
```bash
mvn clean install -DskipTests
```

---

## Jenkins Pipeline

### Manual trigger (from Jenkins UI)
1. Open the automation job → **Build with Parameters**
2. Set `ENV` = `staging` or `prod`
3. Set `SUITE` = `smoke` or `regression`
4. Click **Build**

### What each combination runs

| ENV | SUITE | What runs |
|---|---|---|
| stage | smoke | API1, API2, API3, API6 + fast UI subsets (UI7–UI11) — quick sanity |
| stage | regression | All 9 APIs + all UI classes except UI12 (see note below) |
| prod | smoke | API1, API2, API3, API6 + fast UI subsets (UI7–UI11) — prod sanity |
| prod | regression | All 9 APIs + all UI classes except UI12 (see note below) |

Jenkins `SUITE` currently offers `smoke`/`regression` only. To run just `botverification`, `botverificationTransferMoney`, `multilingual`, or `ui` via CI, trigger manually with e.g. `mvn clean test -DtestGroups=botverification` (or add it as a Jenkins `SUITE` choice — see Jenkinsfile).

`UI12_MultilingualVoiceQueryTest`'s `@Test` methods carry `ui`/`smoke`/`multilingual` groups, not `regression` — so it's part of `testng.xml` but `-DtestGroups=regression` skips it. Run it with `-DtestGroups=multilingual` (or `ui`, or a plain `mvn clean test` with no group filter, which runs everything in `testng.xml` regardless of group).

### No Jenkins global vars required
URLs are managed in `Endpoints.java` and selected via `-Denv` — no server-side configuration needed.

---

## Test Coverage Summary

| API | Test Class | Groups | Test Count |
|---|---|---|---|
| 1 — Account List | API1_GetAccountListTest | smoke, regression, api | 2 |
| 2 — Customer Info | API2_GetCustomerInfoTest | smoke, regression, api | 2 |
| 3 — Account Balance | API3_GetAccountBalanceTest | smoke, regression, api | 2 |
| 4 — Beneficiaries | API4_GetBeneficiariesListTest | regression, api | 3 |
| 5 — Transactions | API5_GetTransactionsListTest | regression, api | 3 |
| 6 — Transfer Money | API6_TransferMoneyTest | smoke, regression, api | 1 |
| 8 — Loan Statement | API8_GetLoanStatementTest | regression, api | 2 |
| 9 — Loan Overdue | API9_GetLoanOverdueDetailsTest | regression, api | 2 |
| 10 — Loan Summary | API10_GetLoanSummaryListTest | regression, api | 4 |

**Smoke total: 7 methods | Regression/API total: 21 methods**

---

## UI / Voice Test Coverage Summary

| Class | Groups | Covers |
|---|---|---|
| UI1_WelcomePageTest | ui, regression | Phone entry, OTP trigger |
| UI2_LoginTest | ui, regression | Login flow |
| UI3_OtpTest | ui, regression | OTP verification |
| UI4_LanguageTest | ui, regression | Language selection |
| UI5_VoiceRegistrationTest | ui, regression | Voice registration screen |
| UI6_HomePageTest | ui, regression | Home screen elements, one mocked voice query |
| UI7_BalanceInquiryTest | ui, regression, botverification | 44 real voice balance queries via fake-mic WAV, incl. account disambiguation with retry; shared browser session across rows |
| UI8_TransactionHistoryTest | ui, regression, botverification | 36 voice transaction-history queries (some marked `SkipException`-skipped, known not working — see class for the current list) + known-account rows; fresh login per row deliberately (a shared-session filter-state leak was observed live) |
| UI9_LoanInquiryTest | ui, regression, botverification | 76 voice loan-inquiry queries, incl. which-loan disambiguation and a "what would you like to know" detail-category walk (EMI/tenure/interest/outstanding/next-EMI-due/loan-amount) in one conversation; shared browser session across rows |
| UI10_TransferMoneyTest | ui, regression, botverificationTransferMoney | Voice money-transfer queries |
| UI11_VoiceRegistrationAuthTest | ui, regression, smoke | Registered-voice vs. mismatched-voice balance-query authorization |
| UI12_MultilingualVoiceQueryTest | ui, multilingual (not regression) | Non-English voice queries |

**UI/voice total: 12 test classes**

---

## Project Structure

```
voicebanking/
├── src/test/java/com/voicebanking/
│   ├── DataText/
│   │   ├── Constants.java          # API test data (IDs, expected values)
│   │   ├── Endpoints.java          # API endpoint paths + base URLs + UI base URL
│   │   ├── VoiceQueries.java       # Spoken query text for voice tests
│   │   └── BotResponsePatterns.java # Regex patterns for bot responses
│   ├── utils/
│   │   ├── APIClient.java          # Playwright HTTP wrapper
│   │   ├── TtsUtil.java            # Generates/deletes WAV files for fake-mic voice input
│   │   ├── SessionEndedTracker.java # Counts + timestamps "Session Ended" recoveries across a run
│   │   └── NoResponseTracker.java  # Counts + timestamps blank/stuck-Processing bot responses across a run
│   ├── pages/
│   │   ├── BaseApiPage.java        # @BeforeMethod setUp() — creates APIClient
│   │   ├── BasePage.java           # Shared Playwright browser setup for non-voice UI tests
│   │   └── HomePage.java, WelcomePage.java, OtpPage.java, LanguagePage.java, VoiceRegistrationPage.java
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
│       ├── base/BaseVoiceTest.java # Chromium + fake-mic lifecycle, shared runVoiceQuery() flow, video recording
│       ├── UI1_WelcomePageTest.java ... UI6_HomePageTest.java
│       ├── UI7_BalanceInquiryTest.java        # 44-query voice balance-inquiry regression suite
│       ├── UI8_TransactionHistoryTest.java    # Voice transaction-history regression suite
│       ├── UI9_LoanInquiryTest.java           # Voice loan-inquiry regression suite
│       ├── UI10_TransferMoneyTest.java        # Voice transfer-money regression suite
│       ├── UI11_VoiceRegistrationAuthTest.java # Registered-voice vs. mismatched-voice auth check
│       └── UI12_MultilingualVoiceQueryTest.java # Non-English voice queries
├── testng.xml                      # Suite definition (all API + UI classes)
├── Jenkinsfile                     # CI pipeline (ENV + SUITE params)
├── pom.xml                         # Maven config (Java 21, TestNG, Playwright)
├── skills.md                       # AI skills — rules for both API and UI/voice test generation
├── TEST_PLAN.md                    # Full test plan + roadmap (API + UI)
├── QUICK_REFERENCE.md              # This file
├── README.md                       # Getting started
├── MICROPHONE_TESTING_GUIDE.md     # Voice/microphone reference
└── BDD_FRAMEWORKS_GUIDE.md         # BDD options reference (API + UI)
```

---

## API Endpoints

| API | Method | Endpoint | Key Request Fields |
|---|---|---|---|
| 1 — Account List | POST | `/api/v1/accounts/list` | `customerId` |
| 2 — Customer Info | POST | `/api/v1/customers/info` | `customerId` |
| 3 — Account Balance | POST | `/api/v1/accounts/balance` | `accountId`, `customerId`, `accountType` |
| 4 — Beneficiaries | POST | `/api/v1/beneficiaries/list` | `customerId`, `accountId` |
| 5 — Transactions | POST | `/api/v1/transactions/list` | `accountId`, `toDate`, `page`, `size` |
| 6 — Transfer | POST | `/api/v1/transactions/transfer` | `customerId`, `fromAccountId`, `beneficiaryId`, `amount` |
| 8 — Loan Statement | POST | `/api/v1/loans/statement` | `accountId`, `fromDate`, `toDate`, `page`, `size` |
| 9 — Loan Overdue | POST | `/api/v1/loans/overdue` | `accountId` |
| 10 — Loan Summary | POST | `/api/v1/loans/summary` | `customerId` |

---

## Test Data

| Data | Value |
|---|---|
| Customer ID | `CIF202602260001` |
| Savings Account ID | `ACC202602260001` |
| Transaction Account ID | `ACC202602260006` |
| Loan Account ID | `LN10001` |
| Customer Name | `Amit Sharma` |

---

## Test Results & Reports

### How the reports work

Two self-contained HTML reports generate automatically on every `mvn clean test` run — no server needed, open either in any browser, attach to an email.

| Report | Generated by | Output |
|---|---|---|
| Extent Report | `TestListener` (TestNG listener, runs inside the test JVM) | `target/extent-report/index.html` |
| Dashboard Report | `DashboardGenerator` (`exec-maven-plugin`, bound to Maven's `test` phase, runs after Surefire) | `target/dashboard-report/index.html` |

The dashboard report parses `target/surefire-reports`, groups results by module (API class group, e.g. "Accounts"), and inlines the `ScreenshotUtil` screenshot for each failed test directly into the HTML (base64 data URI) — searchable results table included.

Maven Surefire runs with `testFailureIgnore=true`, so `mvn clean test` always exits `0`, even with test failures — this is required so the dashboard-generation step (which runs after Surefire) isn't skipped when the suite has failures. Check the report contents, not the Maven exit code, to see if a run passed.

### View reports locally
```bash
# Run tests — both reports generate automatically
mvn clean test -DtestGroups=smoke

# Open Extent report (Windows)
start target\extent-report\index.html

# Open dashboard report (Windows)
start target\dashboard-report\index.html
```

The Extent report shows:
- Pass / Fail / Skip counts with a visual timeline
- Full stack trace for every failure
- Test categories (smoke / regression / api) as filterable tags
- System info: environment, suite, API URL, Java version

The dashboard report shows:
- Pass rate donut chart + per-module breakdown
- Searchable/filterable results table (by module, class, test name, category)
- Inlined failure screenshots and stack traces, expandable per row

### Jenkins reports

After each pipeline run:
- **Test trend graph** — JUnit plugin, visible on the job's main page
- **Artifacts** — both `extent-report/index.html` and `dashboard-report/index.html` downloadable from the build page's "Build Artifacts" section
- **"Test Report" link** — Extent HTML viewable in Jenkins (requires HTML Publisher plugin)
- **"Dashboard" link** — dashboard HTML viewable in Jenkins (requires HTML Publisher plugin)
- Build result is **UNSTABLE** (not FAILURE) when tests fail, since `testFailureIgnore=true` makes the `mvn` step itself always succeed — the `junit` step reading `target/surefire-reports/*.xml` is what flags failures to Jenkins

---

## Jenkins Setup Checklist (DevOps)

### Plugins required
| Plugin | Purpose |
|---|---|
| **HTML Publisher** | Publishes `target/extent-report/index.html` and `target/dashboard-report/index.html` as clickable Jenkins build links |

Install at: **Manage Jenkins → Plugins → Available plugins** → search `HTML Publisher`

### Jenkins Global Environment Variables
None required — URLs are managed in `Endpoints.java`.

---

## Common Issues

| Issue | Cause | Fix |
|---|---|---|
| `Connection refused` | API server not running | Start the server or check the URL |
| `apiClient is null / NPE` | `@BeforeMethod` not running during group filter | Ensure `@BeforeMethod(alwaysRun = true)` in BaseApiPage |
| `Cannot run program "sh"` | Jenkins on Windows using Linux shell command | Jenkinsfile uses `bat`, not `sh` |
| `No test report files found` | Tests did not run (compile or setup error) | Fix the prior error; surefire XML only appears on successful run |
| Jenkins build shows UNSTABLE, not FAILURE, when tests fail | `testFailureIgnore=true` in `pom.xml` makes the `mvn` step exit 0 even with test failures — needed so `DashboardGenerator` still runs after a failing suite | Expected behavior; check the `junit` step / Extent / dashboard report for the actual pass/fail counts, not the build color alone |
| `target/dashboard-report/index.html` missing after a run | `exec-maven-plugin` execution not found (older `pom.xml`, or `mvn` invoked with `-DskipTests`) | Confirm `pom.xml` has the `generate-dashboard-report` execution bound to the `test` phase; `-DskipTests` skips it since Surefire never runs |
| `invalid target release: 21` | VS Code terminal has old JAVA_HOME | Set `$env:JAVA_HOME` to JDK 21 path or restart VS Code |
| `No tests found` | Test class doesn't end with "Test" | Rename: `API1Test.java` ✅, `API1.java` ❌ |
| UI test opens a real browser on CI | `headless` system property not set, or hardcoded `setHeadless(false)` | `BaseVoiceTest`/`BasePage` default `headless` to `true` — pass `-Dheadless=false` only when running locally |
| Disambiguation follow-up ("savings"/"current") not recognized, bot re-asks | Fake-audio-capture loops the previously loaded WAV in memory; overwritten file isn't always picked up in time | `BaseVoiceTest` retries the follow-up up to 3 times with increasing pre-wait — this is expected occasionally, not a product bug |
| Bot response phrasing not detected as a disambiguation prompt | `isAccountDisambiguation()` doesn't match a new bot phrasing | Add the new phrasing to `isAccountDisambiguation()` in `BaseVoiceTest.java` |
| "Home page should remain visible after voice query" fails right after a "Session Ended" screenshot | The reconnect click was targeting the wrong element — `[data-testid='listening-hold-to-speak-btn']` isn't present during "Session Ended"; the actual reconnect control is `[data-testid='listening-reconnect-btn']` ("Hold to reconnect"), only relabeling back to the ordinary button once truly reconnected | Fixed in `HomePage.recoverFromSessionEndedIfPresent()` — if this resurfaces, confirm the reconnect button's `data-testid` hasn't changed again |
| Bot answered with a generic greeting/menu prompt instead of the actual query | The bot occasionally answers the very first query right after a reconnect with its own session-start greeting instead of processing it | `BaseVoiceTest`'s re-ask loop (`isGenericGreeting`, up to `MAX_REASK_ATTEMPTS`) re-asks the same query; if a *new* greeting/menu phrasing shows up unhandled, widen the `GENERIC_GREETING` pattern |
| `target/session-ended-count.txt` / `target/no-response-count.txt` missing or always 0 after a run that clearly hit one | `TestListener` (which writes both via `SessionEndedTracker`/`NoResponseTracker`) was only registered via testng.xml's suite-level `<listener>` tag, which Surefire skips for any `-Dtest=` invocation | `BaseVoiceTest` now also carries `@Listeners(TestListener.class)` directly — should self-heal; if it recurs, check for a listener declared only in testng.xml |

---

## Adding a New API Test

1. Create `src/test/java/com/voicebanking/tests/api/APIxx_DescriptionTest.java`
2. Extend `BaseApiPage`
3. Add test methods with appropriate groups:
   ```java
   @Test(groups = {"smoke", "regression", "api"}, description = "...")
   public void testXxx() throws Exception { ... }
   ```
4. Add the class to `testng.xml`:
   ```xml
   <class name="com.voicebanking.tests.api.APIxx_DescriptionTest"/>
   ```
5. Run: `mvn test -Dtest=APIxx_DescriptionTest`

---

## Adding a New Voice Query (e.g. to UI7_BalanceInquiryTest)

1. Add the phrase to `VoiceQueries.java`.
2. Add a 5-tuple row to the `@DataProvider`:
   ```java
   {"Query Name", VoiceQueries.English.YOUR_QUERY,
           new String[]{"keyword1", "keyword2"}, BotResponsePatterns.Balance.ANY, "savings"}
   ```
   `disambiguationAccount` (5th field) is `"savings"`/`"current"` for ambiguous queries, `null` when the query already names an account. Alternate `"savings"`/`"current"` across new ambiguous rows for balanced coverage.
3. Run: `mvn test -Dtest=UI7_BalanceInquiryTest -Dheadless=false` to watch it locally first.

## Adding a New Non-Voice UI Test

1. Create `src/test/java/com/voicebanking/tests/ui/UIxx_DescriptionTest.java`
2. Extend `BasePage` (not `BaseVoiceTest` — that's for voice-driven flows only)
3. Add test methods with `groups = {"ui", "regression"}`
4. Add the class to `testng.xml`
5. Run: `mvn test -Dtest=UIxx_DescriptionTest`
