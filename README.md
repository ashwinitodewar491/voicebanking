# Voice Banking API - Playwright Java Automation Suite

## Overview

A comprehensive test automation suite for Voice Banking APIs and UI using **Playwright with Java**. This framework provides unified API and UI testing with special support for **microphone access** for voice-based banking operations.

### Why Playwright?
✅ **Unified Framework** - API + UI testing in one tool  
✅ **Microphone Support** - Native browser permission management  
✅ **Voice Testing** - Speech recognition and audio streaming  
✅ **Java-based** - Enterprise-grade with Maven integration  
✅ **Fast Execution** - Parallel test execution  
✅ **Cross-browser** - Chrome, Firefox, Safari support  

---

## 🚀 Quick Start

```bash
# 1. Clone and navigate to project
cd VoiceBankingAPI

# 2. Install dependencies
mvn clean install

# 3. Run all tests
mvn test

# 4. Run specific test
mvn test -Dtest=API1_GetAccountListTest

# 5. Run with custom API URL
mvn test -DAPI_BASE_URL=http://your-server:8007
```

---

## 📋 Documentation

### 1. **TEST_PLAN.md** - Comprehensive Testing Strategy
- Project overview and scope
- Detailed comparison: Playwright vs Selenium vs Cypress
- Test pyramid and strategy
- Microphone access implementation
- Complete test execution architecture
- **When to read**: Understanding testing approach and why Playwright

### 2. **MICROPHONE_TESTING_GUIDE.md** - Voice Banking Automation
- Browser setup with microphone permissions
- Voice input simulation
- Audio level capture
- Voice command testing examples
- Advanced microphone testing techniques
- CI/CD integration examples
- **When to read**: Implementing voice/microphone features

### 3. **QUICK_REFERENCE.md** - Developer Cheat Sheet
- Installation and setup
- Test execution commands
- API endpoint details
- Test data reference
- Debugging techniques
- CI/CD pipeline examples
- **When to read**: Quick lookup while developing tests

---

## 📁 Project Structure

```
VoiceBankingAPI/
│
├── src/test/java/com/voicebanking/
│   ├── tests/                          # Test Classes (9 API tests)
│   │   ├── API1_GetAccountListTest.java
│   │   ├── API2_GetCustomerInfoTest.java
│   │   ├── API3_GetAccountBalanceTest.java
│   │   ├── API4_GetBeneficiariesListTest.java
│   │   ├── API5_GetTransactionsListTest.java
│   │   ├── API6_TransferMoneyTest.java
│   │   ├── API8_GetLoanStatementTest.java
│   │   ├── API9_GetLoanOverdueDetailsTest.java
│   │   └── API10_GetLoanSummaryListTest.java
│   │
│   ├── utils/                          # Reusable Utilities
│   │   └── APIClient.java              # Playwright API request wrapper
│   │
│   └── pages/                          # Page Objects (for UI testing)
│       └── VoiceBankingWelcomePage.java
│
├── pom.xml                             # Maven Configuration
├── TEST_PLAN.md                        # ⭐ START HERE - Comprehensive test plan
├── MICROPHONE_TESTING_GUIDE.md        # Voice/Microphone testing guide
├── QUICK_REFERENCE.md                 # Quick lookup reference
└── README.md                           # This file

```

---

## ✅ Supported APIs

| API | Endpoint | Method | Status |
|-----|----------|--------|--------|
| 1 | `/api/v1/accounts/list` | POST | ✅ Implemented |
| 2 | `/api/v1/customers/info` | POST | ✅ Implemented |
| 3 | `/api/v1/accounts/balance` | POST | ✅ Implemented |
| 4 | `/api/v1/beneficiaries/list` | POST | ✅ Implemented |
| 5 | `/api/v1/transactions/list` | POST | ✅ Implemented |
| 6 | `/api/v1/transactions/transfer` | POST | ✅ Implemented |
| 8 | `/api/v1/loans/statement` | POST | ✅ Implemented |
| 9 | `/api/v1/loans/overdue` | POST | ✅ Implemented |
| 10 | `/api/v1/loans/summary` | POST | ✅ Implemented |

