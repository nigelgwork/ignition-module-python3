package com.inductiveautomation.ignition.examples.python3.gateway;

import com.inductiveautomation.ignition.common.gson.Gson;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonParser;
import com.inductiveautomation.ignition.gateway.dataroutes.HttpMethod;
import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteAccess;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API endpoints for Python 3 Integration module.
 * Provides HTTP access to all Python3 functions via JSON API.
 *
 * Endpoints are mounted at: http://host:port/main/data/python3integration/
 */
public final class Python3RestEndpoints {

    private static final Logger LOGGER = LoggerFactory.getLogger(Python3RestEndpoints.class);
    private static Python3ScriptModule scriptModule;
    private static Python3ScriptRepository scriptRepository;

    private Python3RestEndpoints() {
        // Private constructor for utility class
    }

    /**
     * Initialize the REST endpoints with the script module instance.
     * Called from GatewayHook during startup.
     */
    public static void initialize(Python3ScriptModule module) {
        scriptModule = module;
        LOGGER.info("Python3RestEndpoints initialized");
    }

    /**
     * Set the script repository for script management endpoints.
     * Called from GatewayHook during startup.
     */
    public static void setScriptRepository(Python3ScriptRepository repository) {
        scriptRepository = repository;
        LOGGER.info("Script repository configured");
    }

    /**
     * Mount all REST API routes.
     * Called from GatewayHook.mountRouteHandlers().
     *
     * Routes follow Ignition 8.3 API convention: /api/v1/{endpoint}
     * This ensures they appear in the OpenAPI specification at /openapi.json
     */
    public static void mountRoutes(RouteGroup routes) {
        LOGGER.info("Mounting Python3 REST API routes (Ignition 8.3 OpenAPI compliant)");

        // POST /data/python3integration/api/v1/exec - Execute Python code
        routes.newRoute("/api/v1/exec")
            .handler(Python3RestEndpoints::handleExec)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(req -> RouteAccess.GRANTED)  // Public API - can be secured via API tokens at gateway level
            .mount();

        // POST /data/python3integration/api/v1/eval - Evaluate Python expression
        routes.newRoute("/api/v1/eval")
            .handler(Python3RestEndpoints::handleEval)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(req -> RouteAccess.GRANTED)
            .mount();

        // POST /data/python3integration/api/v1/call-module - Call Python module function
        routes.newRoute("/api/v1/call-module")
            .handler(Python3RestEndpoints::handleCallModule)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(req -> RouteAccess.GRANTED)
            .mount();

        // POST /data/python3integration/api/v1/call-script - Call saved Python script
        routes.newRoute("/api/v1/call-script")
            .handler(Python3RestEndpoints::handleCallScript)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(req -> RouteAccess.GRANTED)
            .mount();

        // GET /data/python3integration/api/v1/version - Get Python version
        routes.newRoute("/api/v1/version")
            .handler(Python3RestEndpoints::handleGetVersion)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(req -> RouteAccess.GRANTED)
            .mount();

        // GET /data/python3integration/api/v1/pool-stats - Get process pool statistics
        routes.newRoute("/api/v1/pool-stats")
            .handler(Python3RestEndpoints::handleGetPoolStats)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(req -> RouteAccess.GRANTED)
            .mount();

        // GET /data/python3integration/api/v1/health - Health check
        routes.newRoute("/api/v1/health")
            .handler(Python3RestEndpoints::handleHealthCheck)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(req -> RouteAccess.GRANTED)
            .mount();

        // GET /data/python3integration/api/v1/diagnostics - Performance diagnostics
        routes.newRoute("/api/v1/diagnostics")
            .handler(Python3RestEndpoints::handleDiagnostics)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(req -> RouteAccess.GRANTED)
            .mount();

        // GET /data/python3integration/api/v1/example - Run example test
        routes.newRoute("/api/v1/example")
            .handler(Python3RestEndpoints::handleExample)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(req -> RouteAccess.GRANTED)
            .mount();

        // Script Management Endpoints

        // POST /data/python3integration/api/v1/scripts/save - Save a script
        routes.newRoute("/api/v1/scripts/save")
            .handler(Python3RestEndpoints::handleSaveScript)
            .method(HttpMethod.POST)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(req -> RouteAccess.GRANTED)
            .mount();

        // GET /data/python3integration/api/v1/scripts/load/{name} - Load a script
        routes.newRoute("/api/v1/scripts/load/:name")
            .handler(Python3RestEndpoints::handleLoadScript)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(req -> RouteAccess.GRANTED)
            .mount();

        // GET /data/python3integration/api/v1/scripts/list - List all scripts
        routes.newRoute("/api/v1/scripts/list")
            .handler(Python3RestEndpoints::handleListScripts)
            .method(HttpMethod.GET)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(req -> RouteAccess.GRANTED)
            .mount();

        // DELETE /data/python3integration/api/v1/scripts/delete/{name} - Delete a script
        routes.newRoute("/api/v1/scripts/delete/:name")
            .handler(Python3RestEndpoints::handleDeleteScript)
            .method(HttpMethod.DELETE)
            .type(RouteGroup.TYPE_JSON)
            .accessControl(req -> RouteAccess.GRANTED)
            .mount();

        LOGGER.info("Python3 REST API routes mounted successfully at /api/v1/*");
    }

