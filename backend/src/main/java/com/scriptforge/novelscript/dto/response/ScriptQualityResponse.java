package com.scriptforge.novelscript.dto.response;

import java.util.List;

public record ScriptQualityResponse(
        Long projectId,
        int overallScore,
        boolean passed,
        String summary,
        ValidationResult validationResult,
        List<ScriptQualityCheckResult> checks
) {
}