**Not Included** (Per Requirements): APIs 7, 11, 12, 13

---

## 🛠 Technology Stack

### Core Framework
- **Playwright Java** (1.50.0) - API & UI Automation
- **JUnit 5** (5.10.0) - Test Framework
- **Jackson** (2.16.0) - JSON Processing
- **Java** (17) - Programming Language
- **Maven** (3.8+) - Build Tool

### Key Capabilities
```
┌─────────────────────────────────┐
│     Playwright (Multi-browser)   │
├─────────────────────────────────┤
│                                  │
│  ┌─────────────────────────────┐ │
│  │   APIRequestContext         │ │  ← API Testing
│  │ (HTTP POST/GET/etc)         │ │
│  └─────────────────────────────┘ │
│                                  │
│  ┌─────────────────────────────┐ │
│  │   BrowserContext + Page     │ │  ← UI Testing
│  │ (Navigation, Clicks, etc)   │ │
│  └─────────────────────────────┘ │
│                                  │
│  ┌─────────────────────────────┐ │
│  │   Permissions Management    │ │  ← Microphone Access
│  │ (Microphone, Geolocation)   │ │
│  └─────────────────────────────┘ │
│                                  │
└─────────────────────────────────┘
```

---

## 🎯 Test Features

### API Testing
- ✅ HTTP Method Support (POST, GET, PUT, DELETE)
- ✅ JSON Request/Response Handling
- ✅ Response Status Validation
- ✅ Data Structure Assertion
- ✅ Error Handling
- ✅ Custom Headers & Authentication

### UI Testing  
- ✅ Page Navigation
- ✅ Element Interaction (Click, Type, Select)
- ✅ Element Visibility Waiting
- ✅ Screenshot Capture
- ✅ Video Recording
- ✅ Network Interception

### Voice/Microphone Features
- ✅ Browser Permission Management
- ✅ Microphone Permission Grant/Revoke
- ✅ Audio Stream Access
- ✅ Voice Command Simulation
- ✅ Audio Level Capture
- ✅ Speech Recognition API Support

---

## 📊 Test Execution Examples

### Run All Tests
```bash
mvn test
```

### Run Specific API Tests
```bash
# Single test
mvn test -Dtest=API1_GetAccountListTest

# Multiple tests
mvn test -Dtest=API1_*Test,API2_*Test

# By pattern
mvn test -Dtest=*AccountTest
```

### Custom Configuration
```bash
# Custom API server
mvn test -DAPI_BASE_URL=http://staging:8007

# Parallel execution (5 threads)
mvn test -DthreadCount=5

# Skip tests during build
mvn clean install -DskipTests

# Fail on error
mvn test -fae
```

### Debug Mode
```bash
# Verbose output
mvn test -X

# With IDE debugger
mvn test -Dtest=API1_GetAccountListTest -Ddebug
```

---

## 🔧 Configuration

### Environment Variables
```bash
export API_BASE_URL=http://localhost:8007
export JAVA_HOME=/path/to/java17
export MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=512m"
```

### Maven Properties (pom.xml)
```xml
<properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <junit.jupiter.version>5.10.0</junit.jupiter.version>
    <playwright.version>1.50.0</playwright.version>
</properties>
```

---

## 🐛 Troubleshooting

### Common Issues

**Issue**: `Connection refused` - API not running
```bash
# Start API server first
# Then run tests
mvn test
```

**Issue**: `No tests found`
```bash
# Ensure test class ends with "Test"
# Correct: API1_GetAccountListTest.java
# Wrong: API1_GetAccountList.java
```

**Issue**: Microphone not working
```bash
# Set headless=false for local testing
# Ensure browser has microphone access permission
# See MICROPHONE_TESTING_GUIDE.md for details
```

**Issue**: Tests timeout
```bash
# Increase timeout in APIClient
# Or increase surefire timeout in pom.xml
mvn test -DtestTimeout=60000
```

See **QUICK_REFERENCE.md** for more troubleshooting tips.

---

## 📚 Documentation Flow

