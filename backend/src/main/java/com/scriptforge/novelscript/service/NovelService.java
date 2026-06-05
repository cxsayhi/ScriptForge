package com.scriptforge.novelscript.service;

import com.scriptforge.novelscript.common.BusinessException;
import com.scriptforge.novelscript.dto.response.ChapterResponse;
import com.scriptforge.novelscript.dto.response.NovelResponse;
import com.scriptforge.novelscript.entity.Chapter;
import com.scriptforge.novelscript.entity.ProjectWorkspace;
import com.scriptforge.novelscript.util.ChapterParser;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NovelService {

    private final ProjectService projectService;
    private final ChapterParser chapterParser;

    public NovelService(ProjectService projectService, ChapterParser chapterParser) {
        this.projectService = projectService;
        this.chapterParser = chapterParser;
    }

    public NovelResponse saveText(Long projectId, String text) {
        ProjectWorkspace project = projectService.get(projectId);
        List<Chapter> chapters = chapterParser.parse(text);
        if (chapters.size() < 3) {
            throw new BusinessException("小说章节数量不足，请上传至少 3 个章节的小说文本。");
        }

        project.getNovelContent().setOriginalText(text);
        project.getNovelContent().setChapters(chapters);
        project.setStatus("novel_ready");
        project.touch();
        return toResponse(project);
    }

    public NovelResponse get(Long projectId) {
        return toResponse(projectService.get(projectId));
    }

    private NovelResponse toResponse(ProjectWorkspace project) {
        List<ChapterResponse> chapters = project.getNovelContent().getChapters().stream()
                .map(this::toChapterResponse)
                .toList();
        return new NovelResponse(
                project.getId(),
                chapters.size(),
                chapters,
                project.getNovelContent().getOriginalText(),
                project.getNovelContent().getUpdatedAt()
        );
    }

    private ChapterResponse toChapterResponse(Chapter chapter) {
        String compact = chapter.content() == null ? "" : chapter.content().replaceAll("\\s+", " ").trim();
        String preview = compact.length() > 90 ? compact.substring(0, 90) + "..." : compact;
        return new ChapterResponse(chapter.index(), chapter.title(), chapter.content().length(), preview);
    }
}
