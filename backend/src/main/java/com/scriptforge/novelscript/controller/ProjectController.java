package com.scriptforge.novelscript.controller;

import com.scriptforge.novelscript.common.ApiResponse;
import com.scriptforge.novelscript.dto.request.ProjectCreateRequest;
import com.scriptforge.novelscript.dto.request.ProjectUpdateRequest;
import com.scriptforge.novelscript.dto.response.ProjectResponse;
import com.scriptforge.novelscript.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ApiResponse<ProjectResponse> create(@Valid @RequestBody ProjectCreateRequest request) {
        return ApiResponse.success(projectService.create(request));
    }

    @GetMapping
    public ApiResponse<List<ProjectResponse>> list() {
        return ApiResponse.success(projectService.list());
    }

    @GetMapping("/{projectId}")
    public ApiResponse<ProjectResponse> get(@PathVariable Long projectId) {
        return ApiResponse.success(projectService.getResponse(projectId));
    }

    @PutMapping("/{projectId}")
    public ApiResponse<ProjectResponse> update(@PathVariable Long projectId, @Valid @RequestBody ProjectUpdateRequest request) {
        return ApiResponse.success(projectService.update(projectId, request));
    }

    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> delete(@PathVariable Long projectId) {
        projectService.delete(projectId);
        return ApiResponse.success();
    }
}
