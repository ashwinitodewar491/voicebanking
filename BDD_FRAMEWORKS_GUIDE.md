# BDD Framework Options for API-Only Testing (No Cucumber)

## Overview

Three approaches to add BDD to your API tests without Cucumber:

### Option 1: JBehave (Recommended) ⭐
- **Pros**: True BDD framework, Gherkin-like syntax, Java-native
- **Cons**: Requires file setup
- **Best for**: Stakeholder communication

### Option 2: Enhanced JUnit 5 (Lightweight) ⭐⭐
- **Pros**: Already using it, minimal overhead, clean
- **Cons**: Less formal BDD structure
- **Best for**: Dev-focused teams

### Option 3: AssertJ + JUnit 5 Custom Runner (Advanced)
- **Pros**: Full customization, powerful assertions
- **Cons**: More code to maintain
- **Best for**: Complex scenarios

---

## Option 1: JBehave Implementation

### Step 1: Add JBehave to pom.xml

```xml
<dependency>
    <groupId>org.jbehave</groupId>
    <artifactId>jbehave-core</artifactId>
    <version>5.1.1</version>
    <scope>test</scope>
</dependency>
```

### Step 2: Create Story Files

**src/test/resources/stories/api_1_account_list.story**
```gherkin
Meta:
@API
@AccountManagement

Narrative:
As a banking customer
I want to retrieve my account list
So that I can see all my accounts

Scenario: Customer retrieves their account list
Given I have a valid customer ID CIF202602260001
When I request the account list
Then the response should contain a successful status
And the response should include at least one account
And each account should have accountId, accountType, and status fields

Scenario: Account list should contain valid account types
Given I have a valid customer ID CIF202602260001
When I request the account list
Then each account type should be one of: SAVINGS, CURRENT, INVESTMENT
And each account status should be one of: ACTIVE, INACTIVE, CLOSED

Scenario: Handle empty account list gracefully
Given I have a customer ID NONEXISTENT123
When I request the account list
Then the response should handle the request appropriately
```

### Step 3: Create Steps Definition Class

**src/test/java/com/voicebanking/bdd/APISteps.java**
```java
package com.voicebanking.bdd;

import com.voicebanking.utils.APIClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.jbehave.core.annotations.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

public class APISteps {
    private APIClient apiClient;
    private JsonNode lastResponse;
    private String customerId;
    private String accountId;
    private String baseURL;

    @Given("I have a valid customer ID $customerId")
    public void givenCustomerID(String id) {
        this.customerId = id;
        this.baseURL = System.getenv("API_BASE_URL") != null ? 
            System.getenv("API_BASE_URL") : "http://localhost:8007";
        apiClient = new APIClient(baseURL);
    }

    @Given("I have a customer ID $customerId")
    public void givenAnyCustomerID(String id) {
        givenCustomerID(id);
    }

    @When("I request the account list")
    public void whenRequestAccountList() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("customerId", customerId);
        lastResponse = apiClient.post("/api/v1/accounts/list", requestBody);
    }

    @Then("the response should contain a successful status")
    public void thenResponseSuccessful() {
        assertEquals("success", lastResponse.get("status").asText());
        assertEquals(200, lastResponse.get("statusCode").asInt());
    }

    @Then("the response should include at least one account")
    public void thenResponseHasAccounts() {
        assertTrue(lastResponse.get("data").has("accountList"));
        assertTrue(lastResponse.get("data").get("accountList").size() > 0);
    }

    @Then("each account should have $fields fields")
    public void thenAccountsHaveFields(String fields) {
        String[] fieldNames = fields.split(", ");
        JsonNode accountList = lastResponse.get("data").get("accountList");
        
        for (JsonNode account : accountList) {
            for (String field : fieldNames) {
                assertTrue(account.has(field), 
                    "Account missing field: " + field);
            }
        }
    }

    @Then("each account type should be one of: $types")
    public void thenAccountTypesValid(String types) {
        String[] validTypes = types.split(", ");
        JsonNode accountList = lastResponse.get("data").get("accountList");
        
        for (JsonNode account : accountList) {
            String type = account.get("accountType").asText();
            assertTrue(java.util.Arrays.asList(validTypes).contains(type),
                "Invalid account type: " + type);
        }
    }

    @Then("each account status should be one of: $statuses")
    public void thenAccountStatusValid(String statuses) {
        String[] validStatuses = statuses.split(", ");
        JsonNode accountList = lastResponse.get("data").get("accountList");
        
        for (JsonNode account : accountList) {
            String status = account.get("status").asText();
            assertTrue(java.util.Arrays.asList(validStatuses).contains(status),
                "Invalid account status: " + status);
        }
    }

    @Then("the response should handle the request appropriately")
    public void thenResponseHandled() {
        // Accept either empty list or error
        if (lastResponse.get("statusCode").asInt() == 200) {
            assertTrue(lastResponse.get("data").has("accountList"));
        }
    }
}
```

