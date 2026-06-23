package com.scriptforge.novelscript.entity;

import java.time.Instant;
import java.util.List;

public class ScriptResult {

    private String yaml = "";
    private String rawLlmResponse = "";
    private String generationStatus = "completed";
    private String generationMessage = "";
    private List<FailedEpisode> failedEpisodes = List.of();
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

    public String getRawLlmResponse() {
        return rawLlmResponse;
    }

    public void setRawLlmResponse(String rawLlmResponse) {
        this.rawLlmResponse = rawLlmResponse == null ? "" : rawLlmResponse;
    }

    public String getGenerationStatus() {
        return generationStatus;
    }

    public void setGenerationStatus(String generationStatus) {
        this.generationStatus = generationStatus == null || generationStatus.isBlank() ? "completed" : generationStatus;
    }

    public String getGenerationMessage() {
        return generationMessage;
    }

    public void setGenerationMessage(String generationMessage) {
        this.generationMessage = generationMessage == null ? "" : generationMessage;
    }

    public boolean requiresReview() {
        return "needs_review".equals(generationStatus);
    }

    public boolean hasGeneratedContent() {
        return hasYaml() || !rawLlmResponse.isBlank() || !failedEpisodes.isEmpty();
    }

    public List<FailedEpisode> getFailedEpisodes() {
        return failedEpisodes;
    }

    public void setFailedEpisodes(List<FailedEpisode> failedEpisodes) {
        this.failedEpisodes = failedEpisodes == null ? List.of() : List.copyOf(failedEpisodes);
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
