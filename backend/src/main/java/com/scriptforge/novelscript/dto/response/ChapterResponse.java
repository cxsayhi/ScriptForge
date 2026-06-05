package com.scriptforge.novelscript.dto.response;

public record ChapterResponse(
        int index,
        String title,
        int contentLength,
        String preview
) {
}
