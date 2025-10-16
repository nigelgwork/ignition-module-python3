package com.inductiveautomation.ignition.examples.python3.designer;

import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonParser;
import com.inductiveautomation.ignition.designer.model.DesignerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API client for communicating with the Gateway's Python 3 Integration module.
 *
 * <p>This client provides methods to execute Python code, get pool statistics, and
 * retrieve diagnostics by making HTTP requests to the Gateway's REST API endpoints.</p>
 *
 * <p>All API endpoints follow the pattern: /data/python3integration/api/v1/{endpoint}</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * Python3RestClient client = new Python3RestClient(designerContext);
 * ExecutionResult result = client.executeCode("result = 2 + 2", new HashMap&lt;&gt;());
 * </pre>
 */
public class Python3RestClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(Python3RestClient.class);

    private static final String API_BASE_PATH = "/data/python3integration/api/v1";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final String gatewayUrl;

    /**
     * Creates a new REST client for the Python 3 Integration module.
     *
     * @param context the Designer context to get the Gateway URL from
     */
    public Python3RestClient(DesignerContext context) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Get Gateway URL from Designer context
        // Format: http://hostname:port or https://hostname:port
        this.gatewayUrl = buildGatewayUrl(context);

        LOGGER.info("Python3RestClient initialized with Gateway URL: {}", gatewayUrl);
    }

    /**
     * Executes Python code on the Gateway.
     *
     * @param code the Python code to execute
     * @param variables variables to pass to the Python environment
     * @return execution result with output or error
     * @throws IOException if the HTTP request fails
     */
    public ExecutionResult executeCode(String code, Map<String, Object> variables) throws IOException {
        LOGGER.debug("Executing Python code via REST API");

        // Build JSON request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("code", code);

        // Add variables as JSON object
        JsonObject varsJson = new JsonObject();
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                addToJson(varsJson, entry.getKey(), entry.getValue());
            }
        }
        requestBody.add("variables", varsJson);

        // Make POST request to /exec endpoint
        String response = post("/exec", requestBody.toString());

        // Parse response
        return parseExecutionResult(response);
    }

    /**
     * Evaluates a Python expression on the Gateway.
     *
     * @param expression the Python expression to evaluate
     * @param variables variables to pass to the Python environment
     * @return execution result with the expression value or error
     * @throws IOException if the HTTP request fails
     */
    public ExecutionResult evaluateExpression(String expression, Map<String, Object> variables) throws IOException {
        LOGGER.debug("Evaluating Python expression via REST API: {}", expression);

        // Build JSON request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("expression", expression);

        // Add variables as JSON object
        JsonObject varsJson = new JsonObject();
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                addToJson(varsJson, entry.getKey(), entry.getValue());
            }
        }
        requestBody.add("variables", varsJson);

        // Make POST request to /eval endpoint
        String response = post("/eval", requestBody.toString());

        // Parse response
        return parseExecutionResult(response);
    }

    /**
     * Gets the current Python process pool statistics.
     *
     * @return pool statistics
     * @throws IOException if the HTTP request fails
     */
    public PoolStats getPoolStats() throws IOException {
        LOGGER.debug("Getting pool stats via REST API");

        String response = get("/pool-stats");

        // Parse JSON response
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        int totalSize = json.has("totalSize") ? json.get("totalSize").getAsInt() : 0;
        int healthy = json.has("healthy") ? json.get("healthy").getAsInt() : 0;
        int available = json.has("available") ? json.get("available").getAsInt() : 0;
        int inUse = json.has("inUse") ? json.get("inUse").getAsInt() : 0;

        return new PoolStats(totalSize, healthy, available, inUse);
    }

    /**
     * Checks if the Python 3 module is available and healthy.
     *
     * @return true if the module is healthy and available
     * @throws IOException if the HTTP request fails
     */
    public boolean isHealthy() throws IOException {
        try {
            String response = get("/health");
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            return json.has("healthy") && json.get("healthy").getAsBoolean();
        } catch (Exception e) {
            LOGGER.warn("Health check failed", e);
            return false;
        }
    }

    /**
     * Gets comprehensive diagnostic information from the Gateway.
     *
     * @return JSON string with diagnostics
     * @throws IOException if the HTTP request fails
     */
    public String getDiagnostics() throws IOException {
        return get("/diagnostics");
    }

    /**
     * Makes a GET request to the specified endpoint.
     *
     * @param endpoint the API endpoint (e.g., "/health", "/pool-stats")
     * @return the response body as a string
     * @throws IOException if the request fails
     */
    private String get(String endpoint) throws IOException {
        String url = gatewayUrl + API_BASE_PATH + endpoint;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .header("Accept", "application/json")
                .build();

        try {
            LOGGER.debug("GET request to: {}", url);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
            }

            return response.body();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    /**
     * Makes a POST request to the specified endpoint.
     *
     * @param endpoint the API endpoint (e.g., "/exec", "/eval")
     * @param jsonBody the JSON request body
     * @return the response body as a string
     * @throws IOException if the request fails
     */
    private String post(String endpoint, String jsonBody) throws IOException {
        String url = gatewayUrl + API_BASE_PATH + endpoint;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build();

        try {
            LOGGER.debug("POST request to: {}", url);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
            }

            return response.body();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    /**
     * Parses an execution result from JSON response.
     *
     * @param jsonResponse the JSON response string
     * @return parsed ExecutionResult
     */
    private ExecutionResult parseExecutionResult(String jsonResponse) {
        try {
            JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();

            boolean success = json.has("success") && json.get("success").getAsBoolean();
            String result = json.has("result") ? json.get("result").getAsString() : null;
            String error = json.has("error") ? json.get("error").getAsString() : null;
            Long executionTimeMs = json.has("executionTimeMs") ? json.get("executionTimeMs").getAsLong() : null;
            Long timestamp = json.has("timestamp") ? json.get("timestamp").getAsLong() : null;

            return new ExecutionResult(success, result, error, executionTimeMs, timestamp);

        } catch (Exception e) {
            LOGGER.error("Failed to parse execution result", e);
            return new ExecutionResult(false, "Failed to parse response: " + e.getMessage());
        }
    }

    /**
     * Builds the Gateway URL from system properties, environment variables, or defaults.
     *
     * @param context the Designer context (not currently used)
     * @return the Gateway base URL (e.g., "http://localhost:8088")
     */
    private String buildGatewayUrl(DesignerContext context) {
        try {
            // Try system property first
            String url = System.getProperty("ignition.python3.gateway.url");

            // Try environment variable
            if (url == null || url.trim().isEmpty()) {
                url = System.getenv("IGNITION_GATEWAY_URL");
            }

            // Default to localhost
            if (url == null || url.trim().isEmpty()) {
                url = "http://localhost:8088";
                LOGGER.info("Using default Gateway URL: {}", url);
            } else {
                LOGGER.info("Using configured Gateway URL: {}", url);
            }

            // Remove trailing slash if present
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }

            return url;

        } catch (Exception e) {
            LOGGER.error("Failed to get Gateway URL, using default", e);
            return "http://localhost:8088";  // Fallback default
        }
    }

    /**
     * Adds a value to a JSON object with appropriate type handling.
     *
     * @param json the JSON object to add to
     * @param key the key
     * @param value the value (String, Number, Boolean, or other)
     */
    private void addToJson(JsonObject json, String key, Object value) {
        if (value == null) {
            json.add(key, null);
        } else if (value instanceof String) {
            json.addProperty(key, (String) value);
        } else if (value instanceof Number) {
            json.addProperty(key, (Number) value);
        } else if (value instanceof Boolean) {
            json.addProperty(key, (Boolean) value);
        } else {
            // For other types, convert to string
            json.addProperty(key, value.toString());
        }
    }
}
