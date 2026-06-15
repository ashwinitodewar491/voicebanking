package com.voicebanking.pages;
import com.voicebanking.DataText.Endpoints;
import com.voicebanking.utils.APIClient;
import org.testng.annotations.BeforeMethod;

public class BaseApiPage {
protected APIClient apiClient;

@BeforeMethod(alwaysRun = true)
public void setUp() {
    apiClient = new APIClient(Endpoints.getBaseUrl());
}
}
