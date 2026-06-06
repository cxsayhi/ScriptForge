package com.scriptforge.novelscript.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.scriptforge.novelscript.ai.agent.ScriptGenerationAgent;
import com.scriptforge.novelscript.common.BusinessException;
import com.scriptforge.novelscript.dto.response.RepairResponse;
import com.scriptforge.novelscript.dto.response.ScriptResponse;
import com.scriptforge.novelscript.dto.response.ValidationResult;
import com.scriptforge.novelscript.entity.GenerationStatus;
import com.scriptforge.novelscript.entity.ProjectWorkspace;
import com.scriptforge.novelscript.entity.persistence.ScriptResultRecord;
import com.scriptforge.novelscript.mapper.ScriptResultMapper;
import com.scriptforge.novelscript.util.YamlScriptValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ScriptService {

    private final ProjectService projectService;
    private final ScriptGenerationAgent scriptGenerationAgent;
    private final YamlScriptValidator validator;
    private final ScriptResultMapper scriptResultMapper;
    private final Map<Long, GenerationStatus> generationStatuses = new ConcurrentHashMap<>();

    public ScriptService(ProjectService projectService,
                         ScriptGenerationAgent scriptGenerationAgent,
                         YamlScriptValidator validator,
                         ScriptResultMapper scriptResultMapper) {
        this.projectService = projectService;
        this.scriptGenerationAgent = scriptGenerationAgent;
        this.validator = validator;
        this.scriptResultMapper = scriptResultMapper;
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

        String yaml;
        ValidationResult validation;
        try {
            yaml = scriptGenerationAgent.generate(project);
            validation = validator.validate(yaml);
            if (!validation.valid()) {
                status.setStatus("failed");
                status.setMessage("生成结果未通过 YAML 校验");
                throw new BusinessException("生成结果未通过 YAML 校验");
            }
        } catch (BusinessException exception) {
            status.setStatus("failed");
            status.setMessage(exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            status.setStatus("failed");
            status.setMessage("剧本生成失败: " + exception.getMessage());
            throw exception;
        }

        saveScriptResult(projectId, yaml, validation);
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
        if (project.getScriptResult().hasYaml()) {
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
        saveScriptResult(projectId, yaml, validation);
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
        String source = yaml == null || yaml.isBlank() ? projectService.get(projectId).getScriptResult().getYaml() : yaml;
        String repaired = validator.repair(source);
        ValidationResult validation = validator.validate(repaired);
        if (validation.valid()) {
            saveScriptResult(projectId, repaired, validation);
            projectService.markScriptReady(projectId);
        }
        return new RepairResponse(repaired, validation);
    }

    private ScriptResponse toResponse(ProjectWorkspace project) {
        String yaml = project.getScriptResult().getYaml();
        return new ScriptResponse(
                project.getId(),
                yaml,
                validator.validate(yaml),
                project.getScriptResult().getUpdatedAt()
        );
    }

    private void saveScriptResult(Long projectId, String yaml, ValidationResult validation) {
        LocalDateTime now = LocalDateTime.now();
        ScriptResultRecord record = findScriptResult(projectId);
        if (record == null) {
            record = new ScriptResultRecord();
            record.setProjectId(projectId);
            record.setYaml(yaml);
            record.setValidationStatus(validation.valid() ? "valid" : "invalid");
            record.setUpdatedAt(now);
            scriptResultMapper.insert(record);
        } else {
            record.setYaml(yaml);
            record.setValidationStatus(validation.valid() ? "valid" : "invalid");
            record.setUpdatedAt(now);
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
