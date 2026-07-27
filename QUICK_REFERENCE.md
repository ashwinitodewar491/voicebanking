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

# All UI tests (UI1-UI7)
mvn clean test -DtestGroups=ui

# Voice balance-inquiry bot-response regression suite only (43 queries, UI7)
mvn clean test -DtestGroups=botverification
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
```

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
| stage | smoke | API1, API2, API3, API6 — quick sanity |
| stage | regression | All 9 APIs + all UI classes (UI1–UI7) |
| prod | smoke | API1, API2, API3, API6 — prod sanity |
| prod | regression | All 9 APIs + all UI classes (UI1–UI7) |

Jenkins `SUITE` currently offers `smoke`/`regression` only. To run just `botverification` or `ui` via CI, trigger manually with `mvn clean test -DtestGroups=botverification` (or add it as a Jenkins `SUITE` choice — see Jenkinsfile).

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
| UI7_BalanceInquiryTest | ui, regression, botverification | 43 real voice balance queries via fake-mic WAV, incl. account disambiguation with retry |

**UI/voice total: 7 test classes, 43 data-driven balance queries in UI7 alone**

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
│   │   └── TtsUtil.java            # Generates/deletes WAV files for fake-mic voice input
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
│       ├── base/BaseVoiceTest.java # Chromium + fake-mic lifecycle, shared runVoiceQuery() flow
│       ├── UI1_WelcomePageTest.java ... UI6_HomePageTest.java
│       └── UI7_BalanceInquiryTest.java   # 43-query voice balance-inquiry regression suite
├── testng.xml                      # Suite definition (all API + UI classes)
├── Jenkinsfile                     # CI pipeline (ENV + SUITE params)
├── pom.xml                         # Maven config (Java 21, TestNG, Playwright)
├── skills.md                       # AI skills — rules for both API and UI/voice test generation
├── TEST_PLAN.md                    # Full test plan + roadmap (API + UI)
├── TEST_CASES.xlsx                 # Test case register (automated + manual)
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
