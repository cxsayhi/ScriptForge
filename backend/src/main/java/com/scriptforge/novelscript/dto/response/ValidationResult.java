package com.scriptforge.novelscript.dto.response;

import java.util.List;

public record ValidationResult(
        boolean valid,
        List<String> errors,
        List<String> warnings
) {
}
