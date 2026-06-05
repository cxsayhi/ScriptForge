package com.scriptforge.novelscript.util;

import com.scriptforge.novelscript.entity.Chapter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ChapterParser {

    private static final Pattern CHAPTER_HEADING = Pattern.compile(
            "(?m)^\\s*(?:#{1,6}\\s*)?((?:第\\s*[0-9一二三四五六七八九十百千零两]+\\s*[章节回卷].*)|(?:Chapter\\s+\\d+.*)|(?:CHAPTER\\s+\\d+.*))\\s*$"
    );

    public List<Chapter> parse(String rawText) {
        String normalized = normalize(rawText);
        Matcher matcher = CHAPTER_HEADING.matcher(normalized);
        List<Match> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(new Match(matcher.group(1).trim(), matcher.start(), matcher.end()));
        }

        if (matches.isEmpty()) {
            return parseByLargeBlocks(normalized);
        }

        List<Chapter> chapters = new ArrayList<>();
        for (int i = 0; i < matches.size(); i++) {
            Match current = matches.get(i);
            int nextStart = i + 1 < matches.size() ? matches.get(i + 1).start() : normalized.length();
            String content = normalized.substring(current.end(), nextStart).trim();
            chapters.add(new Chapter(i + 1, current.title(), content));
        }
        return chapters;
    }

    private List<Chapter> parseByLargeBlocks(String text) {
        String[] blocks = text.split("\\n\\s*\\n\\s*\\n+");
        List<Chapter> chapters = new ArrayList<>();
        int index = 1;
        for (String block : blocks) {
            String content = block.trim();
            if (content.length() >= 120) {
                chapters.add(new Chapter(index, "第 " + index + " 章", content));
                index++;
            }
        }
        return chapters;
    }

    private String normalize(String rawText) {
        if (rawText == null) {
            return "";
        }
        return rawText.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\uFEFF', ' ')
                .trim();
    }

    private record Match(String title, int start, int end) {
    }
}
