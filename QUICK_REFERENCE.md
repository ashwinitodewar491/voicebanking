# Voice Banking API Test Framework - Quick Reference

## Project Setup

### Prerequisites
- Java 17+
- Maven 3.8+
- Git

### Installation

```bash
# Clone/navigate to project
cd VoiceBankingAPI

# Install dependencies
mvn clean install

# Playwright will auto-download browsers
mvn exec:java -Dexec.mainClass="com.microsoft.playwright.CLI" -Dexec.args="install chromium"
```

---

## Running Tests

### Run All API Tests
```bash
mvn test
```

### Run Specific API Test
```bash
# Test API 1 - Get Account List
mvn test -Dtest=API1_GetAccountListTest

# Test API 2 - Get Customer Info
mvn test -Dtest=API2_GetCustomerInfoTest

# Test API 3 - Account Balance
mvn test -Dtest=API3_GetAccountBalanceTest
```

### Run All Tests with Custom Base URL
```bash
mvn test -DAPI_BASE_URL=http://your-server:8007
```

### Run Tests in Parallel
```bash
mvn test -DthreadCount=5
```

### Run Tests with Verbose Output
```bash
mvn test -X
```

---

## Test Coverage

| API | Test Class | Status |
|-----|-----------|--------|
| 1 | API1_GetAccountListTest.java | ✅ Ready |
| 2 | API2_GetCustomerInfoTest.java | ✅ Ready |
| 3 | API3_GetAccountBalanceTest.java | ✅ Ready |
| 4 | API4_GetBeneficiariesListTest.java | ✅ Ready |
| 5 | API5_GetTransactionsListTest.java | ✅ Ready |
| 6 | API6_TransferMoneyTest.java | ✅ Ready |
| 8 | API8_GetLoanStatementTest.java | ✅ Ready |
| 9 | API9_GetLoanOverdueDetailsTest.java | ✅ Ready |
| 10 | API10_GetLoanSummaryListTest.java | ✅ Ready |

---

## Project Structure

```
VoiceBankingAPI/
├── src/
│   └── test/
│       └── java/
│           └── com/voicebanking/
│               ├── tests/              # Test classes
│               │   ├── API1_GetAccountListTest.java
│               │   ├── API2_GetCustomerInfoTest.java
│               │   ├── API3_GetAccountBalanceTest.java
│               │   ├── API4_GetBeneficiariesListTest.java
│               │   ├── API5_GetTransactionsListTest.java
│               │   ├── API6_TransferMoneyTest.java
│               │   ├── API8_GetLoanStatementTest.java
│               │   ├── API9_GetLoanOverdueDetailsTest.java
│               │   └── API10_GetLoanSummaryListTest.java
│               ├── utils/              # Utilities
│               │   └── APIClient.java
│               └── pages/              # Page Objects (for UI testing)
│                   └── VoiceBankingWelcomePage.java
├── pom.xml                             # Maven configuration
├── TEST_PLAN.md                        # Comprehensive test plan
├── MICROPHONE_TESTING_GUIDE.md        # Voice/Microphone testing guide
└── README.md                           # This file
```

---

## Key Technologies

### Playwright Java
- **Version**: 1.50.0
- **Purpose**: API & UI automation with microphone support
- **Features**:
  - Native API request context (APIRequestContext)
  - Cross-browser support (Chromium, Firefox, Safari)
  - Browser permission management
  - Microphone permission handling

### Jackson
- **Version**: 2.16.0
- **Purpose**: JSON parsing and serialization
- **Usage**: Response validation, object mapping

### JUnit 5
- **Version**: 5.10.0
- **Purpose**: Test framework and assertions
- **Features**: DisplayName, BeforeEach, Test annotations

---

## API Test Details

### API 1: Get Account List
**Endpoint**: `POST /api/v1/accounts/list`
**Request**: `{ "customerId": "CIF202602260001" }`
**Tests**:
- Retrieve account list
- Verify account details structure
- Validate account types and status

### API 2: Get Customer Info
**Endpoint**: `POST /api/v1/customers/info`
**Request**: `{ "customerId": "CIF202602260001" }`
**Tests**:
- Retrieve customer information
- Verify required fields (name, email, mobile)
- Validate KYC status

### API 3: Get Account Balance
**Endpoint**: `POST /api/v1/accounts/balance`
**Request**: `{ "accountId": "ACC202602260001", "customerId": "CIF202602260001", "accountType": "SAVINGS" }`
**Tests**:
- Get account balance
- Verify masked account number
- Validate numeric balance value

### API 4: Get Beneficiaries List
**Endpoint**: `POST /api/v1/beneficiaries/list`
**Request**: `{ "customerId": "CIF202602260001", "accountId": "ACC202602260001" }`
**Tests**:
- Retrieve beneficiaries
- Verify beneficiary fields
- Validate status

### API 5: Get Transactions List
**Endpoint**: `POST /api/v1/transactions/list`
**Request**: `{ "accountId": "ACC202602260006", "toDate": "2026-05-26", "page": 0, "size": 100 }`
**Tests**:
- Get transaction list
- Verify pagination
- Validate transaction details
- Verify amount is numeric

