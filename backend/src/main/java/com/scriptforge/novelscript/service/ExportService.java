package com.scriptforge.novelscript.service;

import com.scriptforge.novelscript.common.BusinessException;
import com.scriptforge.novelscript.entity.ProjectWorkspace;
import com.scriptforge.novelscript.util.MarkdownExporter;
import org.springframework.stereotype.Service;

@Service
public class ExportService {

    private final ProjectService projectService;
    private final MarkdownExporter markdownExporter;

    public ExportService(ProjectService projectService, MarkdownExporter markdownExporter) {
        this.projectService = projectService;
        this.markdownExporter = markdownExporter;
    }

    public String yaml(Long projectId) {
        ProjectWorkspace project = projectService.get(projectId);
        ensureScript(project);
        return project.getScriptResult().getYaml();
    }

    public String markdown(Long projectId) {
        return markdownExporter.toMarkdown(yaml(projectId));
    }

    private void ensureScript(ProjectWorkspace project) {
        if (!project.getScriptResult().hasYaml()) {
            throw new BusinessException("当前项目还没有可导出的剧本。");
        }
    }
}
