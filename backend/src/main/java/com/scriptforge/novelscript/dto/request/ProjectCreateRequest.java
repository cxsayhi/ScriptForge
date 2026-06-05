package com.scriptforge.novelscript.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectCreateRequest(
        @NotBlank @Size(max = 80) String title,
        @Size(max = 500) String description
) {
}
