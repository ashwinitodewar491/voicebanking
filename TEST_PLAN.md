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
- Voice Balance Inquiry — 43-query regression suite with account disambiguation handling (UI7, `botverification` group)
- Playwright Chromium with `--use-file-for-fake-audio-capture` for mic simulation

**In Scope — Future (UI Automation)**
- Transaction, Transfer, Loan UI flows beyond balance inquiry
- Voice commands for transfers and beneficiaries
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

---

## 4. UI Automation — Current & Planned

### 4.1 Why Playwright for UI

| Requirement | Playwright Support |
|---|---|
| API + UI in one framework | Native — `APIRequestContext` + `Page` |
| Microphone permission for voice banking | `context.grantPermissions(["microphone"])` |
| Fake mic input for voice queries | Chromium `--use-file-for-fake-audio-capture=<wav>` |
| Java / Maven | Full support |
| Video recording | Built-in |
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
| `UI7_BalanceInquiryTest` | 43 real voice balance queries (fake-mic WAV → STT → bot response), incl. account disambiguation follow-up (savings/current) with retry | `ui`, `regression`, `botverification` |

`BaseVoiceTest` (used by UI7) owns the Chromium+fake-audio lifecycle and the shared `runVoiceQuery(...)` flow: navigate → login → speak query → assert bot response, with automatic retry on transcription mismatch and on account-disambiguation follow-up.

Run just the voice-verification suite:
```bash
mvn clean test -DtestGroups=botverification
```

### 4.3 Planned — Not Yet Automated

**Transactions & Transfers**
- Transfer money end-to-end UI flow
- Beneficiary add / edit
- Transfer success / error messages

**Voice Features Beyond Balance**
- Voice command simulation for transfers, beneficiaries
- Multi-locale voice queries

**Quality & Coverage**
- Cross-browser runs (Chrome, Firefox)
- Accessibility checks
- Session timeout handling
- Error state UI validation

### 4.4 Known Limitation — Fake Audio Follow-ups

`--use-file-for-fake-audio-capture` loads the WAV file into memory and loops it; overwriting the file on disk for a disambiguation follow-up ("savings"/"current") doesn't always get picked up before the next hold-to-speak window closes. `BaseVoiceTest` mitigates this with up to 3 retries (increasing pre-wait each attempt) rather than asserting on the first attempt. This is a framework-level workaround, not a product bug — keep it in mind if new voice flows need a second spoken input.

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
| Phase 5: UI Tests | 🔄 In Progress | Welcome/Login/OTP/Language/VoiceRegistration/Home done (UI1–UI6); Transfer/Loan UI flows planned |
| Phase 6: Voice/Microphone | 🔄 In Progress | Fake-audio-capture + account disambiguation (with retry) working for Balance Inquiry (UI7, `botverification` group); transfer/beneficiary voice commands planned |
| Phase 7: Allure Reporting | 🔜 Planned | Rich HTML reports with screenshots |
