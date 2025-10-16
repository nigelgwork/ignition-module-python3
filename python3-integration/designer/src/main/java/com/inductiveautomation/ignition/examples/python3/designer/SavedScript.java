package com.inductiveautomation.ignition.examples.python3.designer;

/**
 * Represents a saved Python script with full code.
 */
public class SavedScript {
    private String id;
    private String name;
    private String code;
    private String description;
    private String lastModified;

    public SavedScript() {
    }

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

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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
