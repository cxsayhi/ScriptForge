package com.scriptforge.novelscript.dto.response;

public record ScriptQualityIssue(
        String severity,
        String path,
        String message,
        String suggestion
) {
}
