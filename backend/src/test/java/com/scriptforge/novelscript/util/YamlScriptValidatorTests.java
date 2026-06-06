package com.scriptforge.novelscript.util;

import com.scriptforge.novelscript.dto.response.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YamlScriptValidatorTests {

    private final YamlScriptValidator validator = new YamlScriptValidator();

    @Test
    void validateAcceptsCompleteScriptYaml() {
        ValidationResult result = validator.validate(validYaml());

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void validateRejectsWrongFieldTypesAndEnumValues() {
        String yaml = validYaml()
                .replace("script_type: short_drama", "script_type: series")
                .replace("target_episodes: 3", "target_episodes: \"3\"")
                .replace("episode_id: 1", "episode_id: one")
                .replace("characters:\n          - char_001", "characters: char_001");

        ValidationResult result = validator.validate(yaml);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .anyMatch(error -> error.contains("project.script_type 必须是以下值之一"))
                .contains("project.target_episodes 必须是整数")
                .contains("episodes[0].episode_id 必须是整数")
                .contains("episodes[0].scenes[0].characters 必须是数组");
    }

    @Test
    void validateRejectsEmptyScenesAndDialogues() {
        String emptyScenes = validYaml().replace("""
                    scenes:
                      - scene_id: 1-1
                        title: 匿名信
                        location: 林秋家
                        time: 夜晚
                        characters:
                          - char_001
                        action: 林秋读完匿名信，决定前往旧剧院。
                        dialogues:
                          - character: char_001
                            line: 我必须弄清楚这封信是谁寄来的。
                """, """
                    scenes: []
                """);
        String emptyDialogues = validYaml().replace("""
                        dialogues:
                          - character: char_001
                            line: 我必须弄清楚这封信是谁寄来的。
                """, """
                        dialogues: []
                """);

        assertThat(validator.validate(emptyScenes).errors())
                .contains("episodes[0].scenes 必须是至少包含 1 个场景的数组");
        assertThat(validator.validate(emptyDialogues).errors())
                .contains("episodes[0].scenes[0].dialogues 必须至少包含 1 条对白");
    }

    @Test
    void repairExtractsYamlFromMarkdownCodeFence() {
        String repaired = validator.repair("""
                下面是生成的 YAML：
                ```yaml
                %s
                ```
                以上内容仅供参考。
                """.formatted(validYaml()));

        assertThat(repaired).doesNotContain("```");
        assertThat(validator.validate(repaired).valid()).isTrue();
    }

    @Test
    void repairNormalizesCommonFormattingIssues() {
        String repaired = validator.repair("""
                project：
                  title：雨夜来信
                  source_type:novel
                  script_type:short_drama
                  language:zh-CN
                  target_episodes:3
                characters:
                  -id:char_001
                    name：林秋
                    role:主角
                episodes:
                  -episode_id:1
                    title:第一集：雨夜来信
                    summary:匿名信把林秋引向旧剧院。
                    scenes:
                      -scene_id:1-1
                        title:匿名信
                        location:林秋家
                        time:夜晚
                        characters:
                          - char_001
                        action:林秋读完匿名信，决定前往旧剧院。
                        dialogues:
                          -character:char_001
                            line:我必须弄清楚这封信是谁寄来的。
                """);

        assertThat(repaired).contains("project:", "source_type: novel", "- id: char_001");
        assertThat(validator.validate(repaired).valid()).isTrue();
    }

    @Test
    void repairStripsLeadingAndTrailingNarrationWithoutCodeFence() {
        String repaired = validator.repair("""
                我将直接给出 YAML：
                %s
                以上是修复后的版本。
                """.formatted(validYaml()));

        assertThat(repaired).startsWith("project:");
        assertThat(repaired).doesNotContain("我将直接给出", "以上是修复后的版本");
        assertThat(validator.validate(repaired).valid()).isTrue();
    }

    private String validYaml() {
        return """
                project:
                  title: 雨夜来信
                  source_type: novel
                  script_type: short_drama
                  language: zh-CN
                  target_episodes: 3
                  summary: 林秋收到匿名信并前往旧剧院。
                characters:
                  - id: char_001
                    name: 林秋
                    role: 主角
                    description: 收到匿名信的调查者。
                    motivation: 找到信件真相
                    relationships:
                      - target: char_002
                        relation: 朋友
                  - id: char_002
                    name: 朋友
                    role: 配角
                episodes:
                  - episode_id: 1
                    title: 第一集：雨夜来信
                    summary: 林秋收到匿名信并前往旧剧院。
                    scenes:
                      - scene_id: 1-1
                        title: 匿名信
                        location: 林秋家
                        time: 夜晚
                        characters:
                          - char_001
                        action: 林秋读完匿名信，决定前往旧剧院。
                        dialogues:
                          - character: char_001
                            line: 我必须弄清楚这封信是谁寄来的。
                """;
    }
}
