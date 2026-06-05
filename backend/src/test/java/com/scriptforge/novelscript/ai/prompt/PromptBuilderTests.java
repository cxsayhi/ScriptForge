package com.scriptforge.novelscript.ai.prompt;

import com.scriptforge.novelscript.entity.Chapter;
import com.scriptforge.novelscript.entity.ProjectWorkspace;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTests {

    private final PromptBuilder promptBuilder = new PromptBuilder();

    @Test
    void buildScriptGenerationPromptInjectsProjectSettingsChaptersAndSchema() {
        ProjectWorkspace project = new ProjectWorkspace();
        project.setTitle("雨夜来信");
        project.getNovelContent().setChapters(List.of(
                new Chapter(1, "第一章 雨夜", "林秋收到一封没有署名的信。"),
                new Chapter(2, "第二章 旧剧院", "旧剧院里出现一张旧照片。"),
                new Chapter(3, "第三章 回声", "脚步声揭开更大的局。")
        ));
        project.getSetting().setScriptType("short_drama");
        project.getSetting().setTargetEpisodes(6);
        project.getSetting().setStyle("悬疑");

        String prompt = promptBuilder.buildScriptGenerationPrompt(project);

        assertThat(prompt)
                .contains("Novel title: 雨夜来信")
                .contains("Script type: short_drama")
                .contains("Target episodes: 6")
                .contains("--- Chapter 1: 第一章 雨夜 ---")
                .contains("林秋收到一封没有署名的信。")
                .contains("type: object")
                .doesNotContain("{{");
    }
}