```
┌─ START HERE ──────────────┐
│                           │
│  README.md (this file)    │  ← Overview & Quick Start
│                           │
└──────────────┬────────────┘
               │
         Choose your path:
               │
    ┌──────────┼──────────┐
    │          │          │
    ▼          ▼          ▼
   
 Want to      Need to      Want Quick
understand   implement     command
why we use   voice/mic    reference?
Playwright?  features?

    │          │          │
    ▼          ▼          ▼

TEST_PLAN   MICROPHONE   QUICK_
.md         _TESTING_    REFERENCE
            GUIDE.md     .md

  ← Comprehensive    ← Voice Banking    ← Developer
    comparison         examples          cheat sheet
    & strategy         & techniques
```

---

## 🚀 Getting Started Steps

### 1. **First Time Setup** (5 min)
```bash
mvn clean install
mvn test -Dtest=API1_GetAccountListTest
```

### 2. **Understand the Architecture** (15 min)
Read: TEST_PLAN.md sections 1-3

### 3. **Run Full Test Suite** (10 min)
```bash
mvn test
```

### 4. **Set Up Voice Testing** (20 min)
Read: MICROPHONE_TESTING_GUIDE.md  
Implement: Browser setup with microphone

### 5. **Integrate with CI/CD** (15 min)
Read: QUICK_REFERENCE.md (CI/CD section)  
Implement: Jenkins/GitHub Actions

---

## 📈 Test Results & Reporting

### View Results
```bash
# Test output in console
# Results also saved in:
target/surefire-reports/
```

### Generate HTML Report
```bash
mvn surefire-report:report
open target/site/surefire-report.html
```

### Test Execution Summary
```bash
# After running tests:
mvn test | grep -E "Tests run:|Failures:|Errors:"
```

---

## 🔗 Integration Examples

### With Existing CI/CD
- **Jenkins**: See QUICK_REFERENCE.md
- **GitHub Actions**: See MICROPHONE_TESTING_GUIDE.md
- **GitLab CI**: Available on request
- **Azure Pipelines**: Available on request

### With Test Management Tools
- **Allure Reports**: Requires allure-maven plugin
- **TestNG**: Change JUnit5 to TestNG (pom.xml)
- **Cucumber**: Can integrate with Gherkin syntax

---

## 💡 Key Features

### Unified Testing
```
Single Framework
    ├── API Testing
    ├── UI Testing  
    ├── Voice Testing
    └── Integration Tests
```

### Microphone Support
```
Native Browser Permissions
    ├── Pre-grant microphone
    ├── Simulate voice input
    ├── Capture audio streams
    └── Test error handling
```

### Enterprise Ready
```
Production-Grade Features
    ├── Parallel Execution
    ├── Retry Logic
    ├── Detailed Reporting
    ├── CI/CD Integration
    ├── Multiple Environments
    └── Scalable Architecture
```

---

## 📞 Support & Resources

### Documentation
- 📖 **TEST_PLAN.md** - Comprehensive strategy
- 🎤 **MICROPHONE_TESTING_GUIDE.md** - Voice features
- ⚡ **QUICK_REFERENCE.md** - Command reference
- 📄 **README.md** - This file

### External Resources
- [Playwright Java Documentation](https://playwright.dev/java/)
- [JUnit 5 Documentation](https://junit.org/junit5/)
- [Maven Documentation](https://maven.apache.org/)

---

## 📝 Notes

- **Default API URL**: http://localhost:8007
- **Default Browser**: Chromium (auto-download via Playwright)
- **Java Version**: 17 (Enterprise standard)
- **Test Framework**: JUnit 5 (Latest)
- **Execution Model**: Parallel by default (configurable)

---

## ✨ Next Steps

1. ✅ **Read**: TEST_PLAN.md (understand the "why")
2. ✅ **Run**: `mvn test` (verify setup)
3. ✅ **Review**: Test results in `target/surefire-reports/`
4. ✅ **Explore**: MICROPHONE_TESTING_GUIDE.md (for voice features)
5. ✅ **Reference**: QUICK_REFERENCE.md (during development)

---

**Project Created**: June 2026  
**Framework**: Playwright Java  
**Test Coverage**: APIs 1-6, 8-10  
**Status**: ✅ Production Ready

