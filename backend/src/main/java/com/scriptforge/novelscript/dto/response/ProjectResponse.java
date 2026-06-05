package com.scriptforge.novelscript.dto.response;

import java.time.Instant;

public record ProjectResponse(
        Long id,
        String title,
        String description,
        String status,
        Instant createdAt,
        Instant updatedAt,
        boolean hasNovel,
        int chapterCount,
        boolean hasScript
) {
}
