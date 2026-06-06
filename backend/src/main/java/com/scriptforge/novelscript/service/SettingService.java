package com.scriptforge.novelscript.service;

import com.scriptforge.novelscript.dto.request.AdaptationSettingRequest;
import com.scriptforge.novelscript.entity.AdaptationSetting;
import com.scriptforge.novelscript.entity.persistence.AdaptationSettingRecord;
import com.scriptforge.novelscript.entity.ProjectWorkspace;
import com.scriptforge.novelscript.mapper.AdaptationSettingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SettingService {

    private final ProjectService projectService;
    private final AdaptationSettingMapper adaptationSettingMapper;

    public SettingService(ProjectService projectService, AdaptationSettingMapper adaptationSettingMapper) {
        this.projectService = projectService;
        this.adaptationSettingMapper = adaptationSettingMapper;
    }

    public AdaptationSetting get(Long projectId) {
        return projectService.get(projectId).getSetting();
    }

    @Transactional
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
        AdaptationSettingRecord record = projectService.findAdaptationSetting(projectId);
        AdaptationSettingRecord newValues = projectService.toSettingRecord(projectId, setting, LocalDateTime.now());
        if (record == null) {
            adaptationSettingMapper.insert(newValues);
        } else {
            newValues.setId(record.getId());
            adaptationSettingMapper.updateById(newValues);
        }
        project.setSetting(setting);
        return setting;
    }
}
