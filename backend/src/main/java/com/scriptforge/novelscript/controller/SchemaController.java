package com.scriptforge.novelscript.controller;

import com.scriptforge.novelscript.common.ApiResponse;
import com.scriptforge.novelscript.service.SchemaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schema/script")
public class SchemaController {

    private final SchemaService schemaService;

    public SchemaController(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @GetMapping
    public ApiResponse<String> schema() {
        return ApiResponse.success(schemaService.scriptSchema());
    }

    @GetMapping("/design")
    public ApiResponse<String> design() {
        return ApiResponse.success(schemaService.scriptSchemaDesign());
    }
}
