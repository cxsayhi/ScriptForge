package com.scriptforge.novelscript.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ScriptUpdateRequest(@NotBlank String yaml) {
}
