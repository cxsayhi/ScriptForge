package com.scriptforge.novelscript.entity;

import java.time.Instant;

public class ProjectWorkspace {

    private Long id;
    private String title;
    private String description;
    private String status = "draft";
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    private NovelContent novelContent = new NovelContent();
    private AdaptationSetting setting = new AdaptationSetting();
    private ScriptResult scriptResult = new ScriptResult();
    private GenerationStatus generationStatus = new GenerationStatus();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        touch();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        touch();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        touch();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public NovelContent getNovelContent() {
        return novelContent;
    }

    public AdaptationSetting getSetting() {
        return setting;
    }

    public void setSetting(AdaptationSetting setting) {
        this.setting = setting;
        touch();
    }

    public ScriptResult getScriptResult() {
        return scriptResult;
    }

    public GenerationStatus getGenerationStatus() {
        return generationStatus;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
