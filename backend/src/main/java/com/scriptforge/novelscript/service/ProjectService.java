package com.scriptforge.novelscript.service;

import com.scriptforge.novelscript.common.BusinessException;
import com.scriptforge.novelscript.dto.request.ProjectCreateRequest;
import com.scriptforge.novelscript.dto.request.ProjectUpdateRequest;
import com.scriptforge.novelscript.dto.response.ProjectResponse;
import com.scriptforge.novelscript.entity.ProjectWorkspace;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ProjectService {

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Map<Long, ProjectWorkspace> projects = new ConcurrentHashMap<>();

    public ProjectResponse create(ProjectCreateRequest request) {
        ProjectWorkspace project = new ProjectWorkspace();
        project.setId(idGenerator.getAndIncrement());
        project.setTitle(request.title());
        project.setDescription(request.description());
        projects.put(project.getId(), project);
        return toResponse(project);
    }

    public List<ProjectResponse> list() {
        return projects.values().stream()
                .sorted(Comparator.comparing(ProjectWorkspace::getUpdatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    public ProjectResponse getResponse(Long projectId) {
        return toResponse(get(projectId));
    }

    public ProjectWorkspace get(Long projectId) {
        ProjectWorkspace project = projects.get(projectId);
        if (project == null) {
            throw new BusinessException(404, HttpStatus.NOT_FOUND, "项目不存在");
        }
        return project;
    }

    public ProjectResponse update(Long projectId, ProjectUpdateRequest request) {
        ProjectWorkspace project = get(projectId);
        project.setTitle(request.title());
        project.setDescription(request.description());
        if (request.status() != null && !request.status().isBlank()) {
            project.setStatus(request.status());
        }
        return toResponse(project);
    }

    public void delete(Long projectId) {
        ProjectWorkspace removed = projects.remove(projectId);
        if (removed == null) {
            throw new BusinessException(404, HttpStatus.NOT_FOUND, "项目不存在");
        }
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
}
