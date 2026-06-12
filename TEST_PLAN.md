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

**In Scope — Future (UI Automation)**
- Login, Dashboard, Transaction, Transfer, Loan UI flows
- Voice / Microphone permission and command testing
- Playwright Browser Automation

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

---

## 4. Future Plan — UI Automation

### 4.1 Why Playwright for UI

| Requirement | Playwright Support |
|---|---|
| API + UI in one framework | Native — `APIRequestContext` + `Page` |
| Microphone permission for voice banking | `context.grantPermissions(["microphone"])` |
| Cross-browser (Chrome, Firefox, Safari) | Built-in |
| Java / Maven | Full support |
| Video recording | Built-in |
| CI/CD headless mode | Default headless mode |

### 4.2 Planned UI Test Coverage

**Phase 1 — Core Flows**
- Login / Logout
- Dashboard — account list display
- Account balance screen
- Transaction history with pagination

**Phase 2 — Transactions**
- Transfer money end-to-end UI flow
- Beneficiary add / edit
- Transfer success / error messages

**Phase 3 — Voice Features**
- Microphone permission grant via Playwright context
- Voice command simulation (balance, transfer, beneficiaries)
- Speech recognition response validation

**Phase 4 — Quality & Coverage**
- Cross-browser runs (Chrome, Firefox)
- Accessibility checks
- Session timeout handling
- Error state UI validation

### 4.3 UI Group Strategy (Planned)

```
ui-smoke      → critical login + dashboard check
ui-regression → all UI flows
ui-voice      → microphone and voice command tests
```

### 4.4 Proposed BaseUIPage (Future)

```java
public class BaseUIPage {

    protected Page page;
    protected BrowserContext context;

    @BeforeMethod(alwaysRun = true)
    public void setupBrowser() {
        Playwright pw = Playwright.create();
        Browser browser = pw.chromium().launch(
            new BrowserType.LaunchOptions().setHeadless(true)
        );
        context = browser.newContext(
            new Browser.NewContextOptions()
                .setPermissions(List.of("microphone"))
        );
        page = context.newPage();
        page.navigate(System.getenv("UI_BASE_URL"));
    }

    @AfterMethod(alwaysRun = true)
    public void closeBrowser() {
        context.close();
    }
}
```

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
          │    UI Automation     │  ~20% (Planned — Playwright)
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
| Phase 4: UI Test Framework | 🔜 Planned | BaseUIPage, browser setup, page objects |
| Phase 5: UI Tests | 🔜 Planned | Login, Dashboard, Transfer, Loan flows |
| Phase 6: Voice/Microphone | 🔜 Planned | Playwright permission + voice command tests |
| Phase 7: Allure Reporting | 🔜 Planned | Rich HTML reports with screenshots |
