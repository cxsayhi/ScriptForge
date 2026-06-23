package com.scriptforge.novelscript.dto.response;

import com.scriptforge.novelscript.entity.FailedEpisode;

import java.time.Instant;
import java.util.List;

public record ScriptResponse(
        Long projectId,
        String yaml,
        ValidationResult validationResult,
        Instant updatedAt,
        String generationStatus,
        String generationMessage,
        String rawLlmResponse,
        List<FailedEpisode> failedEpisodes
) {
}
