package com.scriptforge.novelscript.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.scriptforge.novelscript.ai.agent.ScriptGenerationAgent;
import com.scriptforge.novelscript.ai.agent.ScriptGenerationResult;
import com.scriptforge.novelscript.common.BusinessException;
import com.scriptforge.novelscript.dto.response.RepairResponse;
import com.scriptforge.novelscript.dto.response.ScriptResponse;
import com.scriptforge.novelscript.dto.response.ValidationResult;
import com.scriptforge.novelscript.entity.FailedEpisode;
import com.scriptforge.novelscript.entity.GenerationStatus;
import com.scriptforge.novelscript.entity.ProjectWorkspace;
import com.scriptforge.novelscript.entity.persistence.ScriptResultRecord;
import com.scriptforge.novelscript.mapper.ScriptResultMapper;
import com.scriptforge.novelscript.util.FailedEpisodeJsonConverter;
import com.scriptforge.novelscript.util.YamlScriptValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ScriptService {

    private final ProjectService projectService;
    private final ScriptGenerationAgent scriptGenerationAgent;
    private final YamlScriptValidator validator;
    private final ScriptResultMapper scriptResultMapper;
    private final FailedEpisodeJsonConverter failedEpisodeJsonConverter;
    private final Map<Long, GenerationStatus> generationStatuses = new ConcurrentHashMap<>();

    public ScriptService(ProjectService projectService,
                         ScriptGenerationAgent scriptGenerationAgent,
                         YamlScriptValidator validator,
                         ScriptResultMapper scriptResultMapper,
                         FailedEpisodeJsonConverter failedEpisodeJsonConverter) {
        this.projectService = projectService;
        this.scriptGenerationAgent = scriptGenerationAgent;
        this.validator = validator;
        this.scriptResultMapper = scriptResultMapper;
        this.failedEpisodeJsonConverter = failedEpisodeJsonConverter;
    }

    @Transactional
    public ScriptResponse generate(Long projectId) {
        ProjectWorkspace project = projectService.get(projectId);
        if (project.getNovelContent().getChapters().size() < 3) {
            throw new BusinessException("请先导入至少 3 个章节的小说文本。");
        }

        GenerationStatus status = generationStatuses.computeIfAbsent(projectId, ignored -> new GenerationStatus());
        status.setStatus("running");
        status.setMessage("正在生成剧本初稿");

        ScriptGenerationResult generationResult;
        ValidationResult validation;
        try {
            generationResult = scriptGenerationAgent.generateResult(project);
            validation = validator.validate(generationResult.yaml());
        } catch (BusinessException exception) {
            status.setStatus("failed");
            status.setMessage(exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            status.setStatus("failed");
            status.setMessage("剧本生成失败: " + exception.getMessage());
            throw exception;
        }

        boolean needsReview = generationResult.requiresReview() || !validation.valid();
        if (needsReview) {
            String message = generationResult.requiresReview()
                    ? generationResult.message()
                    : "生成结果未通过 YAML 校验：" + String.join("；", validation.errors());
            generationResult = ScriptGenerationResult.needsReview(
                    generationResult.yaml(),
                    generationResult.rawLlmResponse(),
                    message,
                    generationResult.failedEpisodes()
            );
            saveScriptResult(projectId, generationResult, validation);
            projectService.markScriptNeedsReview(projectId);
            status.setStatus("needs_review");
            status.setMessage(message);
            return get(projectId);
        }

        saveScriptResult(projectId, generationResult, validation);
        projectService.markScriptReady(projectId);
        status.setStatus("completed");
        status.setMessage("剧本初稿已生成");
        return get(projectId);
    }

    public GenerationStatus status(Long projectId) {
        GenerationStatus cached = generationStatuses.get(projectId);
        if (cached != null) {
            return cached;
        }
        ProjectWorkspace project = projectService.get(projectId);
        GenerationStatus status = new GenerationStatus();
        if (project.getScriptResult().requiresReview()) {
            status.setStatus("needs_review");
            status.setMessage(project.getScriptResult().getGenerationMessage());
        } else if (project.getScriptResult().hasYaml()) {
            status.setStatus("completed");
            status.setMessage("剧本初稿已生成");
        }
        return status;
    }

    public ScriptResponse get(Long projectId) {
        ProjectWorkspace project = projectService.get(projectId);
        return toResponse(project);
    }

    @Transactional
    public ScriptResponse update(Long projectId, String yaml) {
        projectService.get(projectId);
        ValidationResult validation = validator.validate(yaml);
        if (!validation.valid()) {
            throw new BusinessException("YAML 校验失败，未保存。请先修复错误。");
        }
        saveScriptResult(projectId, ScriptGenerationResult.completed(yaml), validation);
        projectService.markScriptReady(projectId);
        return get(projectId);
    }

    public ValidationResult validate(Long projectId, String yaml) {
        if (yaml == null || yaml.isBlank()) {
            yaml = projectService.get(projectId).getScriptResult().getYaml();
        }
        return validator.validate(yaml);
    }

    @Transactional
    public RepairResponse repair(Long projectId, String yaml) {
        ProjectWorkspace project = projectService.get(projectId);
        String source = yaml == null || yaml.isBlank() ? project.getScriptResult().getYaml() : yaml;
        String repaired = validator.repair(source, project);
        ValidationResult validation = validator.validate(repaired);
        ScriptGenerationResult result = null;
        if (validation.valid()) {
            if (project.getScriptResult().getFailedEpisodes().isEmpty()) {
                result = ScriptGenerationResult.completed(repaired);
                projectService.markScriptReady(projectId);
            } else {
                result = ScriptGenerationResult.needsReview(
                        repaired,
                        project.getScriptResult().getRawLlmResponse(),
                        project.getScriptResult().getGenerationMessage(),
                        project.getScriptResult().getFailedEpisodes()
                );
                projectService.markScriptNeedsReview(projectId);
            }
            saveScriptResult(projectId, result, validation);
        }
        return new RepairResponse(
                repaired,
                validation,
                result == null ? project.getScriptResult().getGenerationStatus() : result.status(),
                result == null ? project.getScriptResult().getGenerationMessage() : result.message(),
                result == null ? project.getScriptResult().getRawLlmResponse() : result.rawLlmResponse(),
                result == null ? project.getScriptResult().getFailedEpisodes() : result.failedEpisodes()
        );
    }

    @Transactional
    public ScriptResponse updateFailedEpisode(Long projectId, int episodeId, String rawResponse) {
        ProjectWorkspace project = projectService.get(projectId);
        List<FailedEpisode> updatedEpisodes = new ArrayList<>();
        boolean found = false;
        for (FailedEpisode failedEpisode : project.getScriptResult().getFailedEpisodes()) {
            if (failedEpisode.episodeId() == episodeId) {
                updatedEpisodes.add(failedEpisode.withEditedRawResponse(rawResponse));
                found = true;
            } else {
                updatedEpisodes.add(failedEpisode);
            }
        }
        if (!found) {
            throw new BusinessException("待审核的第 " + episodeId + " 集不存在");
        }

        ScriptResultRecord record = findScriptResult(projectId);
        if (record == null) {
            throw new BusinessException("剧本结果不存在");
        }
        record.setFailedEpisodesJson(failedEpisodeJsonConverter.toJson(updatedEpisodes));
        record.setUpdatedAt(LocalDateTime.now());
        scriptResultMapper.updateById(record);
        return get(projectId);
    }

    private ScriptResponse toResponse(ProjectWorkspace project) {
        String yaml = project.getScriptResult().getYaml();
        return new ScriptResponse(
                project.getId(),
                yaml,
                validator.validate(yaml),
                project.getScriptResult().getUpdatedAt(),
                project.getScriptResult().getGenerationStatus(),
                project.getScriptResult().getGenerationMessage(),
                project.getScriptResult().getRawLlmResponse(),
                project.getScriptResult().getFailedEpisodes()
        );
    }

    private void saveScriptResult(Long projectId,
                                  ScriptGenerationResult generationResult,
                                  ValidationResult validation) {
        LocalDateTime now = LocalDateTime.now();
        ScriptResultRecord record = findScriptResult(projectId);
        if (record == null) {
            record = new ScriptResultRecord();
            record.setProjectId(projectId);
        }
        record.setYaml(generationResult.yaml());
        record.setValidationStatus(validation.valid() ? "valid" : "invalid");
        record.setRawLlmResponse(generationResult.requiresReview() ? generationResult.rawLlmResponse() : null);
        record.setGenerationStatus(generationResult.status());
        record.setGenerationMessage(generationResult.message());
        record.setFailedEpisodesJson(failedEpisodeJsonConverter.toJson(generationResult.failedEpisodes()));
        record.setUpdatedAt(now);
        if (record.getId() == null) {
            scriptResultMapper.insert(record);
        } else {
            scriptResultMapper.updateById(record);
        }
    }

    private ScriptResultRecord findScriptResult(Long projectId) {
        return scriptResultMapper.selectList(
                new QueryWrapper<ScriptResultRecord>()
                        .eq("project_id", projectId)
                        .orderByDesc("updated_at")
        ).stream().findFirst().orElse(null);
    }
}
