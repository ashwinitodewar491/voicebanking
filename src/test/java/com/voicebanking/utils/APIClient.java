package com.voicebanking.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Utility class for making API requests using standard Java HTTP
 * This is a simplified version that uses standard Java libraries
 * compatible with Playwright's execution environment
 */
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

    /**
     * Make a POST request
     * 
     * @param path API endpoint path
     * @param body Request body as Object
     * @return Response body as JsonNode
     */
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

    /**
     * Make a GET request
     * 
     * @param path API endpoint path
     * @return Response body as JsonNode
     */
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

    /**
     * Get response status
     * 
     * @param path API endpoint path
     * @param body Request body
     * @return HTTP status code
     */
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

    /**
     * Close the HTTP client
     */
    public void close() {
        // HttpClient doesn't need explicit close in Java 11+
    }
}
