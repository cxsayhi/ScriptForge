package com.scriptforge.novelscript.entity;

import java.time.Instant;

public class ScriptResult {

    private String yaml = "";
    private Instant updatedAt = Instant.now();

    public String getYaml() {
        return yaml;
    }

    public void setYaml(String yaml) {
        this.yaml = yaml;
        this.updatedAt = Instant.now();
    }

    public boolean hasYaml() {
        return yaml != null && !yaml.isBlank();
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
