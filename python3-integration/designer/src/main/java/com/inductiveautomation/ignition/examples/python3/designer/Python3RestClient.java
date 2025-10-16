package com.inductiveautomation.ignition.examples.python3.designer;

import com.inductiveautomation.ignition.common.gson.JsonArray;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
     * @param gatewayUrl the Gateway URL (e.g., "http://localhost:9088")
     */
    public Python3RestClient(String gatewayUrl) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Remove trailing slash if present
        if (gatewayUrl != null && gatewayUrl.endsWith("/")) {
            gatewayUrl = gatewayUrl.substring(0, gatewayUrl.length() - 1);
        }

        this.gatewayUrl = gatewayUrl != null ? gatewayUrl : "http://localhost:8088";

        LOGGER.info("Python3RestClient initialized with Gateway URL: {}", this.gatewayUrl);
    }

    /**
     * Creates a new REST client using the Designer context (auto-detect Gateway URL).
     *
     * @param context the Designer context
     */
    public Python3RestClient(DesignerContext context) {
        this(buildGatewayUrl(context));
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
     * @param context the Designer context (currently unused, reserved for future auto-detection)
     * @return the Gateway base URL (e.g., "http://localhost:8088")
     */
    private static String buildGatewayUrl(DesignerContext context) {
        try {
            // Try system property first (allows manual override)
            // Set via: -Dignition.python3.gateway.url=http://localhost:9088
            String url = System.getProperty("ignition.python3.gateway.url");

            // Try environment variable
            if (url == null || url.trim().isEmpty()) {
                url = System.getenv("IGNITION_GATEWAY_URL");
            }

            // Default to localhost:8088
            if (url == null || url.trim().isEmpty()) {
                url = "http://localhost:8088";
                LOGGER.info("Using default Gateway URL: {} (configure via IDE settings or set -Dignition.python3.gateway.url=http://host:port)", url);
            } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
                // If URL doesn't have protocol, add http://
                url = "http://" + url;
                LOGGER.info("Added http:// protocol to Gateway URL: {}", url);
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

    // Script Management Methods

    /**
     * Lists all saved scripts from the Gateway.
     *
     * @return list of script metadata
     * @throws IOException if the HTTP request fails
     */
    public List<ScriptMetadata> listScripts() throws IOException {
        LOGGER.debug("Listing saved scripts via REST API");

        String response = get("/scripts/list");
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        List<ScriptMetadata> scripts = new ArrayList<>();

        if (json.has("scripts") && json.get("scripts").isJsonArray()) {
            JsonArray scriptsArray = json.getAsJsonArray("scripts");
            for (int i = 0; i < scriptsArray.size(); i++) {
                JsonObject scriptJson = scriptsArray.get(i).getAsJsonObject();
                ScriptMetadata metadata = new ScriptMetadata();
                metadata.setId(scriptJson.has("id") ? scriptJson.get("id").getAsString() : null);
                metadata.setName(scriptJson.has("name") ? scriptJson.get("name").getAsString() : null);
                metadata.setDescription(scriptJson.has("description") ? scriptJson.get("description").getAsString() : null);
                metadata.setAuthor(scriptJson.has("author") ? scriptJson.get("author").getAsString() : null);
                metadata.setCreatedDate(scriptJson.has("createdDate") ? scriptJson.get("createdDate").getAsString() : null);
                metadata.setLastModified(scriptJson.has("lastModified") ? scriptJson.get("lastModified").getAsString() : null);
                metadata.setFolderPath(scriptJson.has("folderPath") ? scriptJson.get("folderPath").getAsString() : null);
                metadata.setVersion(scriptJson.has("version") ? scriptJson.get("version").getAsString() : null);
                scripts.add(metadata);
            }
        }

        LOGGER.debug("Loaded {} scripts", scripts.size());
        return scripts;
    }

    /**
     * Loads a saved script from the Gateway.
     *
     * @param name the script name
     * @return the saved script with code
     * @throws IOException if the HTTP request fails
     */
    public SavedScript loadScript(String name) throws IOException {
        LOGGER.debug("Loading script: {}", name);

        String response = get("/scripts/load/" + name);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        if (json.has("script") && json.get("script").isJsonObject()) {
            JsonObject scriptJson = json.getAsJsonObject("script");
            SavedScript script = new SavedScript();
            script.setId(scriptJson.has("id") ? scriptJson.get("id").getAsString() : null);
            script.setName(scriptJson.has("name") ? scriptJson.get("name").getAsString() : null);
            script.setCode(scriptJson.has("code") ? scriptJson.get("code").getAsString() : null);
            script.setDescription(scriptJson.has("description") ? scriptJson.get("description").getAsString() : null);
            script.setAuthor(scriptJson.has("author") ? scriptJson.get("author").getAsString() : null);
            script.setCreatedDate(scriptJson.has("createdDate") ? scriptJson.get("createdDate").getAsString() : null);
            script.setLastModified(scriptJson.has("lastModified") ? scriptJson.get("lastModified").getAsString() : null);
            script.setFolderPath(scriptJson.has("folderPath") ? scriptJson.get("folderPath").getAsString() : null);
            script.setVersion(scriptJson.has("version") ? scriptJson.get("version").getAsString() : null);
            return script;
        }

        throw new IOException("Failed to load script: " + name);
    }

    /**
     * Saves a script to the Gateway.
     *
     * @param name the script name
     * @param code the Python code
     * @param description optional description
     * @param author the script author
     * @param folderPath the folder path (e.g., "My Scripts/Utils")
     * @param version the script version (e.g., "1.0")
     * @throws IOException if the HTTP request fails
     */
    public void saveScript(String name, String code, String description,
                          String author, String folderPath, String version) throws IOException {
        LOGGER.debug("Saving script: {} in folder: {}", name, folderPath);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("name", name);
        requestBody.addProperty("code", code);
        requestBody.addProperty("description", description);
        requestBody.addProperty("author", author);
        requestBody.addProperty("folderPath", folderPath);
        requestBody.addProperty("version", version);

        String response = post("/scripts/save", requestBody.toString());
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        if (!json.has("success") || !json.get("success").getAsBoolean()) {
            throw new IOException("Failed to save script: " + name);
        }

        LOGGER.info("Script saved successfully: {} in folder: {}", name, folderPath);
    }

    /**
     * Saves a script to the Gateway (simplified overload for backward compatibility).
     *
     * @param name the script name
     * @param code the Python code
     * @param description optional description
     * @throws IOException if the HTTP request fails
     */
    public void saveScript(String name, String code, String description) throws IOException {
        saveScript(name, code, description, "Unknown", "", "1.0");
    }

    /**
     * Deletes a saved script from the Gateway.
     *
     * @param name the script name
     * @throws IOException if the HTTP request fails
     */
    public void deleteScript(String name) throws IOException {
        LOGGER.debug("Deleting script: {}", name);

        String url = gatewayUrl + API_BASE_PATH + "/scripts/delete/" + name;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .DELETE()
                .header("Accept", "application/json")
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!json.has("success") || !json.get("success").getAsBoolean()) {
                throw new IOException("Failed to delete script: " + name);
            }

            LOGGER.info("Script deleted successfully: {}", name);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }
}
