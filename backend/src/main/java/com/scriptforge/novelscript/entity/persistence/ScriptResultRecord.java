package com.scriptforge.novelscript.entity.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("script_result")
public class ScriptResultRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    private String yaml;

    @TableField("validation_status")
    private String validationStatus;

    @TableField("raw_llm_response")
    private String rawLlmResponse;

    @TableField("generation_status")
    private String generationStatus;

    @TableField("generation_message")
    private String generationMessage;

    @TableField("failed_episodes_json")
    private String failedEpisodesJson;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getYaml() {
        return yaml;
    }

    public void setYaml(String yaml) {
        this.yaml = yaml;
    }

    public String getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getRawLlmResponse() {
        return rawLlmResponse;
    }

    public void setRawLlmResponse(String rawLlmResponse) {
        this.rawLlmResponse = rawLlmResponse;
    }

    public String getGenerationStatus() {
        return generationStatus;
    }

    public void setGenerationStatus(String generationStatus) {
        this.generationStatus = generationStatus;
    }

    public String getGenerationMessage() {
        return generationMessage;
    }

    public void setGenerationMessage(String generationMessage) {
        this.generationMessage = generationMessage;
    }

    public String getFailedEpisodesJson() {
        return failedEpisodesJson;
    }

    public void setFailedEpisodesJson(String failedEpisodesJson) {
        this.failedEpisodesJson = failedEpisodesJson;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
