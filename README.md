# Voice Banking — Automation Suite

API and UI test automation for the Voice Banking platform.  
**Current coverage: 9 APIs, 21 test methods, 2 environments (stage + prod).**

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
```

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
    Q --> R["Jenkins JUnit Plugin\nPublish test results"]
    R --> S{Result}
    S -->|Pass| T[Build SUCCESS]
    S -->|Fail| U[Build FAILURE + echo log]
```

---

## Test Groups

| Group | Tests | Purpose |
|---|---|---|
| `smoke` | 7 methods (API1, API2, API3, API6) | Quick sanity after any deploy |
| `regression` | 21 methods (all 9 APIs) | Full regression |
| `api` | 21 methods (all 9 APIs) | Run all API tests |

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
│   │   ├── Constants.java          # Test data, expected values
│   │   └── Endpoints.java          # API paths + base URLs
│   ├── utils/
│   │   └── APIClient.java          # Playwright HTTP wrapper
│   ├── pages/
│   │   └── BaseApiPage.java        # @BeforeMethod — creates APIClient
│   └── tests/api/
│       ├── API1_GetAccountListTest.java
│       ├── API2_GetCustomerInfoTest.java
│       ├── API3_GetAccountBalanceTest.java
│       ├── API4_GetBeneficiariesListTest.java
│       ├── API5_GetTransactionsListTest.java
│       ├── API6_TransferMoneyTest.java
│       ├── API8_GetLoanStatementTest.java
│       ├── API9_GetLoanOverdueDetailsTest.java
│       └── API10_GetLoanSummaryListTest.java
├── testng.xml                      # Suite — all 9 classes
├── Jenkinsfile                     # CI pipeline
├── pom.xml                         # Maven config
├── CLAUDE.md                       # AI skills — Claude Code test generation rules
├── TEST_PLAN.md                    # Full test plan + roadmap
├── TEST_CASES.xlsx                 # Test case register
├── QUICK_REFERENCE.md              # Commands cheat sheet
├── MICROPHONE_TESTING_GUIDE.md    # Future voice testing reference
└── BDD_FRAMEWORKS_GUIDE.md        # Future BDD options reference
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

Results in `target/surefire-reports/` and `target/extent-report/index.html`.

```bash
# Run tests — Extent report auto-generates
mvn clean test -Denv=stage

# Open Extent report (Windows)
start target\extent-report\index.html
```

Jenkins publishes the report as a downloadable artifact and a **"Test Report"** link on the build page (requires HTML Publisher plugin).

---

## Future: UI Automation

UI automation is planned using Playwright browser APIs (same framework dependency already in `pom.xml`).

Planned coverage:
- Login / Logout
- Account list and balance screens
- Transfer money flow
- Loan summary and statement pages
- Voice / Microphone permission and voice command tests

See [TEST_PLAN.md](TEST_PLAN.md) → Section 4 for the full UI roadmap.

---

## Documentation

| File | Purpose |
|---|---|
| [README.md](README.md) | This file — start here |
| [CLAUDE.md](CLAUDE.md) | AI skills — Claude Code rules for generating tests |
| [TEST_PLAN.md](TEST_PLAN.md) | Full test plan, strategy, roadmap |
| [TEST_CASES.xlsx](TEST_CASES.xlsx) | All test cases (automated + manual) with groups |
| [QUICK_REFERENCE.md](QUICK_REFERENCE.md) | Commands cheat sheet, common issues |
| [MICROPHONE_TESTING_GUIDE.md](MICROPHONE_TESTING_GUIDE.md) | Voice/microphone implementation reference |
| [BDD_FRAMEWORKS_GUIDE.md](BDD_FRAMEWORKS_GUIDE.md) | BDD options for future consideration |

---

**Status**: API automation complete ✅ | UI automation planned 🔜  
**Java**: 21 | **Framework**: TestNG | **CI**: Jenkins