### Step 4: Create Test Runner

**src/test/java/com/voicebanking/bdd/AccountAPIStoryTest.java**
```java
package com.voicebanking.bdd;

import org.jbehave.core.Embeddable;
import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.configuration.MostUsefulConfiguration;
import org.jbehave.core.io.LoadFromClasspath;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.jbehave.core.steps.InstanceStepsFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.jbehave.core.reporters.Format.*;

@DisplayName("Account API BDD Tests")
public class AccountAPIStoryTest extends org.jbehave.core.junit.JUnitStories {

    @Override
    public Configuration configuration() {
        return new MostUsefulConfiguration()
                .useStoryLoader(new LoadFromClasspath(this.getClass()))
                .useStoryReporterBuilder(new StoryReporterBuilder()
                        .withFormats(CONSOLE, HTML)
                        .withDefaultFormats());
    }

    @Override
    public InjectableStepsFactory stepsFactory() {
        return new InstanceStepsFactory(configuration(), new APISteps());
    }

    @Override
    protected List<String> storyPaths() {
        return new StoryFinder()
                .findPaths(codeLocationFromClass(this.getClass())
                        .getFile().getAbsolutePath(), 
                "stories/api_*.story", "");
    }
}
```

---

## Option 2: Enhanced JUnit 5 with BDD Style (Recommended for Your Project)

### Better Approach: BDD-Style JUnit 5

Since you're already using JUnit 5, enhance it with BDD semantics:

**src/test/java/com/voicebanking/bdd/BDDAPITest.java**
```java
package com.voicebanking.bdd;

import com.voicebanking.utils.APIClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

@DisplayName("Account List API - BDD Tests")
public class BDDAPITest {
    private APIClient apiClient;
    private String baseURL;

    @BeforeEach
    public void setup() {
        baseURL = System.getenv("API_BASE_URL") != null ? 
            System.getenv("API_BASE_URL") : "http://localhost:8007";
        apiClient = new APIClient(baseURL);
    }

    @Nested
    @DisplayName("Given a valid customer ID")
    class ValidCustomerScenario {
        private JsonNode response;

        @BeforeEach
        public void givenValidCustomer() throws Exception {
            // GIVEN: Customer ID is valid
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("customerId", "CIF202602260001");
            
            // WHEN: Request account list
            response = apiClient.post("/api/v1/accounts/list", requestBody);
        }

        @Test
        @DisplayName("When requesting account list, Then response is successful")
        public void testSuccessfulResponse() {
            // THEN: Response should be successful
            assertEquals("success", response.get("status").asText());
            assertEquals(200, response.get("statusCode").asInt());
        }

        @Test
        @DisplayName("When requesting account list, Then account list contains accounts")
        public void testAccountListNotEmpty() {
            // THEN: Account list should not be empty
            assertTrue(response.get("data").has("accountList"));
            assertTrue(response.get("data").get("accountList").size() > 0);
        }

        @Test
        @DisplayName("When requesting account list, Then each account has required fields")
        public void testAccountFields() {
            // THEN: Each account should have required fields
            JsonNode accountList = response.get("data").get("accountList");
            
            for (JsonNode account : accountList) {
                assertTrue(account.has("accountId"));
                assertTrue(account.has("accountType"));
                assertTrue(account.has("status"));
            }
        }
    }

    @Nested
    @DisplayName("Given an invalid customer ID")
    class InvalidCustomerScenario {
        private JsonNode response;

        @BeforeEach
        public void givenInvalidCustomer() throws Exception {
            // GIVEN: Customer ID is invalid
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("customerId", "INVALID123");
            
            // WHEN: Request account list
            response = apiClient.post("/api/v1/accounts/list", requestBody);
        }

        @Test
        @DisplayName("When requesting account list, Then response is handled appropriately")
        public void testErrorHandling() {
            // THEN: Should handle gracefully
            assertNotNull(response);
            assertTrue(response.has("status"));
        }
    }
}
```

---

## Option 3: Custom BDD Context Runner

