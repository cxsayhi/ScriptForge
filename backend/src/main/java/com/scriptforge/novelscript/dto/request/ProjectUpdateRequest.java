package com.scriptforge.novelscript.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectUpdateRequest(
        @NotBlank @Size(max = 80) String title,
        @Size(max = 500) String description,
        @Size(max = 40) String status
) {
}
