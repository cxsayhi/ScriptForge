package com.scriptforge.novelscript.entity;

public class AdaptationSetting {

    private String scriptType = "web_drama";
    private int targetEpisodes = 3;
    private int episodeDurationMinutes = 8;
    private String style = "悬疑";
    private String language = "zh-CN";
    private String adaptationIntensity = "适度改编";
    private String dialogueStyle = "影视化";
    private String budgetPreference = "可拍摄优先";
    private boolean keepOriginalDialogues = true;

    public String getScriptType() {
        return scriptType;
    }

    public void setScriptType(String scriptType) {
        this.scriptType = scriptType;
    }

    public int getTargetEpisodes() {
        return targetEpisodes;
    }

    public void setTargetEpisodes(int targetEpisodes) {
        this.targetEpisodes = targetEpisodes;
    }

    public int getEpisodeDurationMinutes() {
        return episodeDurationMinutes;
    }

    public void setEpisodeDurationMinutes(int episodeDurationMinutes) {
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

    public boolean isKeepOriginalDialogues() {
        return keepOriginalDialogues;
    }

    public void setKeepOriginalDialogues(boolean keepOriginalDialogues) {
        this.keepOriginalDialogues = keepOriginalDialogues;
    }
}
