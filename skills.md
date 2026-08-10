# VoiceBanking QA Automation — AI Skills

This file teaches AI coding assistants (Claude Code, Cursor, GitHub Copilot, Ollama) how to generate, extend, and maintain API and UI/voice tests for this project.
Always follow these patterns exactly — do not introduce new libraries, frameworks, or abstractions.

Jump to: [API rules](#project-stack) · [UI / Voice rules](#ui--voice-automation-rules)

---

## Project Stack

- **Language:** Java 21
- **Test framework:** TestNG 7.x
- **HTTP client:** `java.net.http.HttpClient` (via `APIClient`)
- **Assertions:** TestNG `Assert`
- **JSON parsing:** Jackson `JsonNode`
- **Reporting:** Extent Reports 5.x (via `TestListener`)
- **Build:** Maven (`mvn clean test`)

---

## Project Structure

```
src/test/java/com/voicebanking/
├── DataText/
│   ├── Endpoints.java      # Base URLs + endpoint paths + getBaseUrl()
│   └── Constants.java      # All test data, expected values, field names
├── utils/
│   └── APIClient.java      # HTTP client — post(), get(), getPostStatus()
├── pages/
│   └── BaseApiPage.java    # TestNG @BeforeMethod — sets up apiClient
├── listeners/
│   └── TestListener.java   # Extent report wiring — do not modify
└── tests/api/
    └── API{N}_{Feature}Test.java   # One file per API endpoint
```

---

## Naming Conventions

| What | Pattern | Example |
|---|---|---|
| Test class | `API{N}_{FeatureName}Test` | `API1_GetAccountListTest` |
| Test method | `test{WhatIsVerified}` | `testGetAccountList` |
| Private helper | `{verb}{Resource}` | `getAccountListResponse()` |
| Constant | `SCREAMING_SNAKE_CASE` | `ACCOUNT_LIST_SUCCESS_MESSAGE` |
| Endpoint path | `SCREAMING_SNAKE_CASE` | `ACCOUNT_LIST` |

---

## Adding a New API Test — Step-by-Step

### 1. Add the endpoint path to `Endpoints.java`

```java
public static final String YOUR_ENDPOINT
        = "/api/v1/resource/action";
```

### 2. Add constants to `Constants.java`

```java
// Success message
public static final String YOUR_SUCCESS_MESSAGE = "Resource fetched successfully";

// Test data
public static final String YOUR_INPUT_ID = "EXPECTED_ID";

// Field names (for has() checks)
public static final String YOUR_FIELD = "fieldName";
```

### 3. Create the test class

```java
package com.voicebanking.tests.api;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.voicebanking.DataText.Constants;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.pages.BaseApiPage;

public class API{N}_{FeatureName}Test extends BaseApiPage {

    private JsonNode {helperMethodName}() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("fieldName", Constants.YOUR_INPUT_ID);

        return apiClient.post(
                Endpoints.YOUR_ENDPOINT,
                requestBody);
    }

    @Test(groups = {"smoke", "regression", "api"}, description = "Verify {what this test checks}")
    public void test{Feature}() throws Exception {
        JsonNode response = {helperMethodName}();

        // Always verify these four fields first
        Assert.assertEquals(response.get("status").asText(), Constants.SUCCESS_STATUS);
        Assert.assertEquals(response.get("statusCode").asInt(), Constants.SUCCESS_STATUS_CODE);
        Assert.assertTrue(response.has("message"), Constants.MESSAGE_EXIST);
        Assert.assertEquals(response.get("message").asText(), Constants.YOUR_SUCCESS_MESSAGE);

        // Then verify data fields
        Assert.assertTrue(response.has("data"));
    }

    @Test(groups = {"regression"}, description = "Verify {field-level detail}")
    public void testValidate{Feature}Details() throws Exception {
        JsonNode response = {helperMethodName}();
        JsonNode data = response.get("data");

        Assert.assertFalse(data.isEmpty(), "{Resource} should not be empty");
        // field-level assertions here
    }
}
```

---

## Test Groups

Every `@Test` must declare groups. Use these exact values:

| Group | When to use |
|---|---|
| `smoke` | Core happy-path — runs on every deployment trigger |
| `regression` | Full validation — runs on scheduled/manual runs |
| `api` | All API tests (use alongside smoke or regression) |

Smoke tests should always also be tagged `regression`.

---

## APIClient Usage

```java
// POST with body
JsonNode response = apiClient.post(Endpoints.YOUR_ENDPOINT, requestBody);

// GET (no body)
JsonNode response = apiClient.get(Endpoints.YOUR_ENDPOINT);

// Get HTTP status code only
int status = apiClient.getPostStatus(Endpoints.YOUR_ENDPOINT, requestBody);
```

`requestBody` is always `Map<String, String>` or `Map<String, Object>` (use Object when values include numbers or booleans).

---

## Assertion Patterns

**Standard response envelope — always assert these first:**
```java
Assert.assertEquals(response.get("status").asText(),    Constants.SUCCESS_STATUS);
Assert.assertEquals(response.get("statusCode").asInt(), Constants.SUCCESS_STATUS_CODE);
Assert.assertTrue(response.has("message"),              Constants.MESSAGE_EXIST);
Assert.assertEquals(response.get("message").asText(),   Constants.YOUR_SUCCESS_MESSAGE);
```

**Array field:**
```java
JsonNode list = response.get("data").get("itemList");
Assert.assertTrue(list.isArray());
Assert.assertFalse(list.isEmpty(), "List should not be empty");
```

**Field presence:**
```java
Assert.assertTrue(item.has("fieldName"));
Assert.assertFalse(item.get("fieldName").asText().isBlank(), "fieldName should not be empty");
```

**Numeric with delta (for balances/amounts):**
```java
Assert.assertEquals(actualDouble, expectedDouble, 0.01);
```

**Loop over array items:**
```java
for (JsonNode item : list) {
    Assert.assertTrue(item.has("id"));
    Assert.assertFalse(item.get("id").asText().isBlank(), "ID should not be empty");
}
```

**Chained flow (transfer-style):**
```java
// Given
double balanceBefore = getBalance(...);

// When
JsonNode response = callTransferApi(...);

// Then
Assert.assertEquals(response.get("status").asText(), Constants.SUCCESS_STATUS);
double balanceAfter = getBalance(...);
Assert.assertEquals(balanceAfter, balanceBefore - amount, 0.01);

// Cleanup
reverseTransfer(...);
Assert.assertEquals(getBalance(...), balanceBefore, 0.01);
```

---

## Environment & URL Management

URL is resolved automatically from `-Denv` Maven property:

```java
// In Endpoints.java — already implemented
public static String getBaseUrl() {
    String env = System.getProperty("env", System.getenv("ENV") != null ? System.getenv("ENV") : "prod");
    return "stage".equalsIgnoreCase(env) ? BASE_URL_STAGE : BASE_URL_PROD;
}
```

Run commands:
```bash
mvn clean test                                        # prod (default)
mvn clean test -Denv=stage                           # stage
mvn clean test -Denv=prod                            # prod explicit
mvn clean test -Denv=stage -DtestGroups=smoke        # stage + smoke only
```

---

## What NOT to Do

- Do not use RestAssured, OkHttp, or any HTTP library other than `APIClient`
- Do not use JUnit — only TestNG
- Do not use `@BeforeClass` or `@AfterClass` — use `@BeforeMethod` in `BaseApiPage` only
- Do not hardcode URLs in test classes — always use `Endpoints.*`
- Do not hardcode test data strings in test classes — always add to `Constants.*`
- Do not modify `TestListener.java` or `BaseApiPage.java` for individual test needs
- Do not create helper classes outside the existing package structure
- Do not add comments that describe what the code does — only add comments for non-obvious constraints

---

## UI / Voice Automation Rules

### Project Structure

```
src/test/java/com/voicebanking/
├── tests/ui/
│   ├── base/
│   │   └── BaseVoiceTest.java   # Browser lifecycle + shared voice-query flow — extend ONLY for voice-driven tests
│   └── UI{N}_{FeatureName}Test.java   # One file per screen/flow (UI1_WelcomePageTest ... UI12_MultilingualVoiceQueryTest)
├── pages/
│   ├── BasePage.java             # Shared Playwright browser setup for non-voice UI tests
│   ├── HomePage.java, WelcomePage.java, OtpPage.java, LanguagePage.java, VoiceRegistrationPage.java
├── DataText/
│   ├── VoiceQueries.java         # All spoken query text, grouped by locale (English, ...)
│   └── BotResponsePatterns.java  # Regex patterns for bot responses (Balance.ANY/SAVINGS/CURRENT, ...)
└── utils/
    └── TtsUtil.java               # Generates/deletes WAV files for fake-mic input, computes duration
```

### Naming Conventions

| What | Pattern | Example |
|---|---|---|
| Test class | `UI{N}_{FeatureName}Test` | `UI7_BalanceInquiryTest` |
| Test method | `test{WhatIsVerified}` | `testVoiceQuery` |
| Page object | `{Screen}Page` | `HomePage` |
| Voice query constant | `SCREAMING_SNAKE_CASE` describing the phrase | `VoiceQueries.English.CURRENT_BALANCE` |

### Test Groups

| Group | When to use |
|---|---|
| `ui` | All UI tests |
| `regression` | Full validation — runs on scheduled/manual runs |
| `botverification` | Voice-query bot-response regression suite (`UI7_BalanceInquiryTest`, `UI8_TransactionHistoryTest`, `UI9_LoanInquiryTest`) — run with `-DtestGroups=botverification` |
| `botverificationTransferMoney` | Voice transfer-money bot-response regression suite (`UI10_TransferMoneyTest`) |
| `multilingual` | Non-English voice queries (`UI12_MultilingualVoiceQueryTest`) — not part of `regression`, run explicitly |

### Adding a Voice Query Test — Step-by-Step

1. Add the phrase to `VoiceQueries.java` (English, or the relevant locale class).
2. Add the expected bot-response regex to `BotResponsePatterns.java` if a new pattern is needed — reuse `Balance.ANY/SAVINGS/CURRENT` where possible instead of inlining regex literals in the test class.
3. Add a row to the test's `@DataProvider`. Rows are **5-tuples**:
   ```java
   {"Query Name", VoiceQueries.English.YOUR_QUERY,
           new String[]{"keyword1", "keyword2"}, BotResponsePatterns.Balance.ANY, "savings"}
   ```
   - `disambiguationAccount` (5th field) is `"savings"` or `"current"` when the query is **ambiguous** (bot may ask "which account?"), and `null` when the query already names an account explicitly (e.g. "current account balance").
   - When adding several ambiguous queries at once, **alternate** `"savings"`/`"current"` across rows so both disambiguation branches get exercised — don't default every row to the same value.
4. Call `runVoiceQuery(queryName, query, expectedKeywords, assertionPattern, disambiguationAccount)` from the `@Test` method. Do not duplicate the navigate → login → speak → assert flow inline in a test class.

### BaseVoiceTest Rules

- Any test that drives audio through the "hold to speak" flow must extend `BaseVoiceTest`, not `BasePage`.
- `@BeforeMethod` launches Chromium with `--use-file-for-fake-audio-capture` pointed at a generated WAV. `headless` defaults to `true` via the `headless` system property — **never hardcode** `setHeadless(...)`; pass `-Dheadless=false` locally to watch the browser run.
- `isAccountDisambiguation()` matches several bot phrasings: "choose ... account", "which ... account", or a response that names both `CURRENT` and `SAVINGS`. If you hit a new phrasing that isn't caught, extend that method — don't special-case it inside `runVoiceQuery`.
- The disambiguation follow-up retries up to 3 times before asserting. This exists because `--use-file-for-fake-audio-capture` loops whatever WAV content it already loaded into memory, and doesn't always pick up an overwritten file before the hold-to-speak window closes. Keep the retry loop when touching that block — do not collapse it back to a single attempt.
- Transcription/keyword matching (`transcriptionContainsExpectedWords`, `containsAnyKeyword`) is already case-insensitive — don't add extra `.toLowerCase()` calls at the call site.

### What NOT to Do (UI additions)

- Do not hardcode `setHeadless(true)` or `setHeadless(false)` in a test or page object — always read the `headless` system property with the existing default pattern.
- Do not hardcode a disambiguation follow-up word — always take it from the DataProvider's `disambiguationAccount` param.
- Do not skip `BaseVoiceTest.tearDown` cleanup (WAV deletion, browser close) when adding new voice test classes.
- Do not add a case-sensitive string comparison for transcribed speech or bot response text.

---

## Jenkins Trigger

Post-deployment automation is triggered via:
```bash
curl -X POST "http://JENKINS_URL/job/voicebanking-automation/buildWithParameters" \
  --user "username:api_token" \
  --data "token=voicebanking-trigger&ENV=stage&SUITE=smoke"
```

ENV choices: `prod`, `stage`
SUITE choices: `smoke`, `regression`
