package com.scriptforge.novelscript.service;

import com.scriptforge.novelscript.dto.request.AdaptationSettingRequest;
import com.scriptforge.novelscript.entity.AdaptationSetting;
import com.scriptforge.novelscript.entity.ProjectWorkspace;
import org.springframework.stereotype.Service;

@Service
public class SettingService {

    private final ProjectService projectService;

    public SettingService(ProjectService projectService) {
        this.projectService = projectService;
    }

    public AdaptationSetting get(Long projectId) {
        return projectService.get(projectId).getSetting();
    }

    public AdaptationSetting save(Long projectId, AdaptationSettingRequest request) {
        ProjectWorkspace project = projectService.get(projectId);
        AdaptationSetting setting = new AdaptationSetting();
        setting.setScriptType(request.scriptType());
        setting.setTargetEpisodes(request.targetEpisodes());
        setting.setEpisodeDurationMinutes(request.episodeDurationMinutes());
        setting.setStyle(request.style());
        setting.setLanguage(request.language());
        setting.setAdaptationIntensity(request.adaptationIntensity());
        setting.setDialogueStyle(request.dialogueStyle());
        setting.setBudgetPreference(request.budgetPreference());
        setting.setKeepOriginalDialogues(request.keepOriginalDialogues());
        project.setSetting(setting);
        return setting;
    }
}
