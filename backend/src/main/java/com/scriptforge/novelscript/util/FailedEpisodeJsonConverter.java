package com.scriptforge.novelscript.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scriptforge.novelscript.common.BusinessException;
import com.scriptforge.novelscript.entity.FailedEpisode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FailedEpisodeJsonConverter {

    private static final TypeReference<List<FailedEpisode>> FAILED_EPISODE_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public FailedEpisodeJsonConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(List<FailedEpisode> failedEpisodes) {
        try {
            return objectMapper.writeValueAsString(failedEpisodes == null ? List.of() : failedEpisodes);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("失败剧集信息序列化失败");
        }
    }

    public List<FailedEpisode> fromJson(String failedEpisodesJson) {
        if (failedEpisodesJson == null || failedEpisodesJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(failedEpisodesJson, FAILED_EPISODE_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("失败剧集信息解析失败");
        }
    }
}
