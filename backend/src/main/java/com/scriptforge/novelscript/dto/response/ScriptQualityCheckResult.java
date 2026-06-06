package com.scriptforge.novelscript.dto.response;

import java.util.List;

public record ScriptQualityCheckResult(
        String category,
        String label,
        String status,
        int score,
        List<ScriptQualityIssue> issues
) {
}