**src/test/java/com/voicebanking/bdd/BDDContext.java**
```java
package com.voicebanking.bdd;

import com.voicebanking.utils.APIClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;

public class BDDContext {
    private APIClient apiClient;
    private JsonNode lastResponse;
    private int lastStatusCode;
    private Map<String, String> requestBody;
    private String lastErrorMessage;

    public BDDContext(String baseURL) {
        this.apiClient = new APIClient(baseURL);
        this.requestBody = new HashMap<>();
    }

    // GIVEN - Setup methods
    public void givenCustomerID(String customerId) {
        requestBody.put("customerId", customerId);
    }

    public void givenAccountID(String accountId) {
        requestBody.put("accountId", accountId);
    }

    // WHEN - Action methods
    public void whenRequestAccountList() throws Exception {
        lastResponse = apiClient.post("/api/v1/accounts/list", requestBody);
    }

    public void whenRequestCustomerInfo() throws Exception {
        lastResponse = apiClient.post("/api/v1/customers/info", requestBody);
    }

    public void whenRequestBalance() throws Exception {
        lastResponse = apiClient.post("/api/v1/accounts/balance", requestBody);
    }

    // THEN - Assertion methods
    public void thenResponseIsSuccessful() {
        assert lastResponse.get("status").asText().equals("success");
        assert lastResponse.get("statusCode").asInt() == 200;
    }

    public void thenResponseContains(String field) {
        assert lastResponse.get("data").has(field);
    }

    public void thenResponseFieldMatches(String field, String expectedValue) {
        String actualValue = lastResponse.get("data").get(field).asText();
        assert actualValue.equals(expectedValue);
    }

    // Helper methods
    public JsonNode getLastResponse() {
        return lastResponse;
    }

    public void reset() {
        requestBody.clear();
        lastResponse = null;
    }
}
```

**src/test/java/com/voicebanking/bdd/BDDTest.java**
```java
package com.voicebanking.bdd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BDD API Tests with Custom Context")
public class BDDTest {
    private BDDContext context;

    @BeforeEach
    public void setup() {
        String baseURL = System.getenv("API_BASE_URL") != null ? 
            System.getenv("API_BASE_URL") : "http://localhost:8007";
        context = new BDDContext(baseURL);
    }

    @Test
    @DisplayName("Scenario: Get account list for valid customer")
    public void scenarioGetAccountList() throws Exception {
        // GIVEN
        context.givenCustomerID("CIF202602260001");
        
        // WHEN
        context.whenRequestAccountList();
        
        // THEN
        context.thenResponseIsSuccessful();
        context.thenResponseContains("accountList");
    }

    @Test
    @DisplayName("Scenario: Get customer info")
    public void scenarioGetCustomerInfo() throws Exception {
        // GIVEN
        context.givenCustomerID("CIF202602260001");
        
        // WHEN
        context.whenRequestCustomerInfo();
        
        // THEN
        context.thenResponseIsSuccessful();
        context.thenResponseContains("name");
        context.thenResponseContains("email");
    }
}
```

---

## Comparison

| Feature | Option 1 (JBehave) | Option 2 (JUnit 5 BDD) | Option 3 (Context) |
|---------|-------------------|----------------------|-------------------|
| **Setup Complexity** | High | Low | Medium |
| **Gherkin Support** | ✅ Yes | ❌ No | ❌ No |
| **Learning Curve** | Steep | Flat | Medium |
| **Non-technical Users** | ✅ Can read stories | ⚠️ Can read test names | ❌ No |
| **Java Dev Experience** | Moderate | Great | Great |
| **Best for API-only** | ⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| **Maintenance** | Low | Very Low | Medium |
| **Reusability** | High | Medium | High |

---

## My Recommendation for Your Project

### **Use Option 2: Enhanced JUnit 5 with BDD Style** ⭐⭐⭐

**Why:**
1. ✅ Already using JUnit 5
2. ✅ No additional dependencies
3. ✅ Clean @Nested classes provide BDD structure
4. ✅ Perfect for API-only testing
5. ✅ Easy to maintain
6. ✅ Test names are self-documenting

**Example Structure:**
```
BDDAPITest (Main test class)
├── ValidCustomerScenario @Nested
│   ├── testSuccessfulResponse()
│   ├── testAccountListNotEmpty()
│   └── testAccountFields()
└── InvalidCustomerScenario @Nested
    └── testErrorHandling()
```

This gives you:
- 📖 BDD narrative (via DisplayName)
- 🧩 Modular scenarios (via @Nested)
- 🎯 Clear Given-When-Then flow
- 📝 Self-documenting code
- ✨ No extra frameworks to learn

---

## Migration Path

If you want to start with Option 2 and later upgrade to JBehave:

1. **Week 1-2**: Use Option 2 (JUnit 5 BDD)
2. **Week 3**: Add JBehave alongside (Option 1)
3. **Week 4+**: Gradually migrate or keep both

This gives you flexibility without breaking changes.

