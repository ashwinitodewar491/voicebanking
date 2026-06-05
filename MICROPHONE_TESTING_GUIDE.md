# Playwright Java Microphone Testing Guide

## Quick Start

### 1. Browser Setup with Microphone Permission

```java
import com.microsoft.playwright.*;

public class BrowserContextManager {
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    public void setupBrowserWithMicrophone() {
        // Initialize Playwright
        playwright = Playwright.create();
        
        // Launch browser (use chromium for best microphone support)
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
            .setHeadless(false)  // Set to false for testing microphone permissions
        );
        
        // Create context with microphone permission pre-granted
        context = browser.newContext(new Browser.NewContextOptions()
            .setPermissions(java.util.Arrays.asList("microphone"))
            .setGeolocation(40.7128, -74.0060)
            .setTimezoneId("America/New_York")
        );
        
        page = context.newPage();
    }

    public Page getPage() {
        return page;
    }

    public void close() {
        context.close();
        browser.close();
        playwright.close();
    }
}
```

### 2. Check if Microphone is Available

```java
public boolean isMicrophoneAvailable() {
    Object result = page.evaluate(
        "() => navigator.mediaDevices !== undefined && " +
        "navigator.mediaDevices.getUserMedia !== undefined"
    );
    return (Boolean) result;
}
```

### 3. Request Microphone Access

```java
public boolean requestMicrophoneAccess() throws Exception {
    String script = "async () => {" +
        "try {" +
        "  const stream = await navigator.mediaDevices.getUserMedia({audio: true});" +
        "  // Stop the stream after getting it" +
        "  stream.getTracks().forEach(track => track.stop());" +
        "  return {success: true, message: 'Microphone access granted'};" +
        "} catch(error) {" +
        "  return {success: false, error: error.name};" +
        "}" +
        "}";
    
    Object result = page.evaluate(script);
    System.out.println("Microphone Request Result: " + result);
    
    if (result instanceof Map) {
        Map<String, Object> response = (Map<String, Object>) result;
        return (Boolean) response.get("success");
    }
    return false;
}
```

### 4. Simulate Voice Input (Speech Recognition API)

```java
public void simulateVoiceInput(String text) {
    String script = "() => {" +
        "const utterance = new SpeechSynthesisUtterance('" + text + "');" +
        "utterance.rate = 1.0;" +
        "utterance.pitch = 1.0;" +
        "utterance.volume = 1.0;" +
        "window.speechSynthesis.speak(utterance);" +
        "}";
    page.evaluate(script);
}
```

### 5. Capture Audio Levels

```java
public double getAudioLevel() {
    String script = "async () => {" +
        "const stream = await navigator.mediaDevices.getUserMedia({audio: true});" +
        "const audioContext = new AudioContext();" +
        "const source = audioContext.createMediaStreamSource(stream);" +
        "const analyser = audioContext.createAnalyser();" +
        "source.connect(analyser);" +
        "" +
        "const dataArray = new Uint8Array(analyser.frequencyBinCount);" +
        "analyser.getByteFrequencyData(dataArray);" +
        "" +
        "const average = dataArray.reduce((a, b) => a + b) / dataArray.length;" +
        "stream.getTracks().forEach(track => track.stop());" +
        "return average;" +
        "}";
    
    Object result = page.evaluate(script);
    if (result instanceof Number) {
        return ((Number) result).doubleValue();
    }
    return 0.0;
}
```

---

## Voice Banking UI Test Examples

### Example 1: Test Voice Balance Inquiry

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Voice Banking UI Tests")
public class VoiceBankingUITest {
    private BrowserContextManager browserManager;
    private Page page;

    @BeforeEach
    public void setUp() {
        browserManager = new BrowserContextManager();
        browserManager.setupBrowserWithMicrophone();
        page = browserManager.getPage();
    }

    @Test
    @DisplayName("Should handle voice balance inquiry")
    public void testVoiceBalanceInquiry() throws Exception {
        // Navigate to application
        page.navigate("http://localhost:8007/voice-banking");
        
        // Wait for page to load
        page.waitForSelector("[data-testid='voice-button']", 
            new Page.WaitForSelectorOptions().setTimeout(5000));
        
        // Verify microphone is available
        assertTrue(browserManager.isMicrophoneAvailable(), 
            "Microphone should be available");
        
        // Request microphone access
        assertTrue(browserManager.requestMicrophoneAccess(), 
            "Should grant microphone access");
        
        // Click voice button
        page.click("[data-testid='voice-button']");
        
        // Wait for listening indicator
        page.waitForSelector(".listening-indicator", 
            new Page.WaitForSelectorOptions().setTimeout(3000));
        
        // Simulate voice input
        browserManager.simulateVoiceInput("What is my account balance");
        
        // Wait for response
        page.waitForSelector(".balance-display", 
            new Page.WaitForSelectorOptions().setTimeout(10000));
        
        // Verify balance is displayed
        String balance = page.locator(".balance-display").textContent();
        assertFalse(balance.isEmpty(), "Balance should be displayed");
        assertTrue(balance.matches("[0-9,]+\\.[0-9]{2}"), 
            "Balance should be numeric format");
    }

