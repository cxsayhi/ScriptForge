package com.scriptforge.novelscript.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FailedEpisodeUpdateRequest(@NotBlank String rawResponse) {
}
