package com.scriptforge.novelscript.controller;

import com.scriptforge.novelscript.common.ApiResponse;
import com.scriptforge.novelscript.dto.request.NovelTextRequest;
import com.scriptforge.novelscript.dto.response.NovelResponse;
import com.scriptforge.novelscript.service.NovelService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/projects/{projectId}/novel")
public class NovelController {

    private final NovelService novelService;

    public NovelController(NovelService novelService) {
        this.novelService = novelService;
    }

    @PostMapping("/text")
    public ApiResponse<NovelResponse> submitText(@PathVariable Long projectId, @Valid @RequestBody NovelTextRequest request) {
        return ApiResponse.success(novelService.saveText(projectId, request.text()));
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<NovelResponse> uploadFile(@PathVariable Long projectId, @RequestPart("file") MultipartFile file) throws IOException {
        String text = new String(file.getBytes(), StandardCharsets.UTF_8);
        return ApiResponse.success(novelService.saveText(projectId, text));
    }

    @GetMapping
    public ApiResponse<NovelResponse> get(@PathVariable Long projectId) {
        return ApiResponse.success(novelService.get(projectId));
    }
}