    @Test
    @DisplayName("Should handle voice transfer command")
    public void testVoiceTransferCommand() throws Exception {
        page.navigate("http://localhost:8007/voice-banking");
        page.waitForSelector("[data-testid='voice-button']");
        
        // Request microphone
        browserManager.requestMicrophoneAccess();
        
        // Initiate voice command
        page.click("[data-testid='voice-button']");
        page.waitForSelector(".listening-indicator");
        
        // Simulate transfer command
        browserManager.simulateVoiceInput("Transfer 1000 to John");
        
        // Wait for transfer dialog
        page.waitForSelector(".transfer-confirmation", 
            new Page.WaitForSelectorOptions().setTimeout(10000));
        
        // Verify transfer details are shown
        String transferInfo = page.locator(".transfer-amount").textContent();
        assertTrue(transferInfo.contains("1000"), "Amount should be shown");
        
        // Confirm transfer via voice
        page.click("[data-testid='confirm-button']");
        
        // Wait for success message
        page.waitForSelector(".success-message");
        assertTrue(page.isVisible(".success-message"), 
            "Success message should be visible");
    }

    @Test
    @DisplayName("Should show error when microphone is denied")
    public void testMicrophoneDeniedError() throws Exception {
        page.navigate("http://localhost:8007/voice-banking");
        
        // Handle permission denial
        page.onDialog(dialog -> {
            if (dialog.type().equals("alert")) {
                dialog.dismiss();  // Deny microphone
            }
        });
        
        // Try to use voice feature
        page.click("[data-testid='voice-button']");
        
        // Wait for error message
        page.waitForSelector(".error-message", 
            new Page.WaitForSelectorOptions().setTimeout(5000));
        
        // Verify error is shown
        String errorMsg = page.locator(".error-message").textContent();
        assertTrue(errorMsg.contains("microphone") || 
                   errorMsg.contains("permission"), 
            "Should show microphone permission error");
    }
}
```

### Example 2: Test Microphone Permissions Management

```java
@DisplayName("Microphone Permissions Tests")
public class MicrophonePermissionsTest {
    private BrowserContextManager browserManager;
    private Page page;

    @BeforeEach
    public void setUp() {
        browserManager = new BrowserContextManager();
        browserManager.setupBrowserWithMicrophone();
        page = browserManager.getPage();
    }

    @Test
    @DisplayName("Should grant microphone permission to origin")
    public void testGrantMicrophonePermission() {
        // Pre-grant permission to specific origin
        browserManager.grantMicrophonePermission("http://localhost:8007");
        
        // Navigate to app
        page.navigate("http://localhost:8007/voice-banking");
        
        // Verify permission is granted
        String result = (String) page.evaluate(
            "() => {" +
            "  return navigator.permissions.query({name: 'microphone'})" +
            "    .then(permission => permission.state);" +
            "}"
        );
        
        assertEquals("granted", result, "Microphone should be granted");
    }

    @Test
    @DisplayName("Should revoke microphone permission")
    public void testRevokeMicrophonePermission() {
        // First grant permission
        browserManager.grantMicrophonePermission("http://localhost:8007");
        
        page.navigate("http://localhost:8007/voice-banking");
        
        // Revoke permission
        browserManager.revokeMicrophonePermission("http://localhost:8007");
        
        // Try to access microphone
        String result = (String) page.evaluate(
            "async () => {" +
            "  try {" +
            "    await navigator.mediaDevices.getUserMedia({audio: true});" +
            "    return 'success';" +
            "  } catch(e) {" +
            "    return e.name;" +
            "  }" +
            "}"
        );
        
        assertEquals("NotAllowedError", result, 
            "Should not allow microphone access after revoke");
    }

