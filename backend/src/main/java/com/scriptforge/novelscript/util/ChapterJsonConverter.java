package com.scriptforge.novelscript.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scriptforge.novelscript.common.BusinessException;
import com.scriptforge.novelscript.entity.Chapter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ChapterJsonConverter {

    private static final TypeReference<List<Chapter>> CHAPTER_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public ChapterJsonConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(List<Chapter> chapters) {
        try {
            return objectMapper.writeValueAsString(chapters == null ? List.of() : chapters);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("章节内容序列化失败");
        }
    }

    public List<Chapter> fromJson(String chaptersJson) {
        if (chaptersJson == null || chaptersJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(chaptersJson, CHAPTER_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("章节内容解析失败");
        }
    }
}
