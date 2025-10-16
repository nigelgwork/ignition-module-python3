package com.inductiveautomation.ignition.examples.python3.gateway;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages saved Python scripts for the Python 3 Integration module.
 * Scripts are stored as JSON files in the Gateway data directory.
 */
public class Python3ScriptRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(Python3ScriptRepository.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path scriptsDirectory;
    private final Path scriptsIndexFile;
    private final Map<String, SavedScript> scriptIndex;

    /**
     * Creates a new script repository.
     *
     * @param baseDirectory the base directory for script storage
     * @throws IOException if the directory cannot be created
     */
    public Python3ScriptRepository(Path baseDirectory) throws IOException {
        this.scriptsDirectory = baseDirectory.resolve("scripts");
        this.scriptsIndexFile = scriptsDirectory.resolve("index.json");
        this.scriptIndex = new HashMap<>();

        // Create directories if they don't exist
        Files.createDirectories(scriptsDirectory);

        // Load existing scripts
        loadIndex();

        LOGGER.info("Python3ScriptRepository initialized at: {}", scriptsDirectory);
    }

    /**
     * Saves a Python script.
     *
     * @param name the script name (unique identifier)
     * @param code the Python code
     * @param description optional description
     * @return the saved script
     * @throws IOException if save fails
     */
    public SavedScript saveScript(String name, String code, String description) throws IOException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Script name cannot be empty");
        }

        // Sanitize name for filesystem
        String sanitizedName = sanitizeName(name);

        // Create script object
        SavedScript script = new SavedScript(
                sanitizedName,
                name,
                code,
                description,
                Instant.now().toString()
        );

        // Save to index
        scriptIndex.put(sanitizedName, script);

        // Persist index
        saveIndex();

        LOGGER.info("Script saved: {}", name);
        return script;
    }

    /**
     * Loads a saved script by name.
     *
     * @param name the script name
     * @return the saved script, or null if not found
     */
    public SavedScript loadScript(String name) {
        String sanitizedName = sanitizeName(name);
        SavedScript script = scriptIndex.get(sanitizedName);

        if (script == null) {
            LOGGER.warn("Script not found: {}", name);
        }

        return script;
    }

    /**
     * Lists all saved scripts.
     *
     * @return list of saved scripts (metadata only, no code)
     */
    public List<ScriptMetadata> listScripts() {
        return scriptIndex.values().stream()
                .map(script -> new ScriptMetadata(
                        script.getId(),
                        script.getName(),
                        script.getDescription(),
                        script.getLastModified()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Deletes a saved script.
     *
     * @param name the script name
     * @return true if deleted, false if not found
     * @throws IOException if delete fails
     */
    public boolean deleteScript(String name) throws IOException {
        String sanitizedName = sanitizeName(name);

        if (scriptIndex.remove(sanitizedName) != null) {
            saveIndex();
            LOGGER.info("Script deleted: {}", name);
            return true;
        }

        LOGGER.warn("Script not found for deletion: {}", name);
        return false;
    }

    /**
     * Checks if a script exists.
     *
     * @param name the script name
     * @return true if exists
     */
    public boolean exists(String name) {
        return scriptIndex.containsKey(sanitizeName(name));
    }

    /**
     * Gets the number of saved scripts.
     *
     * @return count of scripts
     */
    public int getScriptCount() {
        return scriptIndex.size();
    }

    /**
     * Loads the script index from disk.
     */
    private void loadIndex() throws IOException {
        if (!Files.exists(scriptsIndexFile)) {
            LOGGER.info("No existing script index found, starting fresh");
            return;
        }

        try {
            String json = Files.readString(scriptsIndexFile);
            Map<String, SavedScript> loaded = GSON.fromJson(
                    json,
                    new TypeToken<Map<String, SavedScript>>() {}.getType()
            );

            if (loaded != null) {
                scriptIndex.putAll(loaded);
                LOGGER.info("Loaded {} saved scripts", scriptIndex.size());
            }

        } catch (Exception e) {
            LOGGER.error("Failed to load script index, starting fresh", e);
        }
    }

    /**
     * Saves the script index to disk.
     */
    private void saveIndex() throws IOException {
        String json = GSON.toJson(scriptIndex);
        Files.writeString(
                scriptsIndexFile,
                json,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    /**
     * Sanitizes a script name for use as a filesystem identifier.
     *
     * @param name the original name
     * @return sanitized name
     */
    private String sanitizeName(String name) {
        if (name == null) {
            return "unnamed";
        }

        // Replace spaces with underscores, remove special characters
        return name.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9_-]", "_")
                .replaceAll("_+", "_");
    }

    /**
     * Represents a saved Python script.
     */
    public static class SavedScript {
        private final String id;
        private final String name;
        private final String code;
        private final String description;
        private final String lastModified;

        public SavedScript(String id, String name, String code, String description, String lastModified) {
            this.id = id;
            this.name = name;
            this.code = code;
            this.description = description;
            this.lastModified = lastModified;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public String getLastModified() {
            return lastModified;
        }
    }

    /**
     * Script metadata (without code for listing).
     */
    public static class ScriptMetadata {
        private final String id;
        private final String name;
        private final String description;
        private final String lastModified;

        public ScriptMetadata(String id, String name, String description, String lastModified) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.lastModified = lastModified;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getLastModified() {
            return lastModified;
        }
    }
}
