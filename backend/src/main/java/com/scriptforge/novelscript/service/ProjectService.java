package com.scriptforge.novelscript.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.scriptforge.novelscript.common.BusinessException;
import com.scriptforge.novelscript.dto.request.ProjectCreateRequest;
import com.scriptforge.novelscript.dto.request.ProjectUpdateRequest;
import com.scriptforge.novelscript.dto.response.ProjectResponse;
import com.scriptforge.novelscript.entity.AdaptationSetting;
import com.scriptforge.novelscript.entity.NovelContent;
import com.scriptforge.novelscript.entity.ProjectWorkspace;
import com.scriptforge.novelscript.entity.ScriptResult;
import com.scriptforge.novelscript.entity.persistence.AdaptationSettingRecord;
import com.scriptforge.novelscript.entity.persistence.NovelContentRecord;
import com.scriptforge.novelscript.entity.persistence.ProjectRecord;
import com.scriptforge.novelscript.entity.persistence.ScriptResultRecord;
import com.scriptforge.novelscript.mapper.AdaptationSettingMapper;
import com.scriptforge.novelscript.mapper.NovelContentMapper;
import com.scriptforge.novelscript.mapper.ProjectMapper;
import com.scriptforge.novelscript.mapper.ScriptResultMapper;
import com.scriptforge.novelscript.util.ChapterJsonConverter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final NovelContentMapper novelContentMapper;
    private final AdaptationSettingMapper adaptationSettingMapper;
    private final ScriptResultMapper scriptResultMapper;
    private final ChapterJsonConverter chapterJsonConverter;

    public ProjectService(ProjectMapper projectMapper,
                          NovelContentMapper novelContentMapper,
                          AdaptationSettingMapper adaptationSettingMapper,
                          ScriptResultMapper scriptResultMapper,
                          ChapterJsonConverter chapterJsonConverter) {
        this.projectMapper = projectMapper;
        this.novelContentMapper = novelContentMapper;
        this.adaptationSettingMapper = adaptationSettingMapper;
        this.scriptResultMapper = scriptResultMapper;
        this.chapterJsonConverter = chapterJsonConverter;
    }

    @Transactional
    public ProjectResponse create(ProjectCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();
        ProjectRecord record = new ProjectRecord();
        record.setTitle(request.title());
        record.setDescription(request.description());
        record.setStatus("draft");
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        projectMapper.insert(record);

        AdaptationSettingRecord settingRecord = toSettingRecord(record.getId(), new AdaptationSetting(), now);
        adaptationSettingMapper.insert(settingRecord);

        return getResponse(record.getId());
    }

    public List<ProjectResponse> list() {
        QueryWrapper<ProjectRecord> query = new QueryWrapper<ProjectRecord>()
                .orderByDesc("updated_at");
        return projectMapper.selectList(query).stream()
                .map(this::toWorkspace)
                .map(this::toResponse)
                .toList();
    }

    public ProjectResponse getResponse(Long projectId) {
        return toResponse(get(projectId));
    }

    public ProjectWorkspace get(Long projectId) {
        ProjectRecord record = projectMapper.selectById(projectId);
        if (record == null) {
            throw new BusinessException(404, HttpStatus.NOT_FOUND, "项目不存在");
        }
        return toWorkspace(record);
    }

    @Transactional
    public ProjectResponse update(Long projectId, ProjectUpdateRequest request) {
        ProjectRecord record = requireProjectRecord(projectId);
        record.setTitle(request.title());
        record.setDescription(request.description());
        if (request.status() != null && !request.status().isBlank()) {
            record.setStatus(request.status());
        }
        record.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(record);
        return getResponse(projectId);
    }

    @Transactional
    public void delete(Long projectId) {
        requireProjectRecord(projectId);
        scriptResultMapper.delete(new QueryWrapper<ScriptResultRecord>().eq("project_id", projectId));
        novelContentMapper.delete(new QueryWrapper<NovelContentRecord>().eq("project_id", projectId));
        adaptationSettingMapper.delete(new QueryWrapper<AdaptationSettingRecord>().eq("project_id", projectId));
        projectMapper.deleteById(projectId);
    }

    public ProjectResponse toResponse(ProjectWorkspace project) {
        int chapterCount = project.getNovelContent().getChapters().size();
        return new ProjectResponse(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getStatus(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                chapterCount > 0,
                chapterCount,
                project.getScriptResult().hasYaml()
        );
    }

    void markNovelReady(Long projectId) {
        ProjectRecord record = requireProjectRecord(projectId);
        record.setStatus("novel_ready");
        record.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(record);
    }

    void markScriptReady(Long projectId) {
        ProjectRecord record = requireProjectRecord(projectId);
        record.setStatus("script_ready");
        record.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(record);
    }

    private ProjectWorkspace toWorkspace(ProjectRecord record) {
        ProjectWorkspace project = new ProjectWorkspace();
        project.setId(record.getId());
        project.setTitle(record.getTitle());
        project.setDescription(record.getDescription());
        project.setStatus(record.getStatus());
        project.setCreatedAt(toInstant(record.getCreatedAt()));
        project.setUpdatedAt(toInstant(record.getUpdatedAt()));
        applyNovelContent(project);
        applyAdaptationSetting(project);
        applyScriptResult(project);
        project.setUpdatedAt(toInstant(record.getUpdatedAt()));
        return project;
    }

    private void applyNovelContent(ProjectWorkspace project) {
        NovelContentRecord record = findNovelContent(project.getId());
        if (record == null) {
            return;
        }
        NovelContent novelContent = project.getNovelContent();
        novelContent.setOriginalText(record.getOriginalText());
        novelContent.setChapters(chapterJsonConverter.fromJson(record.getChaptersJson()));
        novelContent.setUpdatedAt(toInstant(record.getUpdatedAt()));
    }

    private void applyAdaptationSetting(ProjectWorkspace project) {
        AdaptationSettingRecord record = findAdaptationSetting(project.getId());
        if (record == null) {
            return;
        }
        project.setSetting(toSetting(record));
    }

    private void applyScriptResult(ProjectWorkspace project) {
        ScriptResultRecord record = findScriptResult(project.getId());
        if (record == null) {
            return;
        }
        ScriptResult scriptResult = project.getScriptResult();
        scriptResult.setYaml(record.getYaml());
        scriptResult.setUpdatedAt(toInstant(record.getUpdatedAt()));
    }

    private AdaptationSetting toSetting(AdaptationSettingRecord record) {
        AdaptationSetting setting = new AdaptationSetting();
        setting.setScriptType(record.getScriptType());
        setting.setTargetEpisodes(record.getTargetEpisodes());
        setting.setEpisodeDurationMinutes(record.getEpisodeDurationMinutes());
        setting.setStyle(record.getStyle());
        setting.setLanguage(record.getLanguage());
        setting.setAdaptationIntensity(record.getAdaptationIntensity());
        setting.setDialogueStyle(record.getDialogueStyle());
        setting.setBudgetPreference(record.getBudgetPreference());
        setting.setKeepOriginalDialogues(Boolean.TRUE.equals(record.getKeepOriginalDialogues()));
        return setting;
    }

    AdaptationSettingRecord toSettingRecord(Long projectId, AdaptationSetting setting, LocalDateTime updatedAt) {
        AdaptationSettingRecord record = new AdaptationSettingRecord();
        record.setProjectId(projectId);
        record.setScriptType(setting.getScriptType());
        record.setTargetEpisodes(setting.getTargetEpisodes());
        record.setEpisodeDurationMinutes(setting.getEpisodeDurationMinutes());
        record.setStyle(setting.getStyle());
        record.setLanguage(setting.getLanguage());
        record.setAdaptationIntensity(setting.getAdaptationIntensity());
        record.setDialogueStyle(setting.getDialogueStyle());
        record.setBudgetPreference(setting.getBudgetPreference());
        record.setKeepOriginalDialogues(setting.isKeepOriginalDialogues());
        record.setUpdatedAt(updatedAt);
        return record;
    }

    AdaptationSettingRecord findAdaptationSetting(Long projectId) {
        List<AdaptationSettingRecord> records = adaptationSettingMapper.selectList(
                new QueryWrapper<AdaptationSettingRecord>()
                        .eq("project_id", projectId)
                        .orderByDesc("updated_at")
        );
        return records.isEmpty() ? null : records.get(0);
    }

    NovelContentRecord findNovelContent(Long projectId) {
        List<NovelContentRecord> records = novelContentMapper.selectList(
                new QueryWrapper<NovelContentRecord>()
                        .eq("project_id", projectId)
                        .orderByDesc("updated_at")
        );
        return records.isEmpty() ? null : records.get(0);
    }

    ScriptResultRecord findScriptResult(Long projectId) {
        List<ScriptResultRecord> records = scriptResultMapper.selectList(
                new QueryWrapper<ScriptResultRecord>()
                        .eq("project_id", projectId)
                        .orderByDesc("updated_at")
        );
        return records.isEmpty() ? null : records.get(0);
    }

    private ProjectRecord requireProjectRecord(Long projectId) {
        ProjectRecord record = projectMapper.selectById(projectId);
        if (record == null) {
            throw new BusinessException(404, HttpStatus.NOT_FOUND, "项目不存在");
        }
        return record;
    }

    private Instant toInstant(LocalDateTime value) {
        if (value == null) {
            return Instant.now();
        }
        return value.atZone(ZoneId.systemDefault()).toInstant();
    }
}