    @Test
    @DisplayName("Should handle multiple microphone access requests")
    public void testMultipleMicrophoneAccess() throws Exception {
        page.navigate("http://localhost:8007/voice-banking");
        browserManager.requestMicrophoneAccess();
        
        // First microphone access
        double level1 = browserManager.getAudioLevel();
        assertTrue(level1 >= 0, "Audio level should be non-negative");
        
        // Second microphone access
        double level2 = browserManager.getAudioLevel();
        assertTrue(level2 >= 0, "Audio level should be non-negative");
        
        System.out.println("Audio Level 1: " + level1);
        System.out.println("Audio Level 2: " + level2);
    }
}
```

---

## Advanced Microphone Testing

### Mock Microphone Data

```java
public class MockMicrophoneData {
    
    public static void injectMockAudioStream(Page page, double[] audioData) {
        String audioArray = java.util.Arrays.toString(audioData);
        String script = "() => {" +
            "const audioData = new Float32Array(" + audioArray + ");" +
            "window.__mockAudioData = audioData;" +
            "}";
        page.evaluate(script);
    }

    public static Object getUserMediaWithMock(Page page) {
        String script = "async () => {" +
            "const originalGetUserMedia = navigator.mediaDevices.getUserMedia;" +
            "" +
            "navigator.mediaDevices.getUserMedia = async (constraints) => {" +
            "  const mockStream = {" +
            "    active: true," +
            "    getTracks: () => [{stop: () => {}}]," +
            "    getAudioTracks: () => [{" +
            "      enabled: true," +
            "      kind: 'audio'," +
            "      id: 'mock-audio-track'" +
            "    }]" +
            "  };" +
            "  return mockStream;" +
            "};" +
            "" +
            "return 'Mock enabled';" +
            "}";
        return page.evaluate(script);
    }
}
```

### Verify Audio Output

```java
public class AudioOutputVerification {
    
    public boolean verifyAudioOutput(Page page, String expectedContent) {
        String script = "async () => {" +
            "try {" +
            "  const utterances = speechSynthesis.getVoices();" +
            "  return utterances.length > 0;" +
            "} catch(e) {" +
            "  return false;" +
            "}" +
            "}";
        
        Object result = page.evaluate(script);
        return result instanceof Boolean && (Boolean) result;
    }

    public void recordAudioOutput(Page page, String filename) {
        String script = "async () => {" +
            "const mediaRecorder = new MediaRecorder(window.__audioStream);" +
            "const chunks = [];" +
            "" +
            "mediaRecorder.ondataavailable = (e) => chunks.push(e.data);" +
            "mediaRecorder.onstop = () => {" +
            "  const blob = new Blob(chunks, {'type': 'audio/wav'});" +
            "  window.__recordedBlob = blob;" +
            "};" +
            "" +
            "mediaRecorder.start();" +
            "return 'Recording started';" +
            "}";
        
        page.evaluate(script);
    }
}
```

---

## Running Tests with Microphone

### Maven Command

```bash
# Run all microphone-related tests
mvn test -Dtest=*MicrophoneTest

# Run specific microphone test
mvn test -Dtest=MicrophonePermissionsTest

# Run with video recording
mvn test -Dtest=VoiceBankingUITest -Drecord=true

# Run with specific browser
mvn test -Dbrowser=chromium
```

### Test Configuration (application.properties)

```properties
# api.properties
api.base.url=http://localhost:8007
api.timeout.ms=10000
api.retry.count=3

# browser.properties
browser.type=chromium
browser.headless=false
browser.width=1280
browser.height=720
browser.record.video=true
browser.record.screenshots=true

# microphone.properties
microphone.enabled=true
microphone.permission.granted=true
microphone.audio.file=src/test/resources/audio/test-voice.wav
microphone.mock.data=true
```

---

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Playwright Voice Banking Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up Java
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Install Playwright browsers
      run: |
        mvn exec:java -Dexec.mainClass="com.microsoft.playwright.CLI" \
                      -Dexec.args="install"
    
    - name: Run tests
      run: mvn test
      env:
        BROWSER: chromium
        HEADLESS: true
    
    - name: Upload test results
      if: always()
      uses: actions/upload-artifact@v3
      with:
        name: test-results
        path: target/surefire-reports
    
    - name: Upload videos
      if: always()
      uses: actions/upload-artifact@v3
      with:
        name: test-videos
        path: test-results/videos
```

---

## Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| Microphone not available | Ensure headless=false in local testing |
| Permission denied | Pre-grant in BrowserContext options |
| Audio stream not working | Check browser version, use Chromium |
| Voice recognition fails | Verify audio input device is available |
| Flaky audio tests | Add longer timeouts for audio processing |

