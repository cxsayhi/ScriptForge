package com.scriptforge.novelscript.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AdaptationSettingRequest(
        @NotBlank String scriptType,
        @Min(1) @Max(80) int targetEpisodes,
        @Min(1) @Max(180) int episodeDurationMinutes,
        @NotBlank String style,
        @NotBlank String language,
        @NotBlank String adaptationIntensity,
        @NotBlank String dialogueStyle,
        @NotBlank String budgetPreference,
        boolean keepOriginalDialogues
) {
}
