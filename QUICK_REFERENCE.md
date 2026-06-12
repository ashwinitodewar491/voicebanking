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

# Full regression — all 9 APIs (21 test methods)
mvn clean test -DtestGroups=regression

# All API tests
mvn clean test -DtestGroups=api
```

### Run against a specific environment
```bash
# Staging
mvn clean test -DtestGroups=smoke -DAPI_BASE_URL=http://staging-server:9090

# Prod
mvn clean test -DtestGroups=smoke -DAPI_BASE_URL=http://98.93.75.232:9090
```

### Run a single test class
```bash
mvn test -Dtest=API1_GetAccountListTest
mvn test -Dtest=API6_TransferMoneyTest
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
| staging | smoke | API1, API2, API3, API6 — quick sanity |
| staging | regression | All 9 APIs |
| prod | smoke | API1, API2, API3, API6 — prod sanity |
| prod | regression | All 9 APIs |

### Jenkins global vars required (DevOps to configure)
```
STAGING_API_URL = http://staging-server:9090
PROD_API_URL    = http://98.93.75.232:9090
```
Set at: **Manage Jenkins → Configure System → Global properties → Environment variables**

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

## Project Structure

```
voicebanking/
├── src/test/java/com/voicebanking/
│   ├── DataText/
│   │   ├── Constants.java          # Test data (IDs, expected values)
│   │   └── Endpoints.java          # API endpoint paths + base URLs
│   ├── utils/
│   │   └── APIClient.java          # Playwright HTTP wrapper
│   ├── pages/
│   │   └── BaseApiPage.java        # @BeforeMethod setUp() — creates APIClient
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
├── testng.xml                      # Suite definition (all 9 classes)
├── Jenkinsfile                     # CI pipeline (ENV + SUITE params)
├── pom.xml                         # Maven config (Java 21, TestNG, Playwright)
├── TEST_PLAN.md                    # Full test plan + roadmap
├── TEST_CASES.xlsx                 # Test case register (automated + manual)
├── QUICK_REFERENCE.md              # This file
├── README.md                       # Getting started
├── MICROPHONE_TESTING_GUIDE.md    # Future voice/microphone reference
└── BDD_FRAMEWORKS_GUIDE.md        # Future BDD options reference
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

### How the report works

Extent Report generates a **single self-contained `index.html`** — no server needed, open it in any browser, attach it to an email.

| Step | What happens |
|---|---|
| `mvn clean test` | Tests run; `target/extent-report/index.html` is generated automatically |
| Jenkins pipeline | Report is published as a build link and emailed as an attachment |

### View report locally
```bash
# Run tests — report generates automatically
mvn clean test -DtestGroups=smoke

# Open report (Windows)
start target\extent-report\index.html
```

The report shows:
- Pass / Fail / Skip counts with a visual timeline
- Full stack trace for every failure
- Test categories (smoke / regression / api) as filterable tags
- System info: environment, suite, API URL, Java version

### Jenkins reports

After each pipeline run:
- **Test trend graph** — JUnit plugin, visible on the job's main page
- **"Test Report" link** — Extent HTML published as a Jenkins build artifact
- **Email** — sent automatically with `index.html` attached (open in browser)

---

## Jenkins Setup Checklist (DevOps)

### Plugins required
| Plugin | Purpose |
|---|---|
| **HTML Publisher** | Publishes `target/extent-report/index.html` as a Jenkins build link |
| **Email Extension (emailext)** | Sends email with the HTML report attached |

Install at: **Manage Jenkins → Plugins → Available plugins**

### SMTP email configuration
Go to **Manage Jenkins → Configure System → Extended E-mail Notification**:

| Setting | Example |
|---|---|
| SMTP server | `smtp.gmail.com` or your company SMTP |
| SMTP port | `587` (TLS) |
| Credentials | Add Jenkins credential with SMTP username/password |
| Default content type | `HTML (text/html)` |

### Jenkins Global Environment Variables required
Go to **Manage Jenkins → Configure System → Global properties → Environment variables**:

| Variable | Example Value | Purpose |
|---|---|---|
| `STAGING_API_URL` | `http://staging-server:9090` | Staging API base URL |
| `PROD_API_URL` | `http://98.93.75.232:9090` | Prod API base URL |
| `AUTOMATION_EMAIL_TO` | `team@joshsoftware.com` | Report recipient(s) — comma-separated for multiple |

---

## Common Issues

| Issue | Cause | Fix |
|---|---|---|
| `Connection refused` | API server not running | Start the server or check the URL |
| `apiClient is null / NPE` | `@BeforeMethod` not running during group filter | Ensure `@BeforeMethod(alwaysRun = true)` in BaseApiPage |
| `Cannot run program "sh"` | Jenkins on Windows using Linux shell command | Jenkinsfile uses `bat`, not `sh` |
| `No test report files found` | Tests did not run (compile or setup error) | Fix the prior error; surefire XML only appears on successful run |
| `invalid target release: 21` | VS Code terminal has old JAVA_HOME | Set `$env:JAVA_HOME` to JDK 21 path or restart VS Code |
| `No tests found` | Test class doesn't end with "Test" | Rename: `API1Test.java` ✅, `API1.java` ❌ |

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
