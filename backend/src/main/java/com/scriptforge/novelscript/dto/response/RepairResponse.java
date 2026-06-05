package com.scriptforge.novelscript.dto.response;

public record RepairResponse(
        String yaml,
        ValidationResult validationResult
) {
}
