package com.scriptforge.novelscript.dto.response;

import com.scriptforge.novelscript.entity.FailedEpisode;

import java.util.List;

public record RepairResponse(
        String yaml,
        ValidationResult validationResult,
        String generationStatus,
        String generationMessage,
        String rawLlmResponse,
        List<FailedEpisode> failedEpisodes
) {
}