### API 6: Transfer Money
**Endpoint**: `POST /api/v1/transactions/transfer`
**Request**: `{ "customerId": "CIF202602260001", "fromAccountId": "ACC202602260001", "beneficiaryId": "...", "amount": 1000.00, "description": "IMPS" }`
**Tests**:
- Execute transfer
- Verify transaction ID generated
- Validate balance after transfer
- Verify success status

### API 8: Get Loan Statement
**Endpoint**: `POST /api/v1/loans/statement`
**Request**: `{ "accountId": "LN10001", "fromDate": "2022-03-01", "toDate": "2026-09-01", "page": 0, "size": 10 }`
**Tests**:
- Get loan statement
- Verify transaction list
- Validate pagination
- Verify transaction amounts

### API 9: Get Loan Overdue Details
**Endpoint**: `POST /api/v1/loans/overdue`
**Request**: `{ "accountId": "LN10001" }`
**Tests**:
- Get overdue details
- Verify overdue amounts
- Validate tenure information
- Check customer information

### API 10: Get Loan Summary List
**Endpoint**: `POST /api/v1/loans/summary`
**Request**: `{ "customerId": "CIF202602260001" }`
**Tests**:
- Get loan summary
- Verify loan details
- Validate amounts
- Verify multiple loans

---

## Test Data

### Test Customers
| Customer ID | Name | Email | Mobile |
|------------|------|-------|--------|
| CIF202602260001 | Amit Sharma | amit.sharma@gmail.com | 9876543213 |
| CIF202602260002 | Customer 2 | customer2@gmail.com | 9876543214 |

### Test Accounts
| Account ID | Type | Customer ID | Balance |
|-----------|------|-----------|---------|
| ACC202602260001 | SAVINGS | CIF202602260001 | 45,230.75 |
| ACC202602260006 | SAVINGS | CIF202602260004 | Variable |

### Test Loans
| Loan ID | Type | Amount | Tenure |
|---------|------|--------|--------|
| LN10001 | HOME_LOAN | 500,000 | 240 months |
| LN10002 | PERSONAL_LOAN | 200,000 | 60 months |

---

## Debugging Tests

### Enable Debug Logging
```bash
mvn test -X
```

### Run with Browser UI (Not Headless)
```java
// In APIClient.java, modify browser launch:
browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
    .setHeadless(false)  // See browser actions
);
```

### Add Breakpoints
- Set breakpoints in test methods in IDE
- Run: `mvn test -Dtest=API1_GetAccountListTest` with debugger

### Print Response Details
```java
JsonNode response = apiClient.post("/api/v1/accounts/list", requestBody);
System.out.println("Full Response: " + response.toPrettyString());
System.out.println("Status: " + response.get("status").asText());
System.out.println("Data: " + response.get("data").toPrettyString());
```

---

## Common Issues

### Issue: Tests fail with "Connection refused"
**Cause**: API server not running
**Solution**: Start API server before running tests
```bash
# Terminal 1: Start API
mvn spring-boot:run

# Terminal 2: Run tests
mvn test
```

### Issue: "No tests found"
**Cause**: Test class naming convention not followed
**Solution**: Ensure test classes end with "Test"
```bash
# Correct: API1_GetAccountListTest.java
# Wrong: API1_GetAccountList.java
```

### Issue: "Jackson cannot deserialize"
**Cause**: Response format doesn't match expected structure
**Solution**: Verify API response structure matches test expectations
```bash
# Use Postman to test API endpoint first
# Verify response matches what test expects
```

### Issue: Tests timeout
**Cause**: API server slow or unreachable
**Solution**: Increase timeout in APIClient
```java
// In pom.xml, increase surefire timeout
<argLine>-Xmx1024m -DtestTimeout=60000</argLine>
```

---

## CI/CD Integration

### GitHub Actions
```yaml
# .github/workflows/test.yml
name: Run Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - run: mvn test
```

### Jenkins
```groovy
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }
    }
    post {
        always {
            junit 'target/surefire-reports/**/*.xml'
        }
    }
}
```

---

## Test Results & Reporting

### View Test Report
```bash
# Test results in:
target/surefire-reports/

# HTML Report (requires plugin):
target/site/surefire-report.html
```

### Maven Surefire Reports
```bash
mvn surefire-report:report
```

### Generate HTML Report
```bash
mvn site
open target/site/index.html
```

---

## Environment Configuration

### Set API Base URL (Default: http://localhost:8007)
```bash
# Command line
mvn test -DAPI_BASE_URL=http://production-api:8007

# Environment variable
export API_BASE_URL=http://staging-api:8007
mvn test

# application.properties
api.base.url=http://localhost:8007
```

### Set Java Version
```bash
export JAVA_HOME=/path/to/java17
mvn test
```

---

## Contributing

### Adding a New Test
1. Create test class: `APIxx_DescriptionTest.java`
2. Extend with setup method
3. Add @Test methods
4. Run: `mvn test -Dtest=APIxx_DescriptionTest`

### Test Naming Convention
```java
@DisplayName("API X - Operation")
public class API_X_OperationTest {
    
    @Test
    @DisplayName("Should [action] [expected result]")
    public void test[Action]() throws Exception {
        // Test code
    }
}
```

---

## Contact & Support

For issues or questions:
1. Check TEST_PLAN.md for comprehensive documentation
2. Review MICROPHONE_TESTING_GUIDE.md for voice features
3. Check CI logs for execution details
4. Enable debug logging with `-X` flag

---

## License

This project is part of Voice Banking API automation suite.

