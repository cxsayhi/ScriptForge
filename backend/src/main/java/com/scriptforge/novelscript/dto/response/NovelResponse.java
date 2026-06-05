package com.scriptforge.novelscript.dto.response;

import java.time.Instant;
import java.util.List;

public record NovelResponse(
        Long projectId,
        int chapterCount,
        List<ChapterResponse> chapters,
        String originalText,
        Instant updatedAt
) {
}
