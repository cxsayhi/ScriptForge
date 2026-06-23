package com.scriptforge.novelscript.controller;

import com.scriptforge.novelscript.common.ApiResponse;
import com.scriptforge.novelscript.dto.request.FailedEpisodeUpdateRequest;
import com.scriptforge.novelscript.dto.request.ScriptUpdateRequest;
import com.scriptforge.novelscript.dto.response.RepairResponse;
import com.scriptforge.novelscript.dto.response.ScriptQualityResponse;
import com.scriptforge.novelscript.dto.response.ScriptResponse;
import com.scriptforge.novelscript.dto.response.ValidationResult;
import com.scriptforge.novelscript.entity.GenerationStatus;
import com.scriptforge.novelscript.service.ScriptQualityService;
import com.scriptforge.novelscript.service.ScriptService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/script")
public class ScriptController {

    private final ScriptService scriptService;
    private final ScriptQualityService scriptQualityService;

    public ScriptController(ScriptService scriptService, ScriptQualityService scriptQualityService) {
        this.scriptService = scriptService;
        this.scriptQualityService = scriptQualityService;
    }

    @PostMapping("/generate")
    public ApiResponse<ScriptResponse> generate(@PathVariable Long projectId) {
        return ApiResponse.success(scriptService.generate(projectId));
    }

    @PostMapping("/regenerate")
    public ApiResponse<ScriptResponse> regenerate(@PathVariable Long projectId) {
        return ApiResponse.success(scriptService.generate(projectId));
    }

    @GetMapping("/status")
    public ApiResponse<GenerationStatus> status(@PathVariable Long projectId) {
        return ApiResponse.success(scriptService.status(projectId));
    }

    @GetMapping
    public ApiResponse<ScriptResponse> get(@PathVariable Long projectId) {
        return ApiResponse.success(scriptService.get(projectId));
    }

    @PutMapping
    public ApiResponse<ScriptResponse> update(@PathVariable Long projectId, @Valid @RequestBody ScriptUpdateRequest request) {
        return ApiResponse.success(scriptService.update(projectId, request.yaml()));
    }

    @PutMapping("/failed-episodes/{episodeId}")
    public ApiResponse<ScriptResponse> updateFailedEpisode(@PathVariable Long projectId,
                                                            @PathVariable int episodeId,
                                                            @Valid @RequestBody FailedEpisodeUpdateRequest request) {
        return ApiResponse.success(scriptService.updateFailedEpisode(projectId, episodeId, request.rawResponse()));
    }

    @PostMapping("/validate")
    public ApiResponse<ValidationResult> validate(@PathVariable Long projectId, @RequestBody(required = false) ScriptUpdateRequest request) {
        String yaml = request == null ? null : request.yaml();
        return ApiResponse.success(scriptService.validate(projectId, yaml));
    }

    @PostMapping("/repair")
    public ApiResponse<RepairResponse> repair(@PathVariable Long projectId, @RequestBody(required = false) ScriptUpdateRequest request) {
        String yaml = request == null ? null : request.yaml();
        return ApiResponse.success(scriptService.repair(projectId, yaml));
    }

    @PostMapping("/quality-check")
    public ApiResponse<ScriptQualityResponse> qualityCheck(@PathVariable Long projectId, @RequestBody(required = false) ScriptUpdateRequest request) {
        String yaml = request == null ? null : request.yaml();
        return ApiResponse.success(scriptQualityService.check(projectId, yaml));
    }
}