    /**
     * Handle POST /exec - Execute Python code
     *
     * Request body: {"code": "...", "variables": {...}}
     * Response: {"success": true/false, "result": ..., "error": "..."}
     */
    private static JsonObject handleExec(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /exec called");

        try {
            JsonObject requestBody = parseJsonBody(req);
            String code = requestBody.has("code") ? requestBody.get("code").getAsString() : "";
            Map<String, Object> variables = new HashMap<>();

            if (requestBody.has("variables") && requestBody.get("variables").isJsonObject()) {
                variables = jsonToMap(requestBody.getAsJsonObject("variables"));
            }

            Object result = scriptModule.exec(code, variables);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("result", result != null ? result.toString() : null);

            LOGGER.debug("REST API: /exec completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /exec failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle POST /eval - Evaluate Python expression
     *
     * Request body: {"expression": "...", "variables": {...}}
     * Response: {"success": true/false, "result": ..., "error": "..."}
     */
    private static JsonObject handleEval(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /eval called");

        try {
            JsonObject requestBody = parseJsonBody(req);
            String expression = requestBody.has("expression") ? requestBody.get("expression").getAsString() : "";
            Map<String, Object> variables = new HashMap<>();

            if (requestBody.has("variables") && requestBody.get("variables").isJsonObject()) {
                variables = jsonToMap(requestBody.getAsJsonObject("variables"));
            }

            Object result = scriptModule.eval(expression, variables);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("result", result != null ? result.toString() : null);

            LOGGER.debug("REST API: /eval completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /eval failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle POST /call-module - Call Python module function
     *
     * Request body: {"module": "...", "function": "...", "args": [...]}
     * Response: {"success": true/false, "result": ..., "error": "..."}
     */
    private static JsonObject handleCallModule(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /call-module called");

        try {
            JsonObject requestBody = parseJsonBody(req);
            String moduleName = requestBody.has("module") ? requestBody.get("module").getAsString() : "";
            String functionName = requestBody.has("function") ? requestBody.get("function").getAsString() : "";
            List<Object> args = new ArrayList<>();

            if (requestBody.has("args") && requestBody.get("args").isJsonArray()) {
                JsonArray jsonArgs = requestBody.getAsJsonArray("args");
                for (JsonElement element : jsonArgs) {
                    args.add(jsonElementToObject(element));
                }
            }

            Object result = scriptModule.callModule(moduleName, functionName, args);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("result", result != null ? result.toString() : null);

            LOGGER.debug("REST API: /call-module completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /call-module failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle POST /call-script - Call saved Python script by path
     *
     * Request body: {"scriptPath": "...", "args": [...], "kwargs": {...}}
     * Response: {"success": true/false, "result": ..., "error": "..."}
     */
    private static JsonObject handleCallScript(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /call-script called");

        try {
            JsonObject requestBody = parseJsonBody(req);
            String scriptPath = requestBody.has("scriptPath") ? requestBody.get("scriptPath").getAsString() : "";

            if (scriptPath.isEmpty()) {
                return createErrorResponse("scriptPath is required");
            }

            List<Object> args = new ArrayList<>();
            if (requestBody.has("args") && requestBody.get("args").isJsonArray()) {
                JsonArray jsonArgs = requestBody.getAsJsonArray("args");
                for (JsonElement element : jsonArgs) {
                    args.add(jsonElementToObject(element));
                }
            }

            Map<String, Object> kwargs = new HashMap<>();
            if (requestBody.has("kwargs") && requestBody.get("kwargs").isJsonObject()) {
                kwargs = jsonToMap(requestBody.getAsJsonObject("kwargs"));
            }

            Object result = scriptModule.callScript(scriptPath, args, kwargs);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("result", result != null ? result.toString() : null);

            LOGGER.debug("REST API: /call-script completed successfully for script: {}", scriptPath);
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /call-script failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /version - Get Python version
     *
     * Response: {"version": "...", "available": true/false}
     */
    private static JsonObject handleGetVersion(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /version called");

        try {
            Map<String, Object> versionInfo = scriptModule.getVersion();
            JsonObject response = mapToJson(versionInfo);

            LOGGER.debug("REST API: /version completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /version failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /pool-stats - Get process pool statistics
     *
     * Response: {"totalSize": ..., "available": ..., "inUse": ..., "healthy": ...}
     */
    private static JsonObject handleGetPoolStats(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /pool-stats called");

        try {
            Map<String, Object> poolStats = scriptModule.getPoolStats();
            JsonObject response = mapToJson(poolStats);

            LOGGER.debug("REST API: /pool-stats completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /pool-stats failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /health - Health check
     *
     * Response: {"healthy": true/false, "available": true/false}
     */
    private static JsonObject handleHealthCheck(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /health called");

        try {
            boolean available = scriptModule.isAvailable();

            JsonObject response = new JsonObject();
            response.addProperty("healthy", available);
            response.addProperty("available", available);
            response.addProperty("timestamp", System.currentTimeMillis());

            LOGGER.debug("REST API: /health completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /health failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /diagnostics - Performance diagnostics
     *
     * Response: comprehensive diagnostic information
     */
    private static JsonObject handleDiagnostics(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /diagnostics called");

        try {
            JsonObject response = new JsonObject();

            // Module availability
            boolean available = scriptModule.isAvailable();
            response.addProperty("available", available);

            // Process pool stats
            Map<String, Object> poolStats = scriptModule.getPoolStats();
            response.add("poolStats", mapToJson(poolStats));

            // Python version info
            Map<String, Object> versionInfo = scriptModule.getVersion();
            response.add("versionInfo", mapToJson(versionInfo));

            // Distribution info
            Map<String, Object> distributionInfo = scriptModule.getDistributionInfo();
            response.add("distributionInfo", mapToJson(distributionInfo));

            // Timestamp
            response.addProperty("timestamp", System.currentTimeMillis());

            LOGGER.debug("REST API: /diagnostics completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /diagnostics failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /example - Run example test
     *
     * Response: {"success": true/false, "result": "..."}
     */
    private static JsonObject handleExample(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /example called");

        try {
            String result = scriptModule.example();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("result", result);

            LOGGER.debug("REST API: /example completed successfully");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /example failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    // Script Management Handlers

    /**
     * Handle POST /scripts/save - Save a Python script
     *
     * Request body: {"name": "...", "code": "...", "description": "...", "author": "...", "folderPath": "...", "version": "..."}
     * Response: {"success": true/false, "script": {...}}
     */
    private static JsonObject handleSaveScript(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /scripts/save called");

        try {
            if (scriptRepository == null) {
                return createErrorResponse("Script repository not initialized");
            }

            JsonObject requestBody = parseJsonBody(req);
            String name = requestBody.has("name") ? requestBody.get("name").getAsString() : null;
            String code = requestBody.has("code") ? requestBody.get("code").getAsString() : "";
            String description = requestBody.has("description") ? requestBody.get("description").getAsString() : null;
            String author = requestBody.has("author") ? requestBody.get("author").getAsString() : "Unknown";
            String folderPath = requestBody.has("folderPath") ? requestBody.get("folderPath").getAsString() : "";
            String version = requestBody.has("version") ? requestBody.get("version").getAsString() : "1.0";

            if (name == null || name.trim().isEmpty()) {
                return createErrorResponse("Script name is required");
            }

            Python3ScriptRepository.SavedScript script = scriptRepository.saveScript(
                name, code, description, author, folderPath, version
            );

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            JsonObject scriptJson = new JsonObject();
            scriptJson.addProperty("id", script.getId());
            scriptJson.addProperty("name", script.getName());
            scriptJson.addProperty("description", script.getDescription());
            scriptJson.addProperty("author", script.getAuthor());
            scriptJson.addProperty("createdDate", script.getCreatedDate());
            scriptJson.addProperty("lastModified", script.getLastModified());
            scriptJson.addProperty("folderPath", script.getFolderPath());
            scriptJson.addProperty("version", script.getVersion());
            response.add("script", scriptJson);

            LOGGER.info("REST API: Script saved: {} in folder: {}", name, folderPath);
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /scripts/save failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /scripts/load/:name - Load a saved script
     *
     * Response: {"success": true/false, "script": {...}}
     */
    private static JsonObject handleLoadScript(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /scripts/load called");

        try {
            if (scriptRepository == null) {
                return createErrorResponse("Script repository not initialized");
            }

            // Extract name from URL path
            String requestPath = req.getRequest().getRequestURI();
            String name = requestPath.substring(requestPath.lastIndexOf('/') + 1);

            if (name == null || name.trim().isEmpty()) {
                return createErrorResponse("Script name is required");
            }

            Python3ScriptRepository.SavedScript script = scriptRepository.loadScript(name);

            if (script == null) {
                return createErrorResponse("Script not found: " + name);
            }

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            JsonObject scriptJson = new JsonObject();
            scriptJson.addProperty("id", script.getId());
            scriptJson.addProperty("name", script.getName());
            scriptJson.addProperty("code", script.getCode());
            scriptJson.addProperty("description", script.getDescription());
            scriptJson.addProperty("author", script.getAuthor());
            scriptJson.addProperty("createdDate", script.getCreatedDate());
            scriptJson.addProperty("lastModified", script.getLastModified());
            scriptJson.addProperty("folderPath", script.getFolderPath());
            scriptJson.addProperty("version", script.getVersion());
            response.add("script", scriptJson);

            LOGGER.debug("REST API: Script loaded: {}", name);
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /scripts/load failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle GET /scripts/list - List all saved scripts
     *
     * Response: {"success": true, "scripts": [...]}
     */
    private static JsonObject handleListScripts(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /scripts/list called");

        try {
            if (scriptRepository == null) {
                return createErrorResponse("Script repository not initialized");
            }

            List<Python3ScriptRepository.ScriptMetadata> scripts = scriptRepository.listScripts();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);

            JsonArray scriptsArray = new JsonArray();
            for (Python3ScriptRepository.ScriptMetadata script : scripts) {
                JsonObject scriptJson = new JsonObject();
                scriptJson.addProperty("id", script.getId());
                scriptJson.addProperty("name", script.getName());
                scriptJson.addProperty("description", script.getDescription());
                scriptJson.addProperty("author", script.getAuthor());
                scriptJson.addProperty("createdDate", script.getCreatedDate());
                scriptJson.addProperty("lastModified", script.getLastModified());
                scriptJson.addProperty("folderPath", script.getFolderPath());
                scriptJson.addProperty("version", script.getVersion());
                scriptsArray.add(scriptJson);
            }
            response.add("scripts", scriptsArray);

            LOGGER.debug("REST API: Listed {} scripts", scripts.size());
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /scripts/list failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Handle DELETE /scripts/delete/:name - Delete a saved script
     *
     * Response: {"success": true/false}
     */
    private static JsonObject handleDeleteScript(RequestContext req, HttpServletResponse res) {
        LOGGER.debug("REST API: /scripts/delete called");

        try {
            if (scriptRepository == null) {
                return createErrorResponse("Script repository not initialized");
            }

            // Extract name from URL path
            String requestPath = req.getRequest().getRequestURI();
            String name = requestPath.substring(requestPath.lastIndexOf('/') + 1);

            if (name == null || name.trim().isEmpty()) {
                return createErrorResponse("Script name is required");
            }

            boolean deleted = scriptRepository.deleteScript(name);

            JsonObject response = new JsonObject();
            response.addProperty("success", deleted);

            if (!deleted) {
                response.addProperty("message", "Script not found: " + name);
            } else {
                response.addProperty("message", "Script deleted successfully");
            }

            LOGGER.info("REST API: Script deletion: {} - {}", name, deleted ? "success" : "not found");
            return response;

        } catch (Exception e) {
            LOGGER.error("REST API: /scripts/delete failed", e);
            return createErrorResponse(e.getMessage());
        }
    }

    // Utility methods

    /**
     * Parse JSON from request body
     */
    private static JsonObject parseJsonBody(RequestContext req) throws IOException {
        HttpServletRequest httpRequest = req.getRequest();
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = httpRequest.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String jsonString = sb.toString();
        return JsonParser.parseString(jsonString).getAsJsonObject();
    }

    private static JsonObject createErrorResponse(String errorMessage) {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", errorMessage);
        return response;
    }

    private static JsonObject mapToJson(Map<String, Object> map) {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                json.addProperty(entry.getKey(), (String) value);
            } else if (value instanceof Number) {
                json.addProperty(entry.getKey(), (Number) value);
            } else if (value instanceof Boolean) {
                json.addProperty(entry.getKey(), (Boolean) value);
            } else if (value != null) {
                json.addProperty(entry.getKey(), value.toString());
            }
        }
        return json;
    }

    private static Map<String, Object> jsonToMap(JsonObject json) {
        Map<String, Object> map = new HashMap<>();
        for (String key : json.keySet()) {
            map.put(key, jsonElementToObject(json.get(key)));
        }
        return map;
    }

    private static Object jsonElementToObject(JsonElement element) {
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isNumber()) {
                return element.getAsNumber();
            } else if (element.getAsJsonPrimitive().isBoolean()) {
                return element.getAsBoolean();
            } else {
                return element.getAsString();
            }
        } else if (element.isJsonNull()) {
            return null;
        } else {
            return element.toString();
        }
    }
}
