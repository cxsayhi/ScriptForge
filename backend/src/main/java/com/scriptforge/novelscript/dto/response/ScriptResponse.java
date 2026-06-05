package com.scriptforge.novelscript.dto.response;

import java.time.Instant;

public record ScriptResponse(
        Long projectId,
        String yaml,
        ValidationResult validationResult,
        Instant updatedAt
) {
}
