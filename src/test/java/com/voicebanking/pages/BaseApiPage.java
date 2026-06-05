package com.voicebanking.pages;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.utils.APIClient;
import org.testng.annotations.BeforeMethod;

public class BaseApiPage {
protected APIClient apiClient;

@BeforeMethod
public void setUp() {
    String baseURL = System.getenv("API_BASE_URL");

    if (baseURL == null || baseURL.isEmpty()) {
        baseURL = Endpoints.BASE_URL_PROD;
    }

    apiClient = new APIClient(baseURL);
}
}
