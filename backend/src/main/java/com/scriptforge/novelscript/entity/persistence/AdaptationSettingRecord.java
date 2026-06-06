package com.scriptforge.novelscript.entity.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("adaptation_setting")
public class AdaptationSettingRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("script_type")
    private String scriptType;

    @TableField("target_episodes")
    private Integer targetEpisodes;

    @TableField("episode_duration_minutes")
    private Integer episodeDurationMinutes;

    private String style;

    private String language;

    @TableField("adaptation_intensity")
    private String adaptationIntensity;

    @TableField("dialogue_style")
    private String dialogueStyle;

    @TableField("budget_preference")
    private String budgetPreference;

    @TableField("keep_original_dialogues")
    private Boolean keepOriginalDialogues;

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

    public String getScriptType() {
        return scriptType;
    }

    public void setScriptType(String scriptType) {
        this.scriptType = scriptType;
    }

    public Integer getTargetEpisodes() {
        return targetEpisodes;
    }

    public void setTargetEpisodes(Integer targetEpisodes) {
        this.targetEpisodes = targetEpisodes;
    }

    public Integer getEpisodeDurationMinutes() {
        return episodeDurationMinutes;
    }

    public void setEpisodeDurationMinutes(Integer episodeDurationMinutes) {
        this.episodeDurationMinutes = episodeDurationMinutes;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getAdaptationIntensity() {
        return adaptationIntensity;
    }

    public void setAdaptationIntensity(String adaptationIntensity) {
        this.adaptationIntensity = adaptationIntensity;
    }

    public String getDialogueStyle() {
        return dialogueStyle;
    }

    public void setDialogueStyle(String dialogueStyle) {
        this.dialogueStyle = dialogueStyle;
    }

    public String getBudgetPreference() {
        return budgetPreference;
    }

    public void setBudgetPreference(String budgetPreference) {
        this.budgetPreference = budgetPreference;
    }

    public Boolean getKeepOriginalDialogues() {
        return keepOriginalDialogues;
    }

    public void setKeepOriginalDialogues(Boolean keepOriginalDialogues) {
        this.keepOriginalDialogues = keepOriginalDialogues;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
