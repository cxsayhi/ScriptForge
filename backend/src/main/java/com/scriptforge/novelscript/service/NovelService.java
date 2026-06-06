package com.scriptforge.novelscript.service;

import com.scriptforge.novelscript.entity.persistence.NovelContentRecord;
import com.scriptforge.novelscript.common.BusinessException;
import com.scriptforge.novelscript.dto.response.ChapterResponse;
import com.scriptforge.novelscript.dto.response.NovelResponse;
import com.scriptforge.novelscript.entity.Chapter;
import com.scriptforge.novelscript.entity.ProjectWorkspace;
import com.scriptforge.novelscript.mapper.NovelContentMapper;
import com.scriptforge.novelscript.util.ChapterParser;
import com.scriptforge.novelscript.util.ChapterJsonConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NovelService {

    private final ProjectService projectService;
    private final ChapterParser chapterParser;
    private final ChapterJsonConverter chapterJsonConverter;
    private final NovelContentMapper novelContentMapper;

    public NovelService(ProjectService projectService,
                        ChapterParser chapterParser,
                        ChapterJsonConverter chapterJsonConverter,
                        NovelContentMapper novelContentMapper) {
        this.projectService = projectService;
        this.chapterParser = chapterParser;
        this.chapterJsonConverter = chapterJsonConverter;
        this.novelContentMapper = novelContentMapper;
    }

    @Transactional
    public NovelResponse saveText(Long projectId, String text) {
        projectService.get(projectId);
        List<Chapter> chapters = chapterParser.parse(text);
        if (chapters.size() < 3) {
            throw new BusinessException("小说章节数量不足，请上传至少 3 个章节的小说文本。");
        }

        LocalDateTime now = LocalDateTime.now();
        NovelContentRecord record = projectService.findNovelContent(projectId);
        if (record == null) {
            record = new NovelContentRecord();
            record.setProjectId(projectId);
            record.setOriginalText(text);
            record.setChaptersJson(chapterJsonConverter.toJson(chapters));
            record.setUpdatedAt(now);
            novelContentMapper.insert(record);
        } else {
            record.setOriginalText(text);
            record.setChaptersJson(chapterJsonConverter.toJson(chapters));
            record.setUpdatedAt(now);
            novelContentMapper.updateById(record);
        }
        projectService.markNovelReady(projectId);
        return get(projectId);
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
