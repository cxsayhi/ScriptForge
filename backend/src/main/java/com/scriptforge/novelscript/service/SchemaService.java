package com.scriptforge.novelscript.service;

import com.scriptforge.novelscript.common.BusinessException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class SchemaService {

    public String scriptSchema() {
        return readResource("schema/script-schema.yaml");
    }

    public String scriptSchemaDesign() {
        return readResource("schema/script-schema-design.md");
    }

    private String readResource(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new BusinessException("资源文件不存在: " + path);
        }
    }
}
