package com.scriptforge.novelscript.service;

import com.scriptforge.novelscript.ai.agent.ScriptGenerationAgent;
import com.scriptforge.novelscript.common.BusinessException;
import com.scriptforge.novelscript.dto.response.RepairResponse;
import com.scriptforge.novelscript.dto.response.ScriptResponse;
import com.scriptforge.novelscript.dto.response.ValidationResult;
import com.scriptforge.novelscript.entity.GenerationStatus;
import com.scriptforge.novelscript.entity.ProjectWorkspace;
import com.scriptforge.novelscript.util.YamlScriptValidator;
import org.springframework.stereotype.Service;

@Service
public class ScriptService {

    private final ProjectService projectService;
    private final ScriptGenerationAgent scriptGenerationAgent;
    private final YamlScriptValidator validator;

    public ScriptService(ProjectService projectService, ScriptGenerationAgent scriptGenerationAgent, YamlScriptValidator validator) {
        this.projectService = projectService;
        this.scriptGenerationAgent = scriptGenerationAgent;
        this.validator = validator;
    }

    public ScriptResponse generate(Long projectId) {
        ProjectWorkspace project = projectService.get(projectId);
        if (project.getNovelContent().getChapters().size() < 3) {
            throw new BusinessException("请先导入至少 3 个章节的小说文本。");
        }

        GenerationStatus status = project.getGenerationStatus();
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

        project.getScriptResult().setYaml(yaml);
        project.setStatus("script_ready");
        status.setStatus("completed");
        status.setMessage("剧本初稿已生成");
        project.touch();
        return toResponse(project);
    }

    public GenerationStatus status(Long projectId) {
        return projectService.get(projectId).getGenerationStatus();
    }

    public ScriptResponse get(Long projectId) {
        ProjectWorkspace project = projectService.get(projectId);
        return toResponse(project);
    }

    public ScriptResponse update(Long projectId, String yaml) {
        ProjectWorkspace project = projectService.get(projectId);
        ValidationResult validation = validator.validate(yaml);
        if (!validation.valid()) {
            throw new BusinessException("YAML 校验失败，未保存。请先修复错误。");
        }
        project.getScriptResult().setYaml(yaml);
        project.touch();
        return toResponse(project);
    }

    public ValidationResult validate(Long projectId, String yaml) {
        if (yaml == null || yaml.isBlank()) {
            yaml = projectService.get(projectId).getScriptResult().getYaml();
        }
        return validator.validate(yaml);
    }

    public RepairResponse repair(Long projectId, String yaml) {
        ProjectWorkspace project = projectService.get(projectId);
        String source = yaml == null || yaml.isBlank() ? project.getScriptResult().getYaml() : yaml;
        String repaired = validator.repair(source);
        ValidationResult validation = validator.validate(repaired);
        if (validation.valid()) {
            project.getScriptResult().setYaml(repaired);
            project.touch();
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
}
