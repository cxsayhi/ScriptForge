package com.scriptforge.novelscript.controller;

import com.scriptforge.novelscript.service.ExportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/projects/{projectId}/export")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/yaml")
    public ResponseEntity<byte[]> yaml(@PathVariable Long projectId) {
        return file("script-" + projectId + ".yaml", "application/x-yaml", exportService.yaml(projectId));
    }

    @GetMapping("/markdown")
    public ResponseEntity<byte[]> markdown(@PathVariable Long projectId) {
        return file("script-" + projectId + ".md", MediaType.TEXT_MARKDOWN_VALUE, exportService.markdown(projectId));
    }

    private ResponseEntity<byte[]> file(String filename, String contentType, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.parseMediaType(contentType + ";charset=UTF-8"))
                .body(bytes);
    }
}
