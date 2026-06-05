package com.scriptforge.novelscript.controller;

import com.scriptforge.novelscript.common.ApiResponse;
import com.scriptforge.novelscript.dto.request.AdaptationSettingRequest;
import com.scriptforge.novelscript.entity.AdaptationSetting;
import com.scriptforge.novelscript.service.SettingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/settings")
public class SettingController {

    private final SettingService settingService;

    public SettingController(SettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping
    public ApiResponse<AdaptationSetting> get(@PathVariable Long projectId) {
        return ApiResponse.success(settingService.get(projectId));
    }

    @PutMapping
    public ApiResponse<AdaptationSetting> save(@PathVariable Long projectId, @Valid @RequestBody AdaptationSettingRequest request) {
        return ApiResponse.success(settingService.save(projectId, request));
    }
}
