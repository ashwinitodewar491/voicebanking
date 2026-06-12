package com.voicebanking.pages;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.utils.APIClient;
import org.testng.annotations.BeforeMethod;

public class BaseApiPage {
protected APIClient apiClient;

@BeforeMethod(alwaysRun = true)
public void setUp() {
    String baseURL = System.getenv("API_BASE_URL");

    if (baseURL == null || baseURL.isEmpty()) {
        baseURL = Endpoints.BASE_URL_PROD;
    }

    if (!baseURL.startsWith("http://") && !baseURL.startsWith("https://")) {
        baseURL = "http://" + baseURL;
    }

    apiClient = new APIClient(baseURL);
}
}
