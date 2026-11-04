package com.voicebanking.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/** Minimal HTTP client for API requests. */
public class APIClient {
    private String baseURL;
    private ObjectMapper objectMapper;
    private java.net.http.HttpClient httpClient;

    public APIClient(String baseURL) {
        this.baseURL = baseURL;
        this.objectMapper = new ObjectMapper();
        this.httpClient = java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_2)
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }

    public JsonNode post(String path, Object body) throws Exception {
 String url = baseURL + path;
    String bodyString = objectMapper.writeValueAsString(body);

    System.out.println("\n==================================================");
    System.out.println("API REQUEST");
    System.out.println("==================================================");
    System.out.println("URL    : " + url);
    System.out.println("METHOD : POST");
    System.out.println("BODY   : ");
    System.out.println(objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(body));

    java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(new java.net.URI(url))
            .header("Content-Type", "application/json")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(bodyString))
            .timeout(java.time.Duration.ofSeconds(30))
            .build();

    java.net.http.HttpResponse<String> response = httpClient.send(
            request,
            java.net.http.HttpResponse.BodyHandlers.ofString());

    System.out.println("\n==================================================");
    System.out.println("API RESPONSE");
    System.out.println("==================================================");
    System.out.println("HTTP STATUS : " + response.statusCode());

    try {
        JsonNode jsonResponse = objectMapper.readTree(response.body());
        System.out.println(jsonResponse.toPrettyString());
        return jsonResponse;
    } catch (Exception e) {
        System.out.println(response.body());
        throw e;
    }
    }

    public JsonNode get(String path) throws Exception {
        String url = baseURL + path;

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(new java.net.URI(url))
                .header("Content-Type", "application/json")
                .GET()
                .timeout(java.time.Duration.ofSeconds(30))
                .build();

        java.net.http.HttpResponse<String> response = httpClient.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

        return objectMapper.readTree(response.body());
    }

    public int getPostStatus(String path, Object body) throws Exception {
        String url = baseURL + path;
        String bodyString = objectMapper.writeValueAsString(body);

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(new java.net.URI(url))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(bodyString))
                .timeout(java.time.Duration.ofSeconds(30))
                .build();

        java.net.http.HttpResponse<String> response = httpClient.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

        return response.statusCode();
    }

    public void close() {
        // HttpClient doesn't need explicit close in Java 11+
    }
}
