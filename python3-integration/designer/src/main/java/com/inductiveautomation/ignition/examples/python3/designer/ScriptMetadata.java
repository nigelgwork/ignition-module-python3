package com.inductiveautomation.ignition.examples.python3.designer;

/**
 * Metadata for a saved Python script (without the full code).
 */
public class ScriptMetadata {
    private String id;
    private String name;
    private String description;
    private String lastModified;

    public ScriptMetadata() {
    }

    public ScriptMetadata(String id, String name, String description, String lastModified) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.lastModified = lastModified;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }
}
